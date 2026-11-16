package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.Arrays;
import java.util.LinkedHashSet;

public class Play {

    public static void main(String[] args) {
        CROBS context = new CROBS();
        User alice = new User("U001", "Alice", Role.REGULAR);
        context.getUsers().put(alice.getId(), alice);
        context.getRooms().put("R001", new Room("R001", "seminarRoom", 30,
                new LinkedHashSet<>(Arrays.asList("projector"))));
        context.setCurrentUser(alice);

        BookingManager manager = new BookingManager(context);
        manager.book("R001", "2026-11-20", "09:00", "10:00", "Meeting");
        manager.book("R001", "2026-11-20", "08:00", "09:00", "Lecture");

        System.out.println(manager.listBookings());
        System.out.println(manager.getBookingsOfRoom("R001").size());

        try {
            manager.book("R001", "2026-11-20", "09:30", "10:30", "Clash");
        } catch (IllegalArgumentException error) {
            System.out.println("rejected: " + error.getMessage());
        }

        System.out.println(manager.cancelAllFutureBookingsOfRoomId("R001"));
    }
}
