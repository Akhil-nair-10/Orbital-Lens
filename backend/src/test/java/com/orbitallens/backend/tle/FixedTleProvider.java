package com.orbitallens.backend.tle;

import java.time.Instant;

/**
 * Deterministic test helper implementing TleProvider.
 * Returns the bundled sample ISS TLE without relying on system clock or external network.
 */
public class FixedTleProvider implements TleProvider {

    private final TleSnapshot snapshot;

    public FixedTleProvider() {
        String sample = """
                ISS (ZARYA)
                1 25544U 98067A   26294.04791053  .00009575  00000+0  18188-3 0  9996
                2 25544  51.6327  37.2623 0006759 289.5321  70.4938 15.48967427591263
                """;
        this.snapshot = TleParser.parse(sample, null, TleSource.BUNDLED_SAMPLE);
    }

    public FixedTleProvider(TleSnapshot customSnapshot) {
        this.snapshot = customSnapshot;
    }

    public static FixedTleProvider withExpiredEpoch(Instant now) {
        // Creates a snapshot whose epoch is 400 hours before now, triggering EXPIRED status
        Instant expiredEpoch = now.minusSeconds(400 * 3600);
        String sample = """
                ISS (ZARYA)
                1 25544U 98067A   26294.04791053  .00009575  00000+0  18188-3 0  9996
                2 25544  51.6327  37.2623 0006759 289.5321  70.4938 15.48967427591263
                """;
        TleSnapshot base = TleParser.parse(sample, null, TleSource.BUNDLED_SAMPLE);
        TleSnapshot expired = new TleSnapshot(base.lines(), base.name(), expiredEpoch, null, TleSource.BUNDLED_SAMPLE);
        return new FixedTleProvider(expired);
    }

    @Override
    public TleSnapshot current() {
        return snapshot;
    }
}
