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
 * Manages fetching, disk caching, cooldowns, and fallbacks for ISS TLE data.
 * Adheres strictly to CelesTrak usage guidelines by honoring cooldowns and caching.
 */
@Component
public class CelesTrakTleProvider implements TleProvider {

    private static final Logger log = LoggerFactory.getLogger(CelesTrakTleProvider.class);

    private final String tleUrl;
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
            @Value("${orbitallens.tle.cache-ttl-minutes}") long cacheTtlMinutes,
            @Value("${orbitallens.tle.failure-cooldown-minutes}") long failureCooldownMinutes,
            @Value("${orbitallens.tle.cache-file}") String cacheFilePath,
            @Value("${orbitallens.tle.http-timeout-seconds}") long httpTimeoutSeconds
    ) {
        this.tleUrl = tleUrl;
        this.cacheTtlMinutes = cacheTtlMinutes;
        this.failureCooldownMinutes = failureCooldownMinutes;
        this.cacheFilePath = cacheFilePath;
        this.httpTimeoutSeconds = httpTimeoutSeconds;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(httpTimeoutSeconds))
                .build();
    }

    @Override
    public synchronized TleSnapshot current() {
        Instant now = Instant.now();

        // 1. If an in-memory TLE was fetched less than cache-ttl-minutes ago, return it
        if (inMemorySnapshot != null && lastFetchTime != null
                && Duration.between(lastFetchTime, now).toMinutes() < cacheTtlMinutes) {
            return inMemorySnapshot;
        }

        // 2. If the last failure was less than failure-cooldown-minutes ago, skip the network
        boolean skipNetwork = (lastFailureTime != null
                && Duration.between(lastFailureTime, now).toMinutes() < failureCooldownMinutes);

        if (!skipNetwork) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(tleUrl))
                        .timeout(Duration.ofSeconds(httpTimeoutSeconds))
                        .header("User-Agent", "OrbitalLens/1.0 (student project)")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    TleSnapshot snapshot = TleParser.parse(response.body(), now, TleSource.CELESTRAK);
                    this.inMemorySnapshot = snapshot;
                    this.lastFetchTime = now;
                    this.lastFailureTime = null;

                    writeCacheFile(snapshot);
                    return snapshot;
                } else {
                    log.warn("CelesTrak returned non-200 response: {}. Cooling down for {} minutes.",
                            response.statusCode(), failureCooldownMinutes);
                    this.lastFailureTime = now;
                }
            } catch (Exception e) {
                log.warn("Failed to fetch TLE from CelesTrak: {}. Cooling down for {} minutes.",
                        e.getMessage(), failureCooldownMinutes);
                this.lastFailureTime = now;
            }
        }

        // 3. Fallbacks in order: in-memory -> cache file -> bundled sample
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

        throw new TleUnavailableException("Failed to acquire ISS TLE data from CelesTrak, cache file, or bundled sample.");
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
