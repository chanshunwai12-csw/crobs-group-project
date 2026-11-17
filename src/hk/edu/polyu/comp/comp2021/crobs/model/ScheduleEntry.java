package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** Booking details that may be printed for the current viewer. */
public final class ScheduleEntry {
    private final String bookingId;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String purpose;
    private final String visibleUserId;

    /**
     * @param booking the source booking
     * @param showUserId whether the viewer is allowed to see the owner ID
     */
    ScheduleEntry(BookingInfo booking, boolean showUserId) {
        bookingId = booking.getBookingId();
        date = booking.getDate();
        startTime = booking.getStartTime();
        endTime = booking.getEndTime();
        purpose = booking.getPurpose();
        visibleUserId = showUserId ? booking.getUserId() : null;
    }

    /** @return the booking ID */
    public String getBookingId() {
        return bookingId;
    }

    /** @return the booking date */
    public LocalDate getDate() {
        return date;
    }

    /** @return the start time */
    public LocalTime getStartTime() {
        return startTime;
    }

    /** @return the end time */
    public LocalTime getEndTime() {
        return endTime;
    }

    /** @return the purpose */
    public String getPurpose() {
        return purpose;
    }

    /** @return the owner ID if visible, or null */
    public String getVisibleUserId() {
        return visibleUserId;
    }
}
