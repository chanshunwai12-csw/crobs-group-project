package hk.edu.polyu.comp.comp2021.crobs.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.*;

class BookingManagerTest {

    private CROBS context;
    private BookingManager manager;
    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        context = new CROBS();
        alice = new User("U001", "Alice", Role.REGULAR);
        bob = new User("U002", "Bob", Role.ADMIN);
        context.getUsers().put(alice.getId(), alice);
        context.getUsers().put(bob.getId(), bob);
        context.getRooms().put("R001", new Room("R001", "seminarRoom", 30,
                new LinkedHashSet<>(Arrays.asList("projector"))));
        context.setCurrentUser(alice);
        manager = new BookingManager(context);
    }

    @Test
    void bookGeneratesSequentialIds() {
        Booking first = manager.book("R001", "2026-11-20", "09:00", "10:00", "Meeting");
        Booking second = manager.book("R001", "2026-11-20", "10:00", "11:00", "Lab");
        assertEquals("B001", first.getBookingId());
        assertEquals("B002", second.getBookingId());
    }

    @Test
    void bookRejectsOverlappingSlot() {
        manager.book("R001", "2026-11-20", "09:00", "10:00", "Meeting");
        assertThrows(IllegalArgumentException.class,
                () -> manager.book("R001", "2026-11-20", "09:30", "10:30", "Clash"));
    }

    @Test
    void bookingsTouchingAtTheBoundaryDoNotOverlap() {
        manager.book("R001", "2026-11-20", "09:00", "10:00", "Meeting");
        Booking next = manager.book("R001", "2026-11-20", "10:00", "11:00", "Lab");
        assertEquals("B002", next.getBookingId());
    }

    @Test
    void modifyDoesNotConflictWithItself() {
        Booking booking = manager.book("R001", "2026-11-20", "09:00", "10:00", "Meeting");
        Booking updated = manager.modifyBooking(booking.getBookingId(), "R001",
                "2026-11-20", "09:30", "10:30", "Moved");
        assertEquals("Moved", updated.getPurpose());
        assertEquals("B001", updated.getBookingId());
    }

    @Test
    void modifyRejectsAnotherUsersBooking() {
        Booking booking = manager.book("R001", "2026-11-20", "09:00", "10:00", "Meeting");
        context.setCurrentUser(bob);
        assertThrows(SecurityException.class, () -> manager.modifyBooking(
                booking.getBookingId(), "R001", "2026-11-21", "09:00", "10:00", "Stolen"));
    }

    @Test
    void listBookingsReturnsOnlyMineSorted() {
        manager.book("R001", "2026-11-20", "14:00", "15:00", "MineLate");
        manager.book("R001", "2026-11-20", "09:00", "10:00", "MineEarly");
        context.setCurrentUser(bob);
        manager.book("R001", "2026-11-20", "11:00", "12:00", "Bobs");
        context.setCurrentUser(alice);

        String text = manager.listBookings();
        assertTrue(text.indexOf("MineEarly") < text.indexOf("MineLate"));
        assertFalse(text.contains("Bobs"));
    }

    @Test
    void cancelAllFutureKeepsPastBookings() {
        Booking past = manager.book("R001", LocalDate.now().minusDays(3).toString(),
                "09:00", "10:00", "Old");
        Booking future = manager.book("R001", LocalDate.now().plusDays(3).toString(),
                "09:00", "10:00", "New");

        assertEquals(1, manager.cancelAllFutureBookingsOfRoomId("R001"));
        assertTrue(context.getBookings().containsKey(past.getBookingId()));
        assertFalse(context.getBookings().containsKey(future.getBookingId()));
    }
}