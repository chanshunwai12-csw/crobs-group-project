package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class CROBS {

    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<String, Room> rooms = new LinkedHashMap<>();
    private final Map<String, Booking> bookings = new LinkedHashMap<>();
    private int nextUserNumber = 1;
    private User currentUser;

    public Map<String, User> getUsers() {
        return users;
    }

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public Map<String, Booking> getBookings() {
        return bookings;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public int takeNextUserNumber() {
        return nextUserNumber++;
    }
}