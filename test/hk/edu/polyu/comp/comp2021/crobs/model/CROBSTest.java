package hk.edu.polyu.comp.comp2021.crobs.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CROBSTest {
    private CROBS crobs;
    private List<String> cancelledUserIds;

    @BeforeEach
    void setUp() {
        cancelledUserIds = new ArrayList<String>();
        crobs = new CROBS(new BookingCancellationService() {
            @Override
            public void cancelAllBookingsForUser(String userId) {
                cancelledUserIds.add(userId);
            }
        });
    }

    @Test
    void constructorCreatesOneSuperadminButStartsLoggedOut() {
        assertEquals(1, crobs.getUserCount());
        assertEquals(Role.SUPERADMIN, crobs.findUser(crobs.getSuperadminId()).getRole());
        assertNull(crobs.getCurrentUser());
    }

    @Test
    void req2AdminCanAddRegularUserWithUniqueId() {
        loginAsSuperadmin();
        assertTrue(crobs.addUser("Alice Chan").isSuccessful());
        assertTrue(crobs.addUser("Bob Lee").isSuccessful());

        User alice = crobs.findUser("U0002");
        User bob = crobs.findUser("U0003");
        assertEquals("Alice Chan", alice.getName());
        assertEquals(Role.REGULAR, alice.getRole());
        assertEquals(Role.REGULAR, bob.getRole());
    }

    @Test
    void req2LoggedOutOrRegularUserCannotAddUser() {
        assertFalse(crobs.addUser("Alice").isSuccessful());

        String regularId = createRegularUser("Alice");
        assertTrue(crobs.logout().isSuccessful());
        assertTrue(crobs.login(regularId).isSuccessful());
        assertFalse(crobs.addUser("Bob").isSuccessful());
    }

    @Test
    void req2RejectsEmptyName() {
        loginAsSuperadmin();
        assertFalse(crobs.addUser("   ").isSuccessful());
        assertEquals(1, crobs.getUserCount());
    }

    @Test
    void req3AdminRemovesRegularUserAndCancelsBookings() {
        String regularId = createRegularUser("Alice");
        assertTrue(crobs.removeUser(regularId).isSuccessful());

        assertNull(crobs.findUser(regularId));
        assertEquals(1, cancelledUserIds.size());
        assertEquals(regularId, cancelledUserIds.get(0));
    }

    @Test
    void req3CannotRemoveAdminSuperadminOrUnknownUser() {
        String regularId = createRegularUser("Alice");
        assertTrue(crobs.changeRole(regularId, "admin").isSuccessful());

        assertFalse(crobs.removeUser(regularId).isSuccessful());
        assertFalse(crobs.removeUser(crobs.getSuperadminId()).isSuccessful());
        assertFalse(crobs.removeUser("U9999").isSuccessful());
        assertTrue(cancelledUserIds.isEmpty());
    }

    @Test
    void req3DoesNotRemoveUserWhenBookingCancellationFails() {
        CROBS failingCrobs = new CROBS(new BookingCancellationService() {
            @Override
            public void cancelAllBookingsForUser(String userId) {
                throw new IllegalStateException("booking store unavailable");
            }
        });
        assertTrue(failingCrobs.login(failingCrobs.getSuperadminId()).isSuccessful());
        assertTrue(failingCrobs.addUser("Alice").isSuccessful());

        assertFalse(failingCrobs.removeUser("U0002").isSuccessful());
        assertEquals("Alice", failingCrobs.findUser("U0002").getName());
    }

    @Test
    void req4SuperadminCanPromoteAndDemoteAnotherUser() {
        String regularId = createRegularUser("Alice");
        assertTrue(crobs.changeRole(regularId, "admin").isSuccessful());
        assertEquals(Role.ADMIN, crobs.findUser(regularId).getRole());

        assertTrue(crobs.changeRole(regularId, "regular").isSuccessful());
        assertEquals(Role.REGULAR, crobs.findUser(regularId).getRole());
    }

    @Test
    void req4OrdinaryAdminCannotChangeRoles() {
        String adminId = createRegularUser("Alice");
        String targetId = createRegularUser("Bob");
        assertTrue(crobs.changeRole(adminId, "admin").isSuccessful());
        assertTrue(crobs.logout().isSuccessful());
        assertTrue(crobs.login(adminId).isSuccessful());

        assertFalse(crobs.changeRole(targetId, "admin").isSuccessful());
        assertEquals(Role.REGULAR, crobs.findUser(targetId).getRole());
    }

    @Test
    void req4CannotChangeSuperadminOrCreateSecondSuperadmin() {
        String regularId = createRegularUser("Alice");
        assertFalse(crobs.changeRole(crobs.getSuperadminId(), "regular").isSuccessful());
        assertFalse(crobs.changeRole(regularId, "superadmin").isSuccessful());
        assertFalse(crobs.changeRole(regularId, "manager").isSuccessful());
    }

    @Test
    void req5LoginFindsExistingUser() {
        loginAsSuperadmin();
        String regularId = createRegularUser("Alice");
        crobs.logout();

        assertTrue(crobs.login(regularId).isSuccessful());
        assertSame(crobs.findUser(regularId), crobs.getCurrentUser());
    }

    @Test
    void req5RejectsUnknownIdAndSecondLogin() {
        assertFalse(crobs.login("U9999").isSuccessful());
        assertTrue(crobs.login(crobs.getSuperadminId()).isSuccessful());
        assertFalse(crobs.login(crobs.getSuperadminId()).isSuccessful());
    }

    @Test
    void req6LogoutClearsCurrentUser() {
        loginAsSuperadmin();
        assertTrue(crobs.logout().isSuccessful());
        assertNull(crobs.getCurrentUser());
    }

    @Test
    void req6RejectsLogoutWhenNobodyIsLoggedIn() {
        assertFalse(crobs.logout().isSuccessful());
    }

    @Test
    void commandParserSupportsRequiredSyntaxAndMalformedCommands() {
        assertTrue(crobs.execute("login " + crobs.getSuperadminId()).isSuccessful());
        assertTrue(crobs.execute("addUser Alice Chan").isSuccessful());
        assertTrue(crobs.execute("changeRole U0002 admin").isSuccessful());
        assertTrue(crobs.execute("logout").isSuccessful());

        assertFalse(crobs.execute("removeUser").isSuccessful());
        assertFalse(crobs.execute("changeRole U0002").isSuccessful());
        assertFalse(crobs.execute("logout extra").isSuccessful());
        assertFalse(crobs.execute("unknown").isSuccessful());
    }

    private void loginAsSuperadmin() {
        assertTrue(crobs.login(crobs.getSuperadminId()).isSuccessful());
    }

    private String createRegularUser(String name) {
        if (crobs.getCurrentUser() == null) {
            loginAsSuperadmin();
        }
        int nextNumber = crobs.getUserCount() + 1;
        assertTrue(crobs.addUser(name).isSuccessful());
        return String.format("U%04d", nextNumber);
    }
}
