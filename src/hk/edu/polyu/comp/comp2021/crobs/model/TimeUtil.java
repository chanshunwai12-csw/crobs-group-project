package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/** Shared parsing and validation for booking, search, and schedule commands. */
public final class TimeUtil {
    /** Strict calendar-date pattern with a four-digit proleptic year. */
    private static final String DATE_PATTERN = "uuuu-MM-dd";
    /** Strict 24-hour hour-and-minute pattern. */
    private static final String TIME_PATTERN = "HH:mm";
    /** Validation response for invalid or incorrectly formatted dates. */
    private static final String INVALID_DATE = "Invalid date. Use yyyy-MM-dd.";
    /** Locale-independent formatter that rejects invalid calendar dates. */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(DATE_PATTERN, Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
    /** Locale-independent formatter for valid 24-hour times. */
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern(TIME_PATTERN, Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);

    /** Prevents instantiation of the shared date and time utilities. */
    private TimeUtil() { }

    /**
     * Parses a date using the four-digit year, month and day format.
     * Rejects invalid calendar dates instead of adjusting them.
     *
     * @param input date in yyyy-MM-dd form
     * @return the parsed date
     * @throws IllegalArgumentException if the input is not a valid date in the required form
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
     * Parses a time in the 24-hour, hour-and-minute format.
     *
     * @param input time in 24-hour HH:mm form
     * @return the parsed time
     * @throws IllegalArgumentException if the input is not a valid time in the required form
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
     * Requires a same-day time slot with a start strictly before its end.
     *
     * @param start the start time
     * @param end the end time
     * @throws IllegalArgumentException if the times do not form an increasing same-day slot
     */
    public static void validateTimeRange(LocalTime start, LocalTime end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new IllegalArgumentException("Start time must be before end time.");
        }
    }
}
