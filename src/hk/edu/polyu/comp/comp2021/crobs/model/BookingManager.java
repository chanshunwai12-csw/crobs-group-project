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

/** Manages the bookings stored in a Crobs session. */
public class BookingManager {

    /** Response shown when the current user has no reservations. */
    private static final String NO_BOOKINGS = "You have no bookings.";

    /** Shared session data used by this component. */
    private final CROBS context;
    /** Time source used to identify future reservations. */
    private final Clock clock;

    /**
     * Creates a booking manager using the system clock for future-booking cancellation.
     * @param context the shared room, user and booking data
     * @throws NullPointerException if the context is null
     */
    public BookingManager(CROBS context) {
        this(context, Clock.systemDefaultZone());
    }

    /**
     * Creates a booking manager with a supplied time source.
     *
     * @param context the shared room, user and booking data
     * @param clock time source for determining which bookings are in the future
     * @throws NullPointerException if either argument is null
     */
    BookingManager(CROBS context, Clock clock) {
        this.context = Objects.requireNonNull(context, "context");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Reserves a room for the logged-in user after validating availability.
     * @param roomId the existing room ID
     * @param dateText the date in yyyy-MM-dd format
     * @param startText the inclusive start in HH:mm format
     * @param endText the exclusive end in HH:mm format
     * @param purpose the nonblank purpose
     * @return the created reservation with a new ID
     * @throws SecurityException if nobody is logged in
     * @throws NoSuchElementException if the room is unknown
     * @throws IllegalArgumentException if the date, time or purpose is invalid, or the slot conflicts
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
     * Replaces an owned reservation after validating the complete replacement.
     * The original reservation remains in place if validation fails.
     * @param bookingId the existing reservation ID
     * @param roomId the replacement room ID
     * @param dateText the replacement date in yyyy-MM-dd format
     * @param startText the replacement inclusive start in HH:mm format
     * @param endText the replacement exclusive end in HH:mm format
     * @param purpose the replacement nonblank purpose
     * @return the updated reservation with its original ID and owner
     * @throws SecurityException if nobody is logged in or the reservation belongs to another user
     * @throws NoSuchElementException if the reservation or replacement room is unknown
     * @throws IllegalArgumentException if a replacement value is invalid or the slot conflicts
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
     * Removes a reservation owned by the logged-in user.
     * @param bookingId the existing reservation ID
     * @throws SecurityException if nobody is logged in or the reservation has a different owner
     * @throws NoSuchElementException if the reservation is unknown
     */
    public void deleteBooking(String bookingId) {
        User user = requireLogin();
        Booking booking = requireBooking(bookingId);
        requireOwnership(booking, user);
        context.getBookings().remove(bookingId);
    }

    /**
     * Formats the current user's reservations in date and start-time order.
     * @return the reservation rows or a message when none exist
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
     * Lists the reservations associated with an existing room.
     * @param roomId the existing room ID
     * @return a read-only list sorted by date, start time and booking ID
     * @throws NoSuchElementException if the room is unknown
     */
    public List<Booking> getBookingsOfRoom(String roomId) {
        requireRoom(roomId);
        return bookingsMatching(booking -> booking.getRoomId().equals(roomId));
    }

    /**
     * Removes every reservation owned by an account that is being removed.
     * @param userId the owner's account ID
     * @return the number of reservations removed
     * @throws NullPointerException if the account ID is null
     */
    public int cancelAllBookingsForUser(String userId) {
        Objects.requireNonNull(userId, "userId");
        return removeBookings(booking -> booking.getOwnerUserId().equals(userId));
    }

    /**
     * Removes a deleted room's reservations whose start is after the current time.
     * @param roomId the deleted room ID
     * @return the number of reservations removed
     * @throws NullPointerException if the room ID is null
     */
    public int cancelAllFutureBookingsOfRoomId(String roomId) {
        Objects.requireNonNull(roomId, "roomId");
        LocalDateTime now = LocalDateTime.now(clock);
        return removeBookings(booking -> booking.getRoomId().equals(roomId)
                && LocalDateTime.of(booking.getDate(), booking.getStartTime()).isAfter(now));
    }

    /**
     * Checks that a user has been selected.
     * @return the currently logged-in account
     * @throws SecurityException if nobody is logged in
     */
    private User requireLogin() {
        User user = context.getCurrentUser();
        if (user == null) {
            throw new SecurityException("No user is logged in. Please login first.");
        }
        return user;
    }

    /**
     * Checks that a room exists.
     * @param roomId the room ID to check
     * @throws NoSuchElementException if the room is unknown
     */
    private void requireRoom(String roomId) {
        if (context.getRoomManager().getRoom(roomId) == null) {
            throw new NoSuchElementException("Room " + roomId + " does not exist.");
        }
    }

    /**
     * Looks up a reservation that must exist.
     * @param bookingId the requested reservation ID
     * @return the existing reservation
     * @throws NoSuchElementException if the reservation is unknown
     */
    private Booking requireBooking(String bookingId) {
        Booking booking = context.getBookings().get(bookingId);
        if (booking == null) {
            throw new NoSuchElementException("Booking " + bookingId + " does not exist.");
        }
        return booking;
    }

    /**
     * Verifies the logged-in account owns a reservation.
     * @param booking the reservation to change
     * @param user the logged-in account
     * @throws SecurityException if the owner and account IDs differ
     */
    private void requireOwnership(Booking booking, User user) {
        if (!booking.getOwnerUserId().equals(user.getId())) {
            throw new SecurityException("You can only change your own bookings.");
        }
    }

    /**
     * Rejects a missing reservation purpose.
     * @param purpose the proposed purpose
     * @throws IllegalArgumentException if the purpose is null or blank
     */
    private void requireValidPurpose(String purpose) {
        if (purpose == null || purpose.trim().isEmpty()) {
            throw new IllegalArgumentException("The purpose must not be empty.");
        }
    }

    /**
     * Validates availability while optionally excluding a reservation being modified.
     * @param roomId the requested room
     * @param date the requested date
     * @param start the inclusive start time
     * @param end the exclusive end time
     * @param excludeBookingId the reservation to ignore, or null
     * @throws IllegalArgumentException if the slot conflicts or its time range is invalid
     */
    private void requireNoConflict(String roomId, LocalDate date, LocalTime start,
                                   LocalTime end, String excludeBookingId) {
        if (context.getConflictChecker().isTimeConflict(roomId, date, start, end, excludeBookingId)) {
            throw new IllegalArgumentException("The room is already booked during that time slot.");
        }
    }

    /**
     * Collects and sorts reservations selected by a query.
     * @param filter the query predicate
     * @return an independent read-only list of matching reservations
     */
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

    /**
     * Removes matching reservations through the booking store's iterator.
     * @param filter the cancellation predicate
     * @return the number removed
     */
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
