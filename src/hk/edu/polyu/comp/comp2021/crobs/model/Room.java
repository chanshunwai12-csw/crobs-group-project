
package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.HashSet;
import java.util.Set;

/** A room and the attributes users can search for. */
public final class Room {

    /** Identifier of the reserved or represented room. */
    private final String roomId;
    /** One of the four supported room categories. */
    private String roomType;
    /** Maximum positive number of occupants. */
    private int capacity;
    /** Validated feature names available in the room. */
    private Set<String> features;

    /**
     * Creates a room after validating all of its attributes.
     * @param roomId the unique, nonblank room ID
     * @param roomType one of the four supported room types
     * @param capacity the positive maximum number of occupants
     * @param features the room's individual feature names
     * @throws IllegalArgumentException if an ID, type, capacity or feature is invalid
     */
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

    /**
     * Identifies this room in commands and bookings.
     * @return the room ID
     */
    public String getRoomId() {
        return roomId;
    }

    /**
     * Supplies the room's category.
     * @return the supported room type
     */
    public String getRoomType() {
        return roomType;
    }

    /**
     * Supplies the maximum number of occupants.
     * @return the positive room capacity
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Supplies a defensive copy of the features available in this room.
     * @return an independent set of feature names
     */
    public Set<String> getFeatures() {
        return new HashSet<>(features);
    }

    /**
     * Changes the category after validating the supported type names.
     * @param roomType lectureTheatre, computerLab, seminarRoom or meetingRoom
     * @throws IllegalArgumentException if the type is null or unsupported
     */
    public void setRoomType(String roomType) {
        if (!"lectureTheatre".equals(roomType)
                && !"computerLab".equals(roomType)
                && !"seminarRoom".equals(roomType)
                && !"meetingRoom".equals(roomType)) {
            throw new IllegalArgumentException("Invalid room type");
        }

        this.roomType = roomType;
    }

    /**
     * Changes the maximum number of occupants.
     * @param capacity the replacement positive capacity
     * @throws IllegalArgumentException if the capacity is not positive
     */
    public void setCapacity(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Invalid capacity");
        }

        this.capacity = capacity;
    }

    /**
     * Replaces the feature names only after every name has been validated.
     * @param features the replacement set, which may be empty
     * @throws IllegalArgumentException if the set is null or a name is null, empty,
     *         contains whitespace or contains a comma
     */
    public void setFeatures(Set<String> features) {
        if (features == null) {
            throw new IllegalArgumentException("Invalid features");
        }

        Set<String> validated = new HashSet<>(features);
        for (String feature : validated) {
            if (feature == null || feature.isEmpty() || feature.indexOf(',') >= 0
                    || feature.codePoints().anyMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
                throw new IllegalArgumentException("Invalid features");
            }
        }
        this.features = validated;
    }
}
