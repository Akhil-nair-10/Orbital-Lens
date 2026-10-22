package com.orbitallens.backend.tle;

import com.orbitallens.backend.api.error.InvalidTleDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TleParserTest {

    private static final String SAMPLE_TLE_3_LINES = """
            ISS (ZARYA)
            1 25544U 98067A   26294.04791053  .00009575  00000+0  18188-3 0  9996
            2 25544  51.6327  37.2623 0006759 289.5321  70.4938 15.48967427591263
            """;

    private static final String SAMPLE_TLE_2_LINES = """
            1 25544U 98067A   26294.04791053  .00009575  00000+0  18188-3 0  9996
            2 25544  51.6327  37.2623 0006759 289.5321  70.4938 15.48967427591263
            """;

    @Test
    @DisplayName("Sample TLE parses correctly with expected name, catalog and epoch within 2 seconds of 2026-10-21T01:08:59Z")
    void parsesSampleTleCorrectly() {
        TleSnapshot snapshot = TleParser.parse(SAMPLE_TLE_3_LINES);

        assertEquals("ISS (ZARYA)", snapshot.name());
        assertEquals("25544", snapshot.lines()[1].substring(2, 7));
        assertEquals("25544", snapshot.lines()[2].substring(2, 7));

        Instant expectedEpoch = Instant.parse("2026-10-21T01:08:59Z");
        long diffSeconds = Math.abs(Duration.between(expectedEpoch, snapshot.epoch()).toSeconds());
        assertTrue(diffSeconds <= 2, "Epoch should be within 2s of 2026-10-21T01:08:59Z, but difference was " + diffSeconds + "s");
    }

    @Test
    @DisplayName("2-line TLE input parses and defaults name to ISS (ZARYA)")
    void parsesTwoLineTleCorrectly() {
        TleSnapshot snapshot = TleParser.parse(SAMPLE_TLE_2_LINES);
        assertEquals("ISS (ZARYA)", snapshot.name());
        assertEquals(3, snapshot.lines().length);
    }

    @Test
    @DisplayName("Line with wrong checksum is rejected with InvalidTleDataException")
    void rejectsInvalidChecksum() {
        // Corrupt the last character (checksum) of line 1 from 6 to 7
        String corrupt = """
                ISS (ZARYA)
                1 25544U 98067A   26294.04791053  .00009575  00000+0  18188-3 0  9997
                2 25544  51.6327  37.2623 0006759 289.5321  70.4938 15.48967427591263
                """;
        InvalidTleDataException ex = assertThrows(InvalidTleDataException.class, () -> TleParser.parse(corrupt));
        assertTrue(ex.getMessage().contains("checksum failure"));
    }

    @Test
    @DisplayName("Line with wrong line number is rejected")
    void rejectsWrongLineNumbers() {
        String swapped = """
                ISS (ZARYA)
                2 25544U 98067A   26294.04791053  .00009575  00000+0  18188-3 0  9996
                1 25544  51.6327  37.2623 0006759 289.5321  70.4938 15.48967427591263
                """;
        assertThrows(InvalidTleDataException.class, () -> TleParser.parse(swapped));
    }

    @Test
    @DisplayName("Line with wrong length is rejected")
    void rejectsWrongLength() {
        // Shortened line 1 (68 chars instead of 69)
        String shortened = """
                ISS (ZARYA)
                1 25544U 98067A   26294.04791053  .00009575  00000+0  18188-3 0  999
                2 25544  51.6327  37.2623 0006759 289.5321  70.4938 15.48967427591263
                """;
        InvalidTleDataException ex = assertThrows(InvalidTleDataException.class, () -> TleParser.parse(shortened));
        assertTrue(ex.getMessage().contains("invalid length"));
    }

    @Test
    @DisplayName("Non-ISS catalog number is rejected")
    void rejectsWrongCatalogNumber() {
        // Catalog changed from 25544 to 12345 on line 1 (with matching checksum recalculation)
        // Original line 1 sum was 186. Replacing '25544' (sum 20) with '12345' (sum 15) reduces sum by 5 -> 181 -> checksum 1
        String nonIssLine1 = "1 12345U 98067A   26294.04791053  .00009575  00000+0  18188-3 0  9991";
        String nonIss = "ISS (ZARYA)\n" + nonIssLine1 + "\n2 25544  51.6327  37.2623 0006759 289.5321  70.4938 15.48967427591263\n";

        InvalidTleDataException ex = assertThrows(InvalidTleDataException.class, () -> TleParser.parse(nonIss));
        assertTrue(ex.getMessage().contains("expected '25544' (ISS)"));
    }
}
