package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

/** Booking details that may be printed for the current viewer. */
public final class ScheduleEntry {
    private final String bookingId;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String purpose;
    private final Optional<String> visibleUserId;

    /**
     * Copies a booking's display fields and applies the owner's visibility rule.
     *
     * @param booking the source booking
     * @param showUserId whether the viewer is allowed to see the owner ID
     * @throws NullPointerException if the booking is {@code null}
     */
    ScheduleEntry(BookingInfo booking, boolean showUserId) {
        bookingId = booking.getBookingId();
        date = booking.getDate();
        startTime = booking.getStartTime();
        endTime = booking.getEndTime();
        purpose = booking.getPurpose();
        visibleUserId = showUserId ? Optional.ofNullable(booking.getUserId()) : Optional.empty();
    }

    /**
     * Identifies the booking represented by this display row.
     * @return the booking ID
     */
    public String getBookingId() {
        return bookingId;
    }

    /**
     * Supplies the date displayed in the schedule row.
     * @return the booking date
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * Supplies the beginning of the displayed time slot.
     * @return the start time
     */
    public LocalTime getStartTime() {
        return startTime;
    }

    /**
     * Supplies the exclusive end of the displayed time slot.
     * @return the end time
     */
    public LocalTime getEndTime() {
        return endTime;
    }

    /**
     * Supplies the booking purpose displayed alongside the time slot.
     * @return the purpose
     */
    public String getPurpose() {
        return purpose;
    }

    /**
     * Supplies the owner ID after applying the viewer's access privileges.
     * @return the owner ID if visible, or {@code null}
     */
    public String getVisibleUserId() {
        return visibleUserId.orElse(null);
    }
}
