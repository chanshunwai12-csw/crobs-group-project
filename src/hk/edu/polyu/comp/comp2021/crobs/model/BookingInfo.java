package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** Booking data for schedules and conflict checks. */
public interface BookingInfo {
    /** Booking identifier.
     * @return the booking ID
     */
    String getBookingId();

    /** Reserved room.
     * @return the room ID
     */
    String getRoomId();

    /** Booking owner.
     * @return the owner ID
     */
    String getUserId();

    /** Reservation date.
     * @return the booking date
     */
    LocalDate getDate();

    /** Inclusive slot start.
     * @return the inclusive start time
     */
    LocalTime getStartTime();

    /** Exclusive slot end.
     * @return the exclusive end time
     */
    LocalTime getEndTime();

    /** Reservation purpose.
     * @return the booking purpose
     */
    String getPurpose();

    /** Booking status.
     * @return whether the booking is active
     */
    boolean isActive();
}
