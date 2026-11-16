package hk.edu.polyu.comp.comp2021.crobs.controller;

import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.BookingManager;
import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;
import hk.edu.polyu.comp.comp2021.crobs.model.TimeUtil;
import hk.edu.polyu.comp.comp2021.crobs.model.User;
import hk.edu.polyu.comp.comp2021.crobs.model.roomManager;
import hk.edu.polyu.comp.comp2021.crobs.view.CLIView;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/** Routes the project's existing user, room, and booking operations from the shared loop. */
public final class ProjectCommands {
    private static final int ROOM_CHANGE_SIZE = 4;
    private static final int SEARCH_SIZE = 6;
    private static final int BOOK_SIZE = 6;
    private static final int MODIFY_BOOKING_SIZE = 7;

    private final CROBS crobs;
    private final roomManager rooms;
    private final BookingManager bookings;
    private final CLIView view;

    /**
     * @param crobs shared application model
     * @param view command output
     */
    public ProjectCommands(CROBS crobs, CLIView view) {
        this.crobs = crobs;
        this.rooms = crobs.getRoomManager();
        this.bookings = crobs.getBookingManager();
        this.view = view;
    }

    /**
     * @param line the original line already recorded by the command loop
     */
    public void execute(String line) {
        String[] args = CommandParser.parse(line);
        switch (args[0]) {
            case "addRoom":
                requireArgs(args, ROOM_CHANGE_SIZE, "addRoom roomType capacity features");
                requireAdmin();
                Room room = rooms.addRoom(args[1], Integer.parseInt(args[2]), args[3]);
                view.showMessage("Room created: " + room.getRoomId());
                break;
            case "modifyRoom":
                requireArgs(args, ROOM_CHANGE_SIZE, "modifyRoom roomId field newValue");
                requireAdmin();
                rooms.modifyRoom(args[1], args[2], args[3]);
                view.showMessage("Room updated: " + args[1]);
                break;
            case "deleteRoom":
                requireArgs(args, 2, "deleteRoom roomId");
                requireAdmin();
                if (!rooms.deleteRoom(args[1])) {
                    throw new IllegalArgumentException("Unknown room ID: " + args[1]);
                }
                bookings.cancelAllFutureBookingsOfRoomId(args[1]);
                view.showMessage("Room removed: " + args[1]);
                break;
            case "listRoom":
                requireArgs(args, 2, "listRoom roomId");
                Room requested = rooms.getRoom(args[1]);
                if (requested == null) {
                    throw new IllegalArgumentException("Unknown room ID: " + args[1]);
                }
                showRoom(requested);
                break;
            case "listAllRooms":
                requireArgs(args, 1, "listAllRooms");
                showRooms(rooms.getAllRooms());
                break;
            case "search":
                search(args);
                break;
            case "book":
                book(line);
                break;
            case "modifyBooking":
                modifyBooking(line);
                break;
            case "deleteBooking":
                requireArgs(args, 2, "deleteBooking bookingID");
                bookings.deleteBooking(args[1]);
                view.showMessage("Booking removed: " + args[1]);
                break;
            case "listBookings":
                requireArgs(args, 1, "listBookings");
                view.showMessage(bookings.listBookings());
                break;
            default:
                view.showMessage(crobs.execute(line).getMessage());
        }
    }

    private void search(String[] args) {
        requireArgs(args, SEARCH_SIZE, "search date startTime endTime minCapacity features");
        LocalDate date = TimeUtil.parseDate(args[1]);
        LocalTime start = TimeUtil.parseTime(args[2]);
        LocalTime end = TimeUtil.parseTime(args[3]);
        TimeUtil.validateTimeRange(start, end);
        int minCapacity = Integer.parseInt(args[4]);
        String features = args[5];
        List<Room> available = rooms.searchRooms(minCapacity, features).stream()
                .filter(room -> !crobs.getConflictChecker().isTimeConflict(room.getRoomId(), date, start, end))
                .collect(Collectors.toList());
        showRooms(available);
    }

    private void book(String line) {
        String[] args = line.trim().split("\\s+", BOOK_SIZE);
        requireArgs(args, BOOK_SIZE, "book roomId date startTime endTime purpose");
        Booking booking = bookings.book(args[1], args[2], args[3], args[4], args[5]);
        view.showMessage("Booking created: " + booking.getBookingId());
    }

    private void modifyBooking(String line) {
        String[] args = line.trim().split("\\s+", MODIFY_BOOKING_SIZE);
        requireArgs(args, MODIFY_BOOKING_SIZE, "modifyBooking bookingID roomId date startTime endTime purpose");
        Booking booking = bookings.modifyBooking(args[1], args[2], args[3], args[4], args[5], args[6]);
        view.showMessage("Booking updated: " + booking.getBookingId());
    }

    private void showRooms(List<Room> found) {
        if (found.isEmpty()) {
            view.showMessage("No matching rooms.");
        }
        found.forEach(this::showRoom);
    }

    private void showRoom(Room room) {
        view.showMessage(room.getRoomId() + " | " + room.getRoomType() + " | capacity=" + room.getCapacity()
                + " | features=" + room.getFeatures().stream().sorted().collect(Collectors.joining(",")));
    }

    private void requireAdmin() {
        User user = crobs.getCurrentUser();
        if (user == null || !user.isAdmin()) {
            throw new SecurityException("Admin privileges are required.");
        }
    }

    private static void requireArgs(String[] args, int count, String syntax) {
        if (args.length != count) {
            throw new IllegalArgumentException("Usage: " + syntax);
        }
    }
}
