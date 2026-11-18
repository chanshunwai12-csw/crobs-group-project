package hk.edu.polyu.comp.comp2021.crobs.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

/** Verifies validation, command parsing, and preservation of model state on failure. */
class ModelEdgeCasesTest {
    /** Creates an independent test fixture for each JUnit test. */
    ModelEdgeCasesTest() {
    }

    /** Calendar date shared by booking validation and ownership tests. */
    private static final String BOOKING_DATE = "2027-01-01";

    /** Verifies that the shared booking manager assigns ownership to the active account. */
    @Test
    void sharedBookingManagerUsesTheCurrentAccountForOwnership() {
        CROBS crobs = new CROBS();
        assertTrue(crobs.login(crobs.getSuperadminId()).isSuccessful());
        assertTrue(crobs.getCurrentUser().isAdmin());
        Room room = crobs.getRoomManager().addRoom("meetingRoom", 10, "screen");
        Booking booking = crobs.getBookingManager().book(room.getRoomId(),
                BOOKING_DATE, "09:00", "10:00", "Planning");
        assertEquals(crobs.getCurrentUser().getUserId(), booking.getUserId());
        assertSame(booking, crobs.getBookings().get(booking.getBookingId()));
        assertTrue(crobs.getBookingManager().listBookings().contains(booking.getBookingId()));
        assertTrue(crobs.addUser("Regular user").isSuccessful());
        assertFalse(crobs.findUser("U0002").isAdmin());
    }

    /** Verifies account validation and rejection of invalid privilege and login changes. */
    @Test
    void rejectedAccountsCannotIntroduceInvalidIdentityOrPrivileges() {
        for (String invalid : Arrays.asList(null, "", "  ")) {
            assertThrows(IllegalArgumentException.class, () -> new User(invalid, "Alice", Role.REGULAR));
            assertThrows(IllegalArgumentException.class, () -> new User("U1", invalid, Role.REGULAR));
        }
        assertThrows(IllegalArgumentException.class, () -> new User("U1", "Alice", null));
        assertThrows(IllegalArgumentException.class, () -> Role.fromCommand(null));
        assertThrows(IllegalArgumentException.class, () -> Role.fromCommand("manager"));
        assertEquals(Role.ADMIN, Role.fromCommand("  AdMiN  "));
        User user = new User("U1", " Alice ", Role.REGULAR);
        assertEquals("U1", user.getUserId());
        assertEquals("U1 Alice REGULAR", user.toString());
        assertThrows(IllegalArgumentException.class, () -> user.changeRole(null));
        assertEquals(Role.REGULAR, user.getRole());
        Session session = new Session();
        assertThrows(IllegalArgumentException.class, () -> session.login(null));
        assertFalse(session.isLoggedIn());
        assertNull(session.getCurrentUser());
    }

    /** Verifies that invalid commands preserve the active account and user collection. */
    @Test
    void malformedCommandsDoNotChangeAccountsOrSession() {
        CROBS crobs = new CROBS();
        assertThrows(IllegalArgumentException.class, () -> new CROBS(null));
        assertFalse(crobs.changeRole("U0002", "ADMIN").isSuccessful());
        assertFalse(crobs.removeUser("U0002").isSuccessful());
        assertTrue(crobs.login(crobs.getSuperadminId()).isSuccessful());
        for (String line : Arrays.asList(null, "", "  ", "addUser", "changeRole",
                "login", "login U0001 extra", "removeUser U0002 extra", "logout extra", "unknown")) {
            assertFalse(crobs.execute(line).isSuccessful(), () -> "Rejected input: " + line);
            assertEquals(1, crobs.getUserCount());
            assertEquals(crobs.getSuperadminId(), crobs.getCurrentUser().getId());
        }
        assertFalse(crobs.changeRole("unknown", "ADMIN").isSuccessful());
        assertFalse(crobs.changeRole("U0001", null).isSuccessful());
        assertTrue(crobs.execute("addUser Alice").isSuccessful());
        assertTrue(crobs.execute("removeUser U0002").isSuccessful());
        assertNull(crobs.findUser("U0002"));
        assertTrue(crobs.execute("addUser Bob").isSuccessful());
        assertNotNull(crobs.findUser("U0003"));
        assertFalse(crobs.changeRole("U0003", null).isSuccessful());
        assertEquals(Role.REGULAR, crobs.findUser("U0003").getRole());
        CommandResult failure = CommandResult.failure("rejected");
        assertFalse(failure.isSuccessful());
        assertEquals("rejected", failure.getMessage());
    }

    /** Verifies booking validation and ordering when reservations have equal time slots. */
    @Test
    void bookingRejectsInvalidSlotsAndSortsTiesByIdentity() {
        LocalDate date = LocalDate.parse(BOOKING_DATE);
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(10, 0);
        assertThrows(IllegalArgumentException.class,
                () -> new Booking("B1", "U1", "R1", date, start, end, " "));
        assertThrows(IllegalArgumentException.class,
                () -> new Booking("B1", "U1", "R1", date, end, start, "Study"));
        assertThrows(IllegalArgumentException.class,
                () -> new Booking("B1", "U1", "R1", date, start, start, "Study"));
        assertThrows(NullPointerException.class,
                () -> new Booking("B1", "U1", "R1", date, start, end, null));
        Booking first = new Booking("B001", "U1", "R1", date, start, end, "Study");
        Booking second = new Booking("B002", "U1", "R1", date, start, end, "Study");
        assertTrue(first.isActive());
        assertTrue(first.compareTo(second) < 0);
        assertTrue(second.compareTo(first) > 0);
        Booking equivalent = new Booking("B001", "U1", "R1", date, start, end, "Study");
        assertEquals(0, first.compareTo(equivalent));
    }

    /** Verifies that invalid room updates leave the previously accepted attributes intact. */
    @Test
    void rejectedRoomUpdatesPreserveTheExistingAttributes() {
        assertThrows(IllegalArgumentException.class,
                () -> new Room(null, "meetingRoom", 10, Collections.emptySet()));
        assertThrows(IllegalArgumentException.class,
                () -> new Room(" ", "meetingRoom", 10, Collections.emptySet()));
        Room room = new Room("R1", "meetingRoom", 10, Collections.singleton("screen"));
        assertThrows(IllegalArgumentException.class, () -> room.setFeatures(null));
        for (String feature : Arrays.asList(null, "", "two words", "a,b", "a\u00a0b")) {
            assertThrows(IllegalArgumentException.class,
                    () -> room.setFeatures(new HashSet<>(Arrays.asList("valid", feature))));
            assertEquals(Collections.singleton("screen"), room.getFeatures());
        }
        roomManager rooms = new roomManager();
        Room stored = rooms.addRoom("meetingRoom", 10, "screen");
        assertThrows(IllegalArgumentException.class, () -> rooms.modifyRoom(stored.getRoomId(), null, "10"));
        assertThrows(IllegalArgumentException.class, () -> rooms.modifyRoom(stored.getRoomId(), "capacity", null));
        assertEquals(10, stored.getCapacity());
        assertEquals("meetingRoom", stored.getRoomType());
    }

    /** Verifies null date-time inputs use the documented validation exception. */
    @Test
    void nullTimeIsRejectedWithTheDocumentedValidationException() {
        assertThrows(IllegalArgumentException.class, () -> TimeUtil.parseTime(null));
        assertThrows(IllegalArgumentException.class,
                () -> TimeUtil.validateTimeRange(null, LocalTime.NOON));
        assertThrows(IllegalArgumentException.class,
                () -> TimeUtil.validateTimeRange(LocalTime.NOON, null));
    }
}
