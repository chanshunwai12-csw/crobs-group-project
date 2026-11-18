
package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Maintains the rooms and their searchable features for one session. */
public class roomManager {

    /** Room collection belonging to this session. */
    private final ArrayList<Room> rooms;
    /** Next number used to allocate a room ID. */
    private int nextRoomId;

    /** Creates an empty room collection with its own sequence of room IDs. */
    public roomManager() {
        rooms = new ArrayList<>();
        nextRoomId = 1;
    }

    /**
     * Adds a validated room and allocates its unique ID.
     * @param roomType one of the four supported room types
     * @param capacity the positive maximum number of occupants
     * @param features a comma-separated list of feature names, or an empty string
     * @return the new room
     * @throws IllegalArgumentException if the type, capacity or feature list is invalid
     */
    public Room addRoom(String roomType, int capacity, String features) {
        String roomId = String.format(Locale.ROOT, "R%03d", nextRoomId);
        Room room = new Room(roomId, roomType, capacity, parseFeatures(features));

        rooms.add(room);
        nextRoomId++;

        return room;
    }


    /**
     * Finds a room by its ID.
     * @param roomId the requested room ID
     * @return the matching room, or {@code null} if none exists
     */
    public Room getRoom(String roomId) {
        for (Room room : rooms) {
            if (room.getRoomId().equals(roomId)) {
                return room;
            }
        }
        return null;
    }


    /**
     * Lists the rooms in ascending ID order.
     * @return an independent list containing the current room objects
     */
    public ArrayList<Room> getAllRooms() {
        ArrayList<Room> result = new ArrayList<>(rooms);

        result.sort(Comparator.comparing(Room::getRoomId));

        return result;
    }


    /**
     * Updates a single room attribute after validating the replacement value.
     * @param roomId the existing room ID
     * @param field roomType, capacity or features
     * @param newValue the replacement value
     * @throws IllegalArgumentException if the room is unknown or the field or value is invalid
     */
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


    /**
     * Removes a room from the room collection.
     * The application command handler also cancels its future bookings.
     * @param roomId the ID to remove
     * @return whether the room existed and was removed
     */
    public boolean deleteRoom(String roomId) {
        Room room = getRoom(roomId);

        if (room == null) {
            return false;
        }

        rooms.remove(room);
        return true;
    }


    /**
     * Finds rooms meeting a minimum capacity and containing all requested features.
     * Time availability is checked by the command handler.
     * @param minCapacity the nonnegative minimum capacity
     * @param features a comma-separated feature list, or an empty string
     * @return matching rooms in ascending ID order
     * @throws IllegalArgumentException if the minimum capacity or feature list is invalid
     */
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

    /**
     * Converts a command's comma-separated features into a set.
     * @param features the feature list, which may be empty
     * @return the distinct feature names
     * @throws IllegalArgumentException if the list is null or contains empty or whitespace-bearing names
     */
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
