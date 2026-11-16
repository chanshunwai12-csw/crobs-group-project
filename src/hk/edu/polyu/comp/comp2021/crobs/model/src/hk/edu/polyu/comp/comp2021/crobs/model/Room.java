package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class Room {

    private final String roomId;
    private final String roomType;
    private final int capacity;
    private final Set<String> features;

    public Room(String roomId, String roomType, int capacity, Set<String> features) {
        this.roomId = Objects.requireNonNull(roomId, "roomId");
        this.roomType = Objects.requireNonNull(roomType, "roomType");
        this.capacity = capacity;
        this.features = new LinkedHashSet<>(Objects.requireNonNull(features, "features"));
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
        return Collections.unmodifiableSet(features);
    }
}