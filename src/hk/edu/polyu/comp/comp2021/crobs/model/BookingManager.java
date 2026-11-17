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

    private static final String NO_BOOKINGS = "You have no bookings.";

    private final CROBS context;
    private final Clock clock;

    /** @param context the shared room, user and booking data */
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

    public void deleteBooking(String bookingId) {
        User user = requireLogin();
        Booking booking = requireBooking(bookingId);
        requireOwnership(booking, user);
        context.getBookings().remove(bookingId);
    }

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

    public List<Booking> getBookingsOfRoom(String roomId) {
        requireRoom(roomId);
        return bookingsMatching(booking -> booking.getRoomId().equals(roomId));
    }

    public int cancelAllBookingsForUser(String userId) {
        Objects.requireNonNull(userId, "userId");
        return removeBookings(booking -> booking.getOwnerUserId().equals(userId));
    }

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
