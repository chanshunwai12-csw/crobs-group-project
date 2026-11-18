package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Creates, changes, deletes and queries the bookings of one Crobs session.
 *
 * <p>All data lives in the shared {@link CROBS} instance. Business failures are
 * reported with standard JDK exceptions so the controller can print one line.</p>
 */
public class BookingManager {

    private static final String NO_BOOKINGS = "You have no bookings.";

    private final CROBS context;
    private final Clock clock;

    /** @param context the shared user, room and booking data */
    public BookingManager(CROBS context) {
        this(context, Clock.systemDefaultZone());
    }

    /**
     * @param context the shared room, user and booking data
     * @param clock time source for determining which bookings are in the future
     */
    BookingManager(CROBS context, Clock clock) {
        this.context = Objects.requireNonNull(context, "context");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Creates a booking for the current user.
     *
     * @param roomId    id of an existing room
     * @param dateText  date in {@code yyyy-MM-dd}
     * @param startText start time in {@code HH:mm}
     * @param endText   end time in {@code HH:mm}
     * @param purpose   purpose of the booking
     * @return the new booking
     * @throws SecurityException        if nobody is logged in
     * @throws java.util.NoSuchElementException if the room does not exist
     * @throws IllegalArgumentException if the input is malformed or the slot is taken
     */
    public Booking book(String roomId, String dateText, String startText,
                        String endText, String purpose) {
        User user = requireLogin();
        requireRoom(roomId);
        LocalDate date = TimeUtil.parseDate(dateText);
        LocalTime start = TimeUtil.parseTime(startText);
        LocalTime end = TimeUtil.parseTime(endText);
        TimeUtil.validateTimeRange(start, end);
        requireValidPurpose(purpose);
        requireNoConflict(roomId, date, start, end, null);
        Booking booking = new Booking(context.nextBookingId(),
                user.getId(), roomId, date, start, end, purpose);
        context.getBookings().put(booking.getBookingId(), booking);
        return booking;
    }

    /**
     * Updates a booking owned by the current user. The id and the owner never change.
     *
     * @param bookingId id of the booking to change
     * @param roomId    new room id
     * @param dateText  new date in {@code yyyy-MM-dd}
     * @param startText new start time in {@code HH:mm}
     * @param endText   new end time in {@code HH:mm}
     * @param purpose   new purpose
     * @return the updated booking
     * @throws SecurityException        if nobody is logged in or the booking belongs to someone else
     * @throws java.util.NoSuchElementException if the booking or the room does not exist
     * @throws IllegalArgumentException if the input is malformed or the new slot is taken
     */
    public Booking modifyBooking(String bookingId, String roomId, String dateText,
                                 String startText, String endText, String purpose) {
        User user = requireLogin();
        Booking existing = requireBooking(bookingId);
        requireOwnership(existing, user);
        requireRoom(roomId);
        LocalDate date = TimeUtil.parseDate(dateText);
        LocalTime start = TimeUtil.parseTime(startText);
        LocalTime end = TimeUtil.parseTime(endText);
        TimeUtil.validateTimeRange(start, end);
        requireValidPurpose(purpose);
        requireNoConflict(roomId, date, start, end, bookingId);
        Booking updated = new Booking(bookingId, existing.getOwnerUserId(),
                roomId, date, start, end, purpose);
        context.getBookings().put(bookingId, updated);
        return updated;
    }

    /**
     * Deletes a booking owned by the current user.
     *
     * @param bookingId id of the booking to delete
     * @throws SecurityException        if nobody is logged in or the booking belongs to someone else
     * @throws java.util.NoSuchElementException if the booking does not exist
     */
    public void deleteBooking(String bookingId) {
        User user = requireLogin();
        Booking booking = requireBooking(bookingId);
        requireOwnership(booking, user);
        context.getBookings().remove(bookingId);
    }

    /**
     * Lists the bookings of the current user, sorted by date and start time.
     *
     * @return one display line per booking, or a message when there is none
     * @throws SecurityException if nobody is logged in
     */
    public String listBookings() {
        User user = requireLogin();
        List<Booking> mine = bookingsMatching(booking ->
                booking.getOwnerUserId().equals(user.getId()));
        if (mine.isEmpty()) {
            return NO_BOOKINGS;
        }
        StringBuilder text = new StringBuilder();
        for (Booking booking : mine) {
            if (text.length() > 0) {
                text.append(System.lineSeparator());
            }
            text.append(booking.describe());
        }
        return text.toString();
    }

    /**
     * Returns a read-only snapshot of the bookings of one room.
     *
     * @param roomId id of the room
     * @return an unmodifiable list, sorted by date and start time
     * @throws java.util.NoSuchElementException if the room does not exist
     */
    public List<Booking> getBookingsOfRoom(String roomId) {
        requireRoom(roomId);
        return bookingsMatching(booking -> booking.getRoomId().equals(roomId));
    }

    /**
     * Cancels every booking of one user. Used when a user is removed.
     *
     * @param userId id of the user being removed
     * @return the number of cancelled bookings
     */
    public int cancelAllBookingsForUser(String userId) {
        Objects.requireNonNull(userId, "userId");
        return removeBookings(booking -> booking.getOwnerUserId().equals(userId));
    }

    /**
     * Cancels the future bookings of one room. Used when a room is deleted.
     *
     * @param roomId id of the room being deleted
     * @return the number of cancelled bookings
     */
    public int cancelAllFutureBookingsOfRoomId(String roomId) {
        Objects.requireNonNull(roomId, "roomId");
        LocalDateTime now = LocalDateTime.now(clock);
        return removeBookings(booking -> booking.getRoomId().equals(roomId)
                && LocalDateTime.of(booking.getDate(), booking.getStartTime()).isAfter(now));
    }

    private User requireLogin() {
        User user = context.getCurrentUser();
        if (user == null) {
            throw new SecurityException("No user is logged in. Please login first.");
        }
        return user;
    }

    private void requireRoom(String roomId) {
        if (context.getRoomManager().getRoom(roomId) == null) {
            throw new NoSuchElementException("Room " + roomId + " does not exist.");
        }
    }

    private Booking requireBooking(String bookingId) {
        Booking booking = context.getBookings().get(bookingId);
        if (booking == null) {
            throw new NoSuchElementException("Booking " + bookingId + " does not exist.");
        }
        return booking;
    }

    private void requireOwnership(Booking booking, User user) {
        if (!booking.getOwnerUserId().equals(user.getId())) {
            throw new SecurityException("You can only change your own bookings.");
        }
    }

    private void requireValidPurpose(String purpose) {
        if (purpose == null || purpose.trim().isEmpty()) {
            throw new IllegalArgumentException("The purpose must not be empty.");
        }
    }

    private void requireNoConflict(String roomId, LocalDate date, LocalTime start,
                                   LocalTime end, String excludeBookingId) {
        if (context.getConflictChecker().isTimeConflict(roomId, date, start, end, excludeBookingId)) {
            throw new IllegalArgumentException("The room is already booked during that time slot.");
        }
    }

    private List<Booking> bookingsMatching(Predicate<Booking> filter) {
        List<Booking> result = new ArrayList<>();
        for (Booking booking : context.getBookings().values()) {
            if (filter.test(booking)) {
                result.add(booking);
            }
        }
        Collections.sort(result);
        return Collections.unmodifiableList(result);
    }

    private int removeBookings(Predicate<Booking> filter) {
        int removed = 0;
        Iterator<Map.Entry<String, Booking>> iterator = context.getBookings().entrySet().iterator();
        while (iterator.hasNext()) {
            if (filter.test(iterator.next().getValue())) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }
}
