package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** Booking details that may be printed for the current viewer. */
public final class ScheduleEntry {
    /** Identifier of the represented reservation. */
    private final String bookingId;
    /** Calendar date of the reservation. */
    private final LocalDate date;
    /** Inclusive beginning of the reservation time slot. */
    private final LocalTime startTime;
    /** Exclusive end of the reservation time slot. */
    private final LocalTime endTime;
    /** Purpose displayed with the reservation. */
    private final String purpose;
    /** Account ID associated with this command or display row. */
    private final String userId;
    /** Whether the display row may expose its owner ID. */
    private final boolean showUserId;

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
        userId = booking.getUserId();
        this.showUserId = showUserId;
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
        return showUserId ? userId : null;
    }
}
