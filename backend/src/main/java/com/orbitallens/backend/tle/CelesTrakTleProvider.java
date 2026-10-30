package com.orbitallens.backend.tle;

import com.orbitallens.backend.api.error.TleUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Fetches ISS TLE data with this priority:
 * CelesTrak -> mirror (GitHub raw file) -> in-memory -> disk cache -> bundled sample.
 * Honors cache TTL and failure cooldown so remote servers are not hammered.
 */
@Component
public class CelesTrakTleProvider implements TleProvider {

    private static final Logger log = LoggerFactory.getLogger(CelesTrakTleProvider.class);

    private final String tleUrl;
    private final String mirrorUrl;
    private final long cacheTtlMinutes;
    private final long failureCooldownMinutes;
    private final String cacheFilePath;
    private final long httpTimeoutSeconds;

    private final HttpClient httpClient;

    private TleSnapshot inMemorySnapshot;
    private Instant lastFetchTime;
    private Instant lastFailureTime;

    public CelesTrakTleProvider(
            @Value("${orbitallens.tle.url}") String tleUrl,
            @Value("${orbitallens.tle.mirror-url:}") String mirrorUrl,
            @Value("${orbitallens.tle.cache-ttl-minutes}") long cacheTtlMinutes,
            @Value("${orbitallens.tle.failure-cooldown-minutes}") long failureCooldownMinutes,
            @Value("${orbitallens.tle.cache-file}") String cacheFilePath,
            @Value("${orbitallens.tle.http-timeout-seconds}") long httpTimeoutSeconds
    ) {
        this.tleUrl = tleUrl;
        this.mirrorUrl = mirrorUrl == null ? "" : mirrorUrl.trim();
        this.cacheTtlMinutes = cacheTtlMinutes;
        this.failureCooldownMinutes = failureCooldownMinutes;
        this.cacheFilePath = cacheFilePath;
        this.httpTimeoutSeconds = httpTimeoutSeconds;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public synchronized TleSnapshot current() {
        Instant now = Instant.now();

        // 1. Fresh enough in memory -> reuse
        if (inMemorySnapshot != null && lastFetchTime != null
                && Duration.between(lastFetchTime, now).toMinutes() < cacheTtlMinutes) {
            return inMemorySnapshot;
        }

        // 2. Recent failure -> skip network for a while.
        // If we only have the bundled sample (or nothing), retry after 1 minute instead of the full cooldown.
        boolean haveRealData = inMemorySnapshot != null && inMemorySnapshot.source() != TleSource.BUNDLED_SAMPLE;
        long cooldownMinutes = haveRealData ? failureCooldownMinutes : 1;
        boolean skipNetwork = lastFailureTime != null
                && Duration.between(lastFailureTime, now).toMinutes() < cooldownMinutes;

        if (!skipNetwork) {
            TleSnapshot fetched = tryFetch(tleUrl, TleSource.CELESTRAK, now, 3);
            if (fetched == null && !mirrorUrl.isEmpty()) {
                fetched = tryFetch(mirrorUrl, TleSource.MIRROR, now, 20);
            }
            if (fetched != null) {
                this.inMemorySnapshot = fetched;
                this.lastFetchTime = now;
                this.lastFailureTime = null;
                writeCacheFile(fetched);
                return fetched;
            }
            log.warn("All TLE sources failed. Retrying in {} minute(s).", cooldownMinutes);
            this.lastFailureTime = now;
        }

        // 3. Fallbacks: in-memory -> cache file -> bundled sample
        if (inMemorySnapshot != null) {
            return inMemorySnapshot;
        }

        TleSnapshot fromCache = readCacheFile();
        if (fromCache != null) {
            this.inMemorySnapshot = fromCache;
            return fromCache;
        }

        TleSnapshot fromBundled = readBundledSample();
        if (fromBundled != null) {
            this.inMemorySnapshot = fromBundled;
            return fromBundled;
        }

        throw new TleUnavailableException("Failed to acquire ISS TLE data from any source.");
    }

    /** Returns a parsed snapshot, or null (after logging the reason) on any failure. */
    private TleSnapshot tryFetch(String url, TleSource source, Instant now, long timeoutSeconds) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("User-Agent", "OrbitalLens/1.0 (student project)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body() == null ? "" : response.body();

            if (response.statusCode() != 200) {
                String snippet = body.substring(0, Math.min(200, body.length())).replace('\n', ' ');
                log.warn("TLE source {} ({}) returned HTTP {} body='{}'", source, url, response.statusCode(), snippet);
                return null;
            }
            TleSnapshot snapshot = TleParser.parse(body, now, source);
            log.info("Fetched TLE from {} (epoch {})", source, snapshot.epoch());
            return snapshot;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("TLE fetch from {} interrupted", source);
            return null;
        } catch (Exception e) {
            log.warn("TLE fetch from {} ({}) failed: {}", source, url, e.toString());
            return null;
        }
    }

    private void writeCacheFile(TleSnapshot snapshot) {
        try {
            Path path = Path.of(cacheFilePath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            StringBuilder sb = new StringBuilder();
            sb.append("retrievedAt=").append(snapshot.retrievedAt()).append("\n");
            for (String line : snapshot.lines()) {
                sb.append(line).append("\n");
            }
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Could not write TLE cache file: {}", e.getMessage());
        }
    }

    private TleSnapshot readCacheFile() {
        try {
            Path path = Path.of(cacheFilePath);
            if (!Files.exists(path)) {
                return null;
            }
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                return null;
            }
            Instant retrievedAt = null;
            int startIndex = 0;
            if (lines.get(0).startsWith("retrievedAt=")) {
                String isoStr = lines.get(0).substring("retrievedAt=".length()).trim();
                retrievedAt = Instant.parse(isoStr);
                startIndex = 1;
            }
            StringBuilder tleText = new StringBuilder();
            for (int i = startIndex; i < lines.size(); i++) {
                tleText.append(lines.get(i)).append("\n");
            }
            return TleParser.parse(tleText.toString(), retrievedAt, TleSource.CACHE_FILE);
        } catch (Exception e) {
            log.warn("Failed to load TLE from cache file: {}", e.getMessage());
            return null;
        }
    }

    private TleSnapshot readBundledSample() {
        try (InputStream is = getClass().getResourceAsStream("/sample-iss-tle.txt")) {
            if (is == null) {
                log.error("Bundled sample resource /sample-iss-tle.txt not found");
                return null;
            }
            String text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return TleParser.parse(text, null, TleSource.BUNDLED_SAMPLE);
        } catch (Exception e) {
            log.error("Failed to read bundled sample TLE: {}", e.getMessage());
            return null;
        }
    }
}