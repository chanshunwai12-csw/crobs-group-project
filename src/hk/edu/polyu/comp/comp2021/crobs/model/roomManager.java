
package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class roomManager {

    private ArrayList<Room> rooms;
    private int nextRoomId;

    public roomManager() {
        rooms = new ArrayList<>();
        nextRoomId = 1;
    }

    public Room addRoom(String roomType, int capacity, String features) {
        Set<String> featureSet = new HashSet<>();

        if (features == null) {
            throw new IllegalArgumentException("Invalid features");
        }

        if (!features.isEmpty()) {
            String[] parts = features.split(",", -1);

            for (String feature : parts) {
                if (feature.isEmpty() || feature.contains(" ")) {
                    throw new IllegalArgumentException("Invalid features");
                }
                featureSet.add(feature);
            }
        }

        String roomId = String.format("R%03d", nextRoomId);

        Room room = new Room(roomId, roomType, capacity, featureSet);

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

        result.sort((r1, r2) ->
                r1.getRoomId().compareTo(r2.getRoomId()));

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
                Set<String> featureSet = new HashSet<>();

                if (!newValue.isEmpty()) {
                    String[] parts = newValue.split(",", -1);

                    for (String feature : parts) {
                        if (feature.isEmpty() || feature.contains(" ")) {
                            throw new IllegalArgumentException("Invalid features");
                        }

                        featureSet.add(feature);
                    }
                }

                room.setFeatures(featureSet);
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
        if (minCapacity <= 0) {
            throw new IllegalArgumentException("Invalid capacity");
        }

        if (features == null) {
            throw new IllegalArgumentException("Invalid features");
        }

        Set<String> requiredFeatures = new HashSet<>();

        if (!features.isEmpty()) {
            String[] parts = features.split(",", -1);

            for (String feature : parts) {
                if (feature.isEmpty() || feature.contains(" ")) {
                    throw new IllegalArgumentException("Invalid features");
                }

                requiredFeatures.add(feature);
            }
        }

        ArrayList<Room> result = new ArrayList<>();

        for (Room room : rooms) {
            if (room.getCapacity() >= minCapacity
                    && room.getFeatures().containsAll(requiredFeatures)) {
                result.add(room);
            }
        }

        result.sort((r1, r2) ->
                r1.getRoomId().compareTo(r2.getRoomId()));

        return result;
    }
}
