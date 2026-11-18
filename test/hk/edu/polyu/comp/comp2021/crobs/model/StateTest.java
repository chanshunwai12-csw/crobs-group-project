package hk.edu.polyu.comp.comp2021.crobs.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StateTest {
    private CROBS crobs;
    private String roomId;

    @BeforeEach
    void setUp() {
        crobs = new CROBS();
        assertTrue(crobs.login(crobs.getSuperadminId()).isSuccessful());
        assertTrue(crobs.addUser("Student").isSuccessful());
        roomId = crobs.getRoomManager().addRoom("meetingRoom", 20, "projector").getRoomId();
    }

    @Test
    void repeatedRestoresKeepUserRolesAndRoomAttributesIndependent() {
        CROBS.State original = crobs.snapshot();
        crobs.changeRole("U0002", "ADMIN");
        crobs.getRoomManager().modifyRoom(roomId, "capacity", "40");
        crobs.getRoomManager().modifyRoom(roomId, "features", "whiteboard");
        CROBS.State changed = crobs.snapshot();
        crobs.restore(original);
        assertEquals(Role.REGULAR, crobs.findUser("U0002").getRole());
        assertEquals(20, crobs.getRoomManager().getRoom(roomId).getCapacity());
        assertTrue(crobs.getRoomManager().getRoom(roomId).getFeatures().contains("projector"));
        crobs.changeRole("U0002", "ADMIN");
        crobs.getRoomManager().modifyRoom(roomId, "capacity", "60");
        crobs.restore(original);
        assertEquals(Role.REGULAR, crobs.findUser("U0002").getRole());
        assertEquals(20, crobs.getRoomManager().getRoom(roomId).getCapacity());
        crobs.restore(changed);
        assertEquals(Role.ADMIN, crobs.findUser("U0002").getRole());
        assertEquals(40, crobs.getRoomManager().getRoom(roomId).getCapacity());
        assertTrue(crobs.getRoomManager().getRoom(roomId).getFeatures().contains("whiteboard"));
        assertEquals(crobs.getSuperadminId(), crobs.getCurrentUser().getId());
    }

    @Test
    void restorationRecoversRemovedUsersRoomsAndTheirCancelledBookings() {
        crobs.logout();
        crobs.login("U0002");
        Booking booking = crobs.getBookingManager().book(roomId, "2099-01-01", "10:00", "11:00", "Meeting");
        crobs.logout();
        crobs.login(crobs.getSuperadminId());
        CROBS.State original = crobs.snapshot();
        assertTrue(crobs.removeUser("U0002").isSuccessful());
        assertTrue(crobs.getBookings().isEmpty());
        assertTrue(crobs.getRoomManager().deleteRoom(roomId));
        crobs.restore(original);
        assertNotNull(crobs.findUser("U0002"));
        assertNotNull(crobs.getRoomManager().getRoom(roomId));
        assertSame(booking, crobs.getBookings().get(booking.getBookingId()));
        assertTrue(crobs.getConflictChecker().isTimeConflict(roomId, booking.getDate(),
                booking.getStartTime(), booking.getEndTime()));
    }

    @Test
    void restorationPreservesLoginRulesAndNeverReusesAllocatedIds() {
        CROBS.State original = crobs.snapshot();
        crobs.addUser("Temporary");
        String temporaryRoom = crobs.getRoomManager().addRoom("seminarRoom", 10, "").getRoomId();
        Booking temporaryBooking = crobs.getBookingManager().book(roomId, "2099-01-01", "10:00", "11:00", "Temporary");
        crobs.logout();
        crobs.login("U0003");
        crobs.restore(original);
        assertNull(crobs.getCurrentUser());
        crobs.restore(original);
        assertNull(crobs.getCurrentUser());
        crobs.login(crobs.getSuperadminId());
        crobs.addUser("Next");
        assertNotNull(crobs.findUser("U0004"));
        assertNull(crobs.findUser("U0003"));
        assertNotEquals(temporaryRoom, crobs.getRoomManager().addRoom("seminarRoom", 10, "").getRoomId());
        assertNotEquals(temporaryBooking.getBookingId(), crobs.getBookingManager()
                .book(roomId, "2099-01-01", "10:00", "11:00", "Next").getBookingId());
    }

    @Test
    void invalidSnapshotIsRejectedBeforeChangingTheCurrentSession() {
        assertThrows(NullPointerException.class, () -> crobs.restore(null));
        assertEquals(2, crobs.getUserCount());
        assertNotNull(crobs.getRoomManager().getRoom(roomId));
        assertEquals(crobs.getSuperadminId(), crobs.getCurrentUser().getId());
    }
}
