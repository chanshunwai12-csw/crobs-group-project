
package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.HashSet;
import java.util.Set;

/** A room and the attributes users can search for. */
public final class Room {

    private final String roomId;
    private String roomType;
    private int capacity;
    private Set<String> features;

    public Room(String roomId, String roomType,
                int capacity, Set<String> features) {
        if (roomId == null || roomId.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid room ID");
        }

        this.roomId = roomId;
        setRoomType(roomType);
        setCapacity(capacity);
        setFeatures(features);
    }

    public String getRoomId() {
        return roomId;
    }

    public String getRoomType() {
        return roomType;
    }

    public int getCapacity() {
        return capacity;
    }

    public Set<String> getFeatures() {
        return new HashSet<>(features);
    }

    public void setRoomType(String roomType) {
        if (!"lectureTheatre".equals(roomType)
                && !"computerLab".equals(roomType)
                && !"seminarRoom".equals(roomType)
                && !"meetingRoom".equals(roomType)) {
            throw new IllegalArgumentException("Invalid room type");
        }

        this.roomType = roomType;
    }

    public void setCapacity(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Invalid capacity");
        }

        this.capacity = capacity;
    }

    public void setFeatures(Set<String> features) {
        if (features == null) {
            throw new IllegalArgumentException("Invalid features");
        }

        this.features = new HashSet<>(features);
    }
}
