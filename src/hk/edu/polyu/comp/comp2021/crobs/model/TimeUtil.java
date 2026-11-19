package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/** Strict date, time and interval validation. */
public final class TimeUtil {
    /** Four-digit proleptic-year date pattern. */
    private static final String DATE_PATTERN = "uuuu-MM-dd";
    /** 24-hour time pattern. */
    private static final String TIME_PATTERN = "HH:mm";
    /** Invalid-date message. */
    private static final String INVALID_DATE = "Invalid date. Use yyyy-MM-dd.";
    /** Strict locale-independent date formatter. */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(DATE_PATTERN, Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
    /** Strict locale-independent time formatter. */
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern(TIME_PATTERN, Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);

    /** Prevents instantiation. */
    private TimeUtil() { }

    /**
     * Parses a strict yyyy-MM-dd date.
     *
     * @param input date text
     * @return the parsed date
     * @throws IllegalArgumentException if input is null, invalid or incorrectly formatted
     */
    public static LocalDate parseDate(String input) {
        if (input == null || !input.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            throw new IllegalArgumentException(INVALID_DATE);
        }
        try {
            return LocalDate.parse(input, DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(INVALID_DATE, exception);
        }
    }

    /**
     * Parses a 24-hour HH:mm time.
     *
     * @param input time text
     * @return the parsed time
     * @throws IllegalArgumentException if input is null, invalid or incorrectly formatted
     */
    public static LocalTime parseTime(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Invalid time. Use HH:mm.");
        }
        try {
            return LocalTime.parse(input, TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Invalid time. Use HH:mm.", exception);
        }
    }

    /**
     * Requires start to be earlier than end.
     *
     * @param start start time
     * @param end end time
     * @throws IllegalArgumentException if either time is null or end is not after start
     */
    public static void validateTimeRange(LocalTime start, LocalTime end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new IllegalArgumentException("Start time must be before end time.");
        }
    }
}
