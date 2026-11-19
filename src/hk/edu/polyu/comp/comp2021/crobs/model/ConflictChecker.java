package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Objects;

/** Checks time conflicts against live bookings. */
public final class ConflictChecker {
    /** Live bookings. */
    private final Collection<? extends BookingInfo> bookings;

    /**
     * Uses the context's live bookings.
     *
     * @param context shared session
     * @throws NullPointerException if the context is null
     */
    public ConflictChecker(CrobsContext context) {
        this(context.getAllBookings());
    }

    /**
     * Uses a live booking collection.
     *
     * @param bookings live bookings
     * @throws NullPointerException if the collection is null
     */
    public ConflictChecker(Collection<? extends BookingInfo> bookings) {
        this.bookings = Objects.requireNonNull(bookings);
    }

    /**
     * Checks active bookings for overlap; adjacent slots are allowed.
     *
     * @param roomId requested room
     * @param date requested date
     * @param start inclusive start
     * @param end exclusive end
     * @return whether the slot conflicts
     * @throws IllegalArgumentException if the time range is invalid
     */
    public boolean isTimeConflict(String roomId, LocalDate date, LocalTime start, LocalTime end) {
        return isTimeConflict(roomId, date, start, end, null);
    }

    /**
     * Checks overlap, excluding the booking being modified.
     *
     * @param roomId requested room
     * @param date requested date
     * @param start inclusive start
     * @param end exclusive end
     * @param ignoredBookingId booking to exclude, or null
     * @return whether another booking conflicts
     * @throws IllegalArgumentException if the time range is invalid
     */
    public boolean isTimeConflict(String roomId, LocalDate date, LocalTime start, LocalTime end,
                                  String ignoredBookingId) {
        TimeUtil.validateTimeRange(start, end);
        for (BookingInfo booking : bookings) {
            if (booking.isActive() && booking.getRoomId().equals(roomId) && booking.getDate().equals(date)
                    && !booking.getBookingId().equals(ignoredBookingId)
                    && start.isBefore(booking.getEndTime()) && booking.getStartTime().isBefore(end)) {
                return true;
            }
        }
        return false;
    }
}
