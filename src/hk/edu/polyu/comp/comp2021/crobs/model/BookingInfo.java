package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** Booking fields used by room availability and the weekly schedule. */
public interface BookingInfo {
    /**
     * Identifies this booking within the session.
     * @return the unique booking ID
     */
    String getBookingId();

    /**
     * Identifies the room reserved by this booking.
     * @return the booked room ID
     */
    String getRoomId();

    /**
     * Identifies the user who owns this booking.
     * @return the owner user ID
     */
    String getUserId();

    /**
     * Supplies the calendar date used for conflict and schedule queries.
     * @return the booking date
     */
    LocalDate getDate();

    /**
     * Supplies the beginning of the reserved time slot.
     * @return the start time on the booking date
     */
    LocalTime getStartTime();

    /**
     * Supplies the exclusive end of the reserved time slot.
     * @return the end time on the booking date
     */
    LocalTime getEndTime();

    /**
     * Supplies the purpose displayed in the weekly schedule.
     * @return the recorded purpose
     */
    String getPurpose();

    /**
     * Indicates whether availability and schedule queries should include this booking.
     * @return whether the booking is active
     */
    boolean isActive();
}
