package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Objects;

/** Checks room availability against the live booking collection. */
public final class ConflictChecker {
    /** Live reservations used by operations and availability queries. */
    private final Collection<? extends BookingInfo> bookings;

    /**
     * Connects the checker to the session's live collection of bookings.
     *
     * @param context the shared Crobs session
     * @throws NullPointerException if the context is {@code null}
     */
    public ConflictChecker(CrobsContext context) {
        this(context.getAllBookings());
    }

    /**
     * Connects the checker directly to a live collection of bookings.
     *
     * @param bookings live bookings used by the application model
     * @throws NullPointerException if the collection is {@code null}
     */
    public ConflictChecker(Collection<? extends BookingInfo> bookings) {
        this.bookings = Objects.requireNonNull(bookings);
    }

    /**
     * Checks whether an active booking overlaps the requested room and time slot.
     * A booking ending at the requested start time does not overlap.
     *
     * @param roomId room to check
     * @param date requested booking date
     * @param start requested start time
     * @param end requested end time
     * @return whether an active booking overlaps the requested slot
     * @throws IllegalArgumentException if the time range is invalid
     */
    public boolean isTimeConflict(String roomId, LocalDate date, LocalTime start, LocalTime end) {
        return isTimeConflict(roomId, date, start, end, null);
    }

    /**
     * Checks a slot while excluding the booking being changed.
     *
     * @param roomId room to check
     * @param date requested booking date
     * @param start requested start time
     * @param end requested end time
     * @param ignoredBookingId ID of the existing booking to exclude, or null
     * @return whether another active booking overlaps the requested slot
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
