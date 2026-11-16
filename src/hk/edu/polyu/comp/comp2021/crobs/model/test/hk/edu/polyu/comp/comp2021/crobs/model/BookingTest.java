package hk.edu.polyu.comp.comp2021.crobs.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingTest {

    private static Booking booking(String id, String date, String start, String end) {
        return new Booking(id, "U001", "R001",
                LocalDate.parse(date), LocalTime.parse(start), LocalTime.parse(end), "Meeting");
    }

    @Test
    void sortOrdersByDateThenStartTime() {
        List<Booking> bookings = new ArrayList<>();
        bookings.add(booking("B003", "2026-11-21", "14:00", "15:00"));
        bookings.add(booking("B001", "2026-11-20", "09:00", "10:00"));
        bookings.add(booking("B002", "2026-11-20", "08:00", "09:00"));

        Collections.sort(bookings);

        assertEquals("B002", bookings.get(0).getBookingId());
        assertEquals("B001", bookings.get(1).getBookingId());
        assertEquals("B003", bookings.get(2).getBookingId());
    }

    @Test
    void rejectsEndBeforeStart() {
        assertThrows(IllegalArgumentException.class,
                () -> booking("B004", "2026-11-22", "10:00", "09:00"));
    }

    @Test
    void rejectsEndEqualToStart() {
        assertThrows(IllegalArgumentException.class,
                () -> booking("B005", "2026-11-22", "10:00", "10:00"));
    }

    @Test
    void nextIdStartsAtOneAndSkipsExisting() {
        assertEquals("B001", BookingIds.next(Collections.emptyList()));
        assertEquals("B002", BookingIds.next(Arrays.asList("B001")));
        assertEquals("B005", BookingIds.next(Arrays.asList("B001", "B004", "R009")));
    }

    @Test
    void rejectsBlankPurpose() {
        assertThrows(IllegalArgumentException.class,
                () -> new Booking("B006", "U001", "R001", LocalDate.of(2026, 11, 22),
                        LocalTime.of(9, 0), LocalTime.of(10, 0), "   "));
    }
}