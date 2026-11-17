package hk.edu.polyu.comp.comp2021.crobs.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingManagerTest {
    private static final String DATE = "2027-01-04";
    private CROBS crobs;
    private BookingManager bookings;
    private String room;
    private String anotherRoom;

    @BeforeEach
    void setUp() {
        crobs = new CROBS();
        Clock clock = Clock.fixed(Instant.parse("2027-01-04T10:30:00Z"), ZoneOffset.UTC);
        bookings = new BookingManager(crobs, clock);
        room = crobs.getRoomManager().addRoom("seminarRoom", 30, "projector").getRoomId();
        anotherRoom = crobs.getRoomManager().addRoom("meetingRoom", 10, "").getRoomId();
        login(crobs.getSuperadminId());
    }

    @Test
    void bookingIdsAreNotReusedAfterDeletion() {
        Booking first = book(room, "10:00", "11:00", "Meeting");
        bookings.deleteBooking(first.getBookingId());
        assertEquals("B002", book(room, "10:00", "11:00", "Replacement").getBookingId());
    }

    @Test
    void creationAndModificationRejectConflictsAndKeepIdAndOwner() {
        Booking first = book(room, "10:00", "11:00", "First");
        Booking next = book(room, "11:00", "12:00", "Next");
        assertThrows(IllegalArgumentException.class, () -> book(room, "10:30", "11:30", "Overlaps"));
        assertThrows(IllegalArgumentException.class, () -> bookings.modifyBooking(next.getBookingId(),
                room, DATE, "10:30", "11:30", "Overlaps"));
        assertSame(next, crobs.getBookings().get(next.getBookingId()));

        Booking changed = bookings.modifyBooking(first.getBookingId(), room, DATE, "09:30", "10:30", "New purpose");
        assertEquals(first.getBookingId(), changed.getBookingId());
        assertEquals(first.getOwnerUserId(), changed.getOwnerUserId());
        assertEquals("New purpose", changed.getPurpose());
    }

    @Test
    void deletingRoomCancelsOnlyBookingsThatHaveNotStarted() {
        Booking finished = book(room, "09:00", "10:00", "Finished");
        Booking inProgress = book(room, "10:00", "11:00", "In progress");
        Booking future = book(room, "11:00", "12:00", "Future");
        Booking other = book(anotherRoom, "11:00", "12:00", "Other room");

        assertEquals(1, bookings.cancelAllFutureBookingsOfRoomId(room));
        assertFalse(crobs.getBookings().containsKey(future.getBookingId()));
        assertTrue(crobs.getBookings().values().containsAll(Arrays.asList(finished, inProgress, other)));
    }

    @Test
    void deletingUserCancelsAllOfTheirBookings() {
        Booking adminBooking = book(room, "09:00", "10:00", "Admin");
        assertTrue(crobs.addUser("Alice").isSuccessful());
        login("U0002");
        book(room, "10:00", "11:00", "Alice past");
        book(room, "11:00", "12:00", "Alice future");

        login(crobs.getSuperadminId());
        assertTrue(crobs.removeUser("U0002").isSuccessful());
        assertEquals(Arrays.asList(adminBooking), bookings.getBookingsOfRoom(room));
    }

    @Test
    void listsOnlyOwnedBookingsByDateAndStartTime() {
        book(anotherRoom, "09:00", "10:00", "Other user's booking");
        assertTrue(crobs.addUser("Alice").isSuccessful());
        login("U0002");
        Booking later = book(room, "11:00", "12:00", "Later");
        Booking earlier = book(room, "09:00", "10:00", "Earlier");
        Booking earlierDate = bookings.book(room, "2027-01-03", "11:00", "12:00", "Previous day");

        assertEquals(String.join(System.lineSeparator(), earlierDate.describe(), earlier.describe(), later.describe()),
                bookings.listBookings());
    }

    @Test
    void handlesWrongUserMissingIdsAndInvalidInput() {
        Booking own = book(room, "10:00", "11:00", "Owned by admin");
        assertTrue(crobs.addUser("Alice").isSuccessful());
        login("U0002");
        assertThrows(SecurityException.class, () -> bookings.deleteBooking(own.getBookingId()));
        assertThrows(SecurityException.class, () -> bookings.modifyBooking(own.getBookingId(),
                room, DATE, "10:00", "11:00", "Another user's booking"));
        assertThrows(NoSuchElementException.class, () -> bookings.deleteBooking("missing"));
        assertThrows(NoSuchElementException.class, () -> book("missing", "10:00", "11:00", "Meeting"));
        assertThrows(IllegalArgumentException.class, () -> book(room, "11:00", "10:00", "Meeting"));
        assertThrows(IllegalArgumentException.class, () -> book(room, "10:00", "11:00", " "));
        assertEquals("You have no bookings.", bookings.listBookings());
        assertTrue(crobs.logout().isSuccessful());
        assertThrows(SecurityException.class, bookings::listBookings);
    }

    private Booking book(String roomId, String start, String end, String purpose) {
        return bookings.book(roomId, DATE, start, end, purpose);
    }

    private void login(String userId) {
        if (crobs.getCurrentUser() != null) {
            assertTrue(crobs.logout().isSuccessful());
        }
        assertTrue(crobs.login(userId).isSuccessful());
    }
}
