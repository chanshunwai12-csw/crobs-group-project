package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;

public class BookingManager implements BookingCancellationService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String NO_BOOKINGS = "You have no bookings.";

    private final CROBS context;

    public BookingManager(CROBS context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public Booking book(String roomId, String dateText, String startText,
                        String endText, String purpose) {
        User user = requireLogin();
        requireRoom(roomId);
        LocalDate date = parseDate(dateText);
        LocalTime start = parseTime(startText);
        LocalTime end = parseTime(endText);
        requireValidInterval(start, end);
        requireValidPurpose(purpose);
        requireNoConflict(roomId, date, start, end, null);
        Booking booking = new Booking(BookingIds.next(context.getBookings().keySet()),
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
        LocalDate date = parseDate(dateText);
        LocalTime start = parseTime(startText);
        LocalTime end = parseTime(endText);
        requireValidInterval(start, end);
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

    @Override
    public int cancelAllBookingsForUser(String userId) {
        Objects.requireNonNull(userId, "userId");
        return removeBookings(booking -> booking.getOwnerUserId().equals(userId));
    }

    public int cancelAllFutureBookingsOfRoomId(String roomId) {
        Objects.requireNonNull(roomId, "roomId");
        LocalDateTime now = LocalDateTime.now();
        return removeBookings(booking -> booking.getRoomId().equals(roomId)
                && LocalDateTime.of(booking.getDate(), booking.getEndTime()).isAfter(now));
    }

    private User requireLogin() {
        User user = context.getCurrentUser();
        if (user == null) {
            throw new SecurityException("No user is logged in. Please login first.");
        }
        return user;
    }

    private void requireRoom(String roomId) {
        if (roomId == null || !context.getRooms().containsKey(roomId)) {
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

    private void requireValidInterval(LocalTime start, LocalTime end) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be later than start time.");
        }
    }

    private void requireValidPurpose(String purpose) {
        if (purpose == null || purpose.trim().isEmpty()) {
            throw new IllegalArgumentException("The purpose must not be empty.");
        }
    }

    private void requireNoConflict(String roomId, LocalDate date, LocalTime start,
                                   LocalTime end, String excludeBookingId) {
        if (hasConflict(roomId, date, start, end, excludeBookingId)) {
            throw new IllegalArgumentException("The room is already booked during that time slot.");
        }
    }

    private boolean hasConflict(String roomId, LocalDate date, LocalTime start,
                                LocalTime end, String excludeBookingId) {
        for (Booking booking : context.getBookings().values()) {
            if (booking.getBookingId().equals(excludeBookingId)) {
                continue;
            }
            if (!booking.getRoomId().equals(roomId)) {
                continue;
            }
            if (!booking.getDate().equals(date)) {
                continue;
            }
            if (start.isBefore(booking.getEndTime()) && booking.getStartTime().isBefore(end)) {
                return true;
            }
        }
        return false;
    }

    private static LocalDate parseDate(String text) {
        if (text == null) {
            throw new IllegalArgumentException("The date must not be empty.");
        }
        try {
            return LocalDate.parse(text, DATE_FORMAT);
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("Invalid date: " + text + " (expected yyyy-MM-dd).");
        }
    }

    private static LocalTime parseTime(String text) {
        if (text == null) {
            throw new IllegalArgumentException("The time must not be empty.");
        }
        try {
            return LocalTime.parse(text, TIME_FORMAT);
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("Invalid time: " + text + " (expected HH:mm).");
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
