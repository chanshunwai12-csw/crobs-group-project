
package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Maintains the rooms and their searchable features for one session. */
public class roomManager {

    private final ArrayList<Room> rooms;
    private int nextRoomId;

    public roomManager() {
        rooms = new ArrayList<>();
        nextRoomId = 1;
    }

    List<Room> snapshotRooms() {
        List<Room> snapshot = new ArrayList<>();
        for (Room room : rooms) {
            snapshot.add(copyRoom(room));
        }
        return snapshot;
    }

    void restoreRooms(List<Room> snapshot) {
        rooms.clear();
        for (Room room : snapshot) {
            rooms.add(copyRoom(room));
        }
    }

    private static Room copyRoom(Room room) {
        return new Room(room.getRoomId(), room.getRoomType(), room.getCapacity(), room.getFeatures());
    }

    public Room addRoom(String roomType, int capacity, String features) {
        String roomId = String.format(Locale.ROOT, "R%03d", nextRoomId);
        Room room = new Room(roomId, roomType, capacity, parseFeatures(features));

        rooms.add(room);
        nextRoomId++;

        return room;
    }


    public Room getRoom(String roomId) {
        for (Room room : rooms) {
            if (room.getRoomId().equals(roomId)) {
                return room;
            }
        }
        return null;
    }


    public ArrayList<Room> getAllRooms() {
        ArrayList<Room> result = new ArrayList<>(rooms);

        result.sort(Comparator.comparing(Room::getRoomId));

        return result;
    }


    public void modifyRoom(String roomId, String field, String newValue) {
        Room room = getRoom(roomId);

        if (room == null) {
            throw new IllegalArgumentException("Room does not exist");
        }

        if (field == null || newValue == null) {
            throw new IllegalArgumentException("Invalid input");
        }

        switch (field) {
            case "roomType":
                room.setRoomType(newValue);
                break;

            case "capacity":
                try {
                    int capacity = Integer.parseInt(newValue);
                    room.setCapacity(capacity);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid capacity");
                }
                break;

            case "features":
                room.setFeatures(parseFeatures(newValue));
                break;

            default:
                throw new IllegalArgumentException("Invalid field");
        }
    }


    public boolean deleteRoom(String roomId) {
        Room room = getRoom(roomId);

        if (room == null) {
            return false;
        }

        rooms.remove(room);
        return true;
    }


    public ArrayList<Room> searchRooms(int minCapacity, String features) {
        if (minCapacity < 0) {
            throw new IllegalArgumentException("Invalid capacity");
        }
        Set<String> requiredFeatures = parseFeatures(features);

        ArrayList<Room> result = new ArrayList<>();

        for (Room room : rooms) {
            if (room.getCapacity() >= minCapacity
                    && room.getFeatures().containsAll(requiredFeatures)) {
                result.add(room);
            }
        }

        result.sort(Comparator.comparing(Room::getRoomId));

        return result;
    }

    private Set<String> parseFeatures(String features) {
        if (features == null) {
            throw new IllegalArgumentException("Invalid features");
        }
        Set<String> parsed = new HashSet<>();
        if (!features.isEmpty()) {
            for (String feature : features.split(",", -1)) {
                if (feature.isEmpty()
                        || feature.codePoints().anyMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
                    throw new IllegalArgumentException("Invalid features");
                }
                parsed.add(feature);
            }
        }
        return parsed;
    }
}
