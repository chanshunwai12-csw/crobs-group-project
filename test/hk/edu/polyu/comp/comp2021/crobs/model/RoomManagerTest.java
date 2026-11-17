package hk.edu.polyu.comp.comp2021.crobs.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomManagerTest {
    private final roomManager rooms = new roomManager();

    @Test
    void assignsUniqueIdsAndListsRoomsById() {
        Room first = rooms.addRoom("lectureTheatre", 80, "projector,speakers");
        Room second = rooms.addRoom("computerLab", 25, "computers");
        assertEquals(Arrays.asList(first, second), rooms.getAllRooms());
        assertSame(first, rooms.getRoom(first.getRoomId()));

        assertTrue(rooms.deleteRoom(first.getRoomId()));
        assertFalse(rooms.deleteRoom(first.getRoomId()));
        assertNull(rooms.getRoom(first.getRoomId()));
        assertEquals("R003", rooms.addRoom("meetingRoom", 4, "").getRoomId());
    }

    @Test
    void modifiesOneFieldAtATimeAndRejectsInvalidUpdates() {
        Room room = rooms.addRoom("meetingRoom", 4, "projector");
        String id = room.getRoomId();
        rooms.modifyRoom(id, "roomType", "seminarRoom");
        assertEquals("seminarRoom", room.getRoomType());
        assertEquals(4, room.getCapacity());

        rooms.modifyRoom(id, "capacity", "20");
        rooms.modifyRoom(id, "features", "whiteboard");
        assertThrows(IllegalArgumentException.class, () -> rooms.modifyRoom(id, "capacity", "twenty"));
        assertThrows(IllegalArgumentException.class, () -> rooms.modifyRoom(id, "capacity", "0"));
        assertThrows(IllegalArgumentException.class, () -> rooms.modifyRoom(id, "features", "projector,"));
        assertThrows(IllegalArgumentException.class, () -> rooms.modifyRoom(id, "roomId", "R005"));
        assertThrows(IllegalArgumentException.class, () -> rooms.modifyRoom("missing", "capacity", "30"));
        assertEquals(20, room.getCapacity());
        assertEquals(Collections.singleton("whiteboard"), room.getFeatures());
    }

    @Test
    void searchMatchesMinimumCapacityAndEveryRequestedFeature() {
        Room match = rooms.addRoom("seminarRoom", 30, "projector,whiteboard,speakers");
        rooms.addRoom("meetingRoom", 10, "projector,whiteboard");
        rooms.addRoom("lectureTheatre", 80, "projector");
        assertEquals(Collections.singletonList(match), rooms.searchRooms(30, "whiteboard,projector"));
        assertEquals(3, rooms.searchRooms(0, "").size());
        assertThrows(IllegalArgumentException.class, () -> rooms.searchRooms(-1, "projector"));
        assertThrows(IllegalArgumentException.class, () -> rooms.searchRooms(0, "white board"));
    }

    @Test
    void invalidRoomDoesNotConsumeAnId() {
        assertThrows(IllegalArgumentException.class, () -> rooms.addRoom("invalid", 10, ""));
        assertThrows(IllegalArgumentException.class, () -> rooms.addRoom("meetingRoom", 10, "a\tb"));
        assertThrows(IllegalArgumentException.class, () -> rooms.addRoom("meetingRoom", 10, null));
        assertEquals("R001", rooms.addRoom("meetingRoom", 10, "").getRoomId());
    }
}
