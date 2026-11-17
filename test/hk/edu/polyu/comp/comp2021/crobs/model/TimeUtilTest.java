package hk.edu.polyu.comp.comp2021.crobs.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeUtilTest {
    @Test
    void parsesLeapDayAnd24HourTimes() {
        assertEquals(LocalDate.of(2028, 2, 29), TimeUtil.parseDate("2028-02-29"));
        assertEquals(LocalTime.MIDNIGHT, TimeUtil.parseTime("00:00"));
        assertEquals(LocalTime.of(23, 59), TimeUtil.parseTime("23:59"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-02-29", "2026-04-31", "2026-11-1", "+10000-01-01"})
    void rejectsInvalidDates(String date) {
        assertThrows(IllegalArgumentException.class, () -> TimeUtil.parseDate(date));
    }

    @ParameterizedTest
    @ValueSource(strings = {"24:00", "12:60", "9:00", "09:00:00"})
    void rejectsInvalidTimes(String time) {
        assertThrows(IllegalArgumentException.class, () -> TimeUtil.parseTime(time));
    }

    @Test
    void requiresEndAfterStart() {
        LocalTime noon = LocalTime.NOON;
        assertDoesNotThrow(() -> TimeUtil.validateTimeRange(noon, noon.plusMinutes(1)));
        assertThrows(IllegalArgumentException.class, () -> TimeUtil.validateTimeRange(noon, noon));
        assertThrows(IllegalArgumentException.class, () -> TimeUtil.validateTimeRange(noon, noon.minusMinutes(1)));
        assertThrows(IllegalArgumentException.class, () -> TimeUtil.validateTimeRange(null, noon));
        assertThrows(IllegalArgumentException.class, () -> TimeUtil.validateTimeRange(noon, null));
    }
}
