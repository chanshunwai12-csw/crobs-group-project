package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** Booking fields used by room availability and the weekly schedule. */
public interface BookingInfo {
    /** @return the unique booking ID */
    String getBookingId();

    /** @return the booked room ID */
    String getRoomId();

    /** @return the owner user ID */
    String getUserId();

    /** @return the booking date */
    LocalDate getDate();

    /** @return the start time on the booking date */
    LocalTime getStartTime();

    /** @return the end time on the booking date */
    LocalTime getEndTime();

    /** @return the recorded purpose */
    String getPurpose();

    /** @return whether the booking is active */
    boolean isActive();
}
