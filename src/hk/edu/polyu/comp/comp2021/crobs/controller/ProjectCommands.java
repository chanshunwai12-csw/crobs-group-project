package hk.edu.polyu.comp.comp2021.crobs.controller;

import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.BookingManager;
import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.CommandResult;
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
    /** Token count for adding or modifying a room. */
    private static final int ROOM_CHANGE_SIZE = 4;
    /** Token count for an availability search. */
    private static final int SEARCH_SIZE = 6;
    /** Token limit that preserves the complete booking purpose. */
    private static final int BOOK_SIZE = 6;
    /** Token limit that preserves the replacement booking purpose. */
    private static final int MODIFY_BOOKING_SIZE = 7;

    /** Shared application model used by the command handler. */
    private final CROBS crobs;
    /** Room collection belonging to this session. */
    private final roomManager rooms;
    /** Live reservations used by operations and availability queries. */
    private final BookingManager bookings;
    /** Output view for command results and errors. */
    private final CLIView view;
    /**
     * Successful reversible operations for this session.
     */
    private final CommandHistory history = new CommandHistory();

    /**
     * Connects the shared command loop to the user, room and booking models.
     *
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
     * Executes a command and records successful data changes for undo.
     * @param line the submitted command line
     * @throws IllegalArgumentException if the syntax or values are invalid
     * @throws IllegalStateException if the requested history entry does not exist
     * @throws SecurityException if the current user cannot perform the operation
     */
    public void execute(String line) {
        String[] args = CommandParser.parse(line);
        if (args.length == 0) {
            return;
        }
        switch (args[0]) {
            case "undo":
                requireArgs(args, 1, "undo");
                view.showMessage(history.undo(crobs));
                return;
            case "redo":
                requireArgs(args, 1, "redo");
                view.showMessage(history.redo(crobs));
                return;
            default:
                break;
        }
        User actor = crobs.getCurrentUser();
        boolean track = actor != null && changesData(args[0]);
        CROBS.State before = track ? crobs.snapshot() : null;
        if (dispatch(line, args) && track) {
            history.record(actor.getId(), line, before, crobs.snapshot());
        }
    }

    /**
     * Identifies operations that change model data.
     * @param command the command name
     * @return whether it needs before and after snapshots
     */
    private static boolean changesData(String command) {
        switch (command) {
            case "addUser":
            case "removeUser":
            case "changeRole":
            case "addRoom":
            case "modifyRoom":
            case "deleteRoom":
            case "book":
            case "modifyBooking":
            case "deleteBooking":
                return true;
            default:
                return false;
        }
    }

    /**
     * Records a successful schedule display so redo can display it again.
     * @param line the original schedule command
     * @param replay the display operation to repeat
     */
    void recordSchedule(String line, Runnable replay) {
        history.recordSchedule(crobs.getCurrentUser().getId(), line, replay);
    }

    /**
     * Dispatches a validated command to its model and reports the outcome.
     * @param line the original command line
     * @param args parsed command tokens
     * @return whether execution succeeded
     * @throws IllegalArgumentException if the syntax or values are invalid
     * @throws SecurityException if the operation is not authorized
     * @throws java.util.NoSuchElementException if a required ID does not exist
     */
    private boolean dispatch(String line, String[] args) {
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
                int cancelled = bookings.cancelAllFutureBookingsOfRoomId(args[1]);
                view.showMessage("Room removed: " + args[1]);
                view.showMessage("Future bookings cancelled: " + cancelled);
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
                CommandResult result = crobs.execute(line);
                view.showMessage(result.getMessage());
                return result.isSuccessful();
        }
        return true;
    }

    /**
     * Displays rooms meeting feature, capacity and time-availability constraints.
     * @param args the search command and its five arguments
     * @throws IllegalArgumentException if a search value or argument count is invalid
     */
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

    /**
     * Creates a reservation while preserving spaces in its purpose.
     * @param line the original booking command
     * @throws IllegalArgumentException if its syntax, values or requested slot are invalid
     * @throws SecurityException if nobody is logged in
     * @throws java.util.NoSuchElementException if the room is unknown
     */
    private void book(String line) {
        String[] args = line.trim().split("\\s+", BOOK_SIZE);
        requireArgs(args, BOOK_SIZE, "book roomId date startTime endTime purpose");
        Booking booking = bookings.book(args[1], args[2], args[3], args[4], args[5]);
        view.showMessage("Booking created: " + booking.getBookingId());
    }

    /**
     * Changes an owned reservation while preserving spaces in its purpose.
     * @param line the original modification command
     * @throws IllegalArgumentException if its syntax, values or replacement slot are invalid
     * @throws SecurityException if the user cannot change the reservation
     * @throws java.util.NoSuchElementException if a required ID is unknown
     */
    private void modifyBooking(String line) {
        String[] args = line.trim().split("\\s+", MODIFY_BOOKING_SIZE);
        requireArgs(args, MODIFY_BOOKING_SIZE, "modifyBooking bookingID roomId date startTime endTime purpose");
        Booking booking = bookings.modifyBooking(args[1], args[2], args[3], args[4], args[5], args[6]);
        view.showMessage("Booking updated: " + booking.getBookingId());
    }

    /**
     * Displays room rows or an empty-result message.
     * @param found the rooms to display in their supplied order
     */
    private void showRooms(List<Room> found) {
        if (found.isEmpty()) {
            view.showMessage("No matching rooms.");
        }
        found.forEach(this::showRoom);
    }

    /**
     * Displays one room with feature names in alphabetical order.
     * @param room the room to display
     */
    private void showRoom(Room room) {
        view.showMessage(room.getRoomId() + " | " + room.getRoomType() + " | capacity=" + room.getCapacity()
                + " | features=" + room.getFeatures().stream().sorted().collect(Collectors.joining(",")));
    }

    /**
     * Checks authorization for room-management commands.
     * @throws SecurityException if nobody is logged in or the user lacks admin privileges
     */
    private void requireAdmin() {
        User user = crobs.getCurrentUser();
        if (user == null || !user.isAdmin()) {
            throw new SecurityException("Admin privileges are required.");
        }
    }

    /**
     * Enforces a command's token count.
     * @param args the parsed command tokens
     * @param count the required token count
     * @param syntax the syntax displayed on failure
     * @throws IllegalArgumentException if the token count differs
     */
    private static void requireArgs(String[] args, int count, String syntax) {
        if (args.length != count) {
            throw new IllegalArgumentException("Usage: " + syntax);
        }
    }
}
