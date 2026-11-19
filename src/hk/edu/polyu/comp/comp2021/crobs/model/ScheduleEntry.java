package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** Schedule row with controlled owner-ID visibility. */
public final class ScheduleEntry {
    /** Booking ID. */
    private final String bookingId;
    /** Booking date. */
    private final LocalDate date;
    /** Inclusive start time. */
    private final LocalTime startTime;
    /** Exclusive end time. */
    private final LocalTime endTime;
    /** Booking purpose. */
    private final String purpose;
    /** Owner ID. */
    private final String userId;
    /** Whether the owner ID is visible. */
    private final boolean showUserId;

    /**
     * Copies booking details and owner visibility.
     *
     * @param booking source booking
     * @param showUserId whether to reveal its owner ID
     * @throws NullPointerException if the booking is null
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

    /** Booking identifier.
     * @return the booking ID
     */
    public String getBookingId() {
        return bookingId;
    }

    /** Reservation date.
     * @return the booking date
     */
    public LocalDate getDate() {
        return date;
    }

    /** Inclusive slot start.
     * @return the inclusive start time
     */
    public LocalTime getStartTime() {
        return startTime;
    }

    /** Exclusive slot end.
     * @return the exclusive end time
     */
    public LocalTime getEndTime() {
        return endTime;
    }

    /** Reservation purpose.
     * @return the booking purpose
     */
    public String getPurpose() {
        return purpose;
    }

    /** Visible booking owner.
     * @return the owner ID if visible, otherwise null
     */
    public String getVisibleUserId() {
        return showUserId ? userId : null;
    }
}
