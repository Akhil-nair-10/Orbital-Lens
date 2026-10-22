package com.orbitallens.backend.tle;

import com.orbitallens.backend.api.error.InvalidTleDataException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;

/**
 * Parses and validates Two-Line Element (TLE) text data for the International Space Station.
 * Validates NORAD standard 69-character format, catalog number 25544, and checksums.
 */
public final class TleParser {

    private static final String DEFAULT_NAME = "ISS (ZARYA)";
    private static final String EXPECTED_CATALOG_NUMBER = "25544";

    private TleParser() {
        // Utility class
    }

    /**
     * Parses TLE text with default source BUNDLED_SAMPLE and null retrievedAt.
     */
    public static TleSnapshot parse(String text) {
        return parse(text, null, TleSource.BUNDLED_SAMPLE);
    }

    /**
     * Parses TLE text with explicit metadata.
     *
     * @param text Raw TLE text (2 or 3 lines)
     * @param retrievedAt Instant when fetched/cached
     * @param source Source enum
     * @return Validated TleSnapshot
     */
    public static TleSnapshot parse(String text, Instant retrievedAt, TleSource source) {
        if (text == null || text.isBlank()) {
            throw new InvalidTleDataException("TLE data cannot be empty or null");
        }

        // Split into lines and strip trailing whitespace as requested by CelesTrak specifications
        String[] rawLines = text.lines()
                .map(String::stripTrailing)
                .filter(line -> !line.isBlank())
                .toArray(String[]::new);

        String name;
        String line1;
        String line2;

        if (rawLines.length == 3) {
            name = rawLines[0].trim();
            line1 = rawLines[1];
            line2 = rawLines[2];
        } else if (rawLines.length == 2) {
            name = DEFAULT_NAME;
            line1 = rawLines[0];
            line2 = rawLines[1];
        } else {
            throw new InvalidTleDataException(
                    "Invalid TLE format: expected 2 or 3 lines, found " + rawLines.length);
        }

        // Line 1 validation
        validateLine(line1, "1");
        // Line 2 validation
        validateLine(line2, "2");

        // Parse epoch timestamp from Line 1
        Instant epoch = parseEpoch(line1);

        // predict4java expects an array of 3 lines: [satelliteName, line1, line2]
        String[] tleLines = new String[] { name, line1, line2 };

        return new TleSnapshot(tleLines, name, epoch, retrievedAt, source);
    }

    private static void validateLine(String line, String expectedLineNumber) {
        // Rule: Each line must be exactly 69 characters
        if (line.length() != 69) {
            throw new InvalidTleDataException(String.format(
                    "Line %s has invalid length %d (expected 69)", expectedLineNumber, line.length()));
        }

        // Rule: Line 1 starts with "1 ", Line 2 starts with "2 "
        if (!line.startsWith(expectedLineNumber + " ")) {
            throw new InvalidTleDataException(String.format(
                    "Line %s must start with '%s '", expectedLineNumber, expectedLineNumber));
        }

        // Rule: Catalog number (columns 3-7, zero-indexed 2 to 7) must match 25544
        String catNr = line.substring(2, 7).trim();
        if (!EXPECTED_CATALOG_NUMBER.equals(catNr)) {
            throw new InvalidTleDataException(String.format(
                    "Line %s contains catalog number '%s', expected '%s' (ISS)",
                    expectedLineNumber, catNr, EXPECTED_CATALOG_NUMBER));
        }

        // Rule: Checksum verification
        int calculatedChecksum = computeChecksum(line);
        char expectedChecksumChar = line.charAt(68);
        int expectedChecksum = Character.digit(expectedChecksumChar, 10);
        if (expectedChecksum == -1 || calculatedChecksum != expectedChecksum) {
            throw new InvalidTleDataException(String.format(
                    "Line %s checksum failure: calculated %d, found %c",
                    expectedLineNumber, calculatedChecksum, expectedChecksumChar));
        }
    }

    /**
     * Computes the modulo-10 checksum of the first 68 characters of a TLE line.
     * Digits count as their numerical value, '-' counts as 1, all other characters count as 0.
     */
    public static int computeChecksum(String line) {
        int sum = 0;
        for (int i = 0; i < 68; i++) {
            char c = line.charAt(i);
            if (c >= '0' && c <= '9') {
                sum += (c - '0');
            } else if (c == '-') {
                sum += 1;
            }
        }
        return sum % 10;
    }

    /**
     * Extracts and converts the epoch from TLE line 1 into an Instant.
     * Index 18-19: 2-digit year (yy < 57 means 2000+yy, else 1900+yy)
     * Index 20-31: fractional day of year (1.0 = Jan 1 00:00:00 UTC)
     */
    public static Instant parseEpoch(String line1) {
        try {
            int twoDigitYear = Integer.parseInt(line1.substring(18, 20).trim());
            int fullYear = (twoDigitYear < 57) ? 2000 + twoDigitYear : 1900 + twoDigitYear;

            double dayOfYearFraction = Double.parseDouble(line1.substring(20, 32).trim());

            // Day 1.0 represents Jan 1 at 00:00:00 UTC, so subtract 1.0 to get elapsed days
            double elapsedDays = dayOfYearFraction - 1.0;
            long wholeSeconds = (long) (elapsedDays * 86_400.0);
            double fractionalSeconds = (elapsedDays * 86_400.0) - wholeSeconds;
            long nanos = (long) Math.round(fractionalSeconds * 1_000_000_000.0);

            Instant startOfYear = LocalDate.of(fullYear, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
            return startOfYear.plusSeconds(wholeSeconds).plusNanos(nanos);
        } catch (Exception e) {
            throw new InvalidTleDataException("Failed to parse epoch from line 1: " + e.getMessage(), e);
        }
    }
}
