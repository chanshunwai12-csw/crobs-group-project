package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * A single room booking.
 *
 * <p>Instances are immutable. The natural order is date, then start time,
 * then booking id.</p>
 */
public class Booking implements Comparable<Booking>, BookingInfo {

    private final String bookingId;
    private final String ownerUserId;
    private final String roomId;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String purpose;

    /**
     * Creates a booking and rejects combinations that can never be valid.
     *
     * @param bookingId   unique booking id such as {@code B001}
     * @param ownerUserId id of the user who owns the booking
     * @param roomId      id of an existing room
     * @param date        date of the booking
     * @param startTime   start time
     * @param endTime     end time, must be later than {@code startTime}
     * @param purpose     non-blank purpose
     * @throws IllegalArgumentException if the purpose is blank or the interval is empty
     */
    public Booking(String bookingId, String ownerUserId, String roomId,
                   LocalDate date, LocalTime startTime, LocalTime endTime, String purpose) {
        this.bookingId = Objects.requireNonNull(bookingId, "bookingId");
        this.ownerUserId = Objects.requireNonNull(ownerUserId, "ownerUserId");
        this.roomId = Objects.requireNonNull(roomId, "roomId");
        this.date = Objects.requireNonNull(date, "date");
        this.startTime = Objects.requireNonNull(startTime, "startTime");
        this.endTime = Objects.requireNonNull(endTime, "endTime");
        this.purpose = Objects.requireNonNull(purpose, "purpose");
        if (purpose.trim().isEmpty()) {
            throw new IllegalArgumentException("The purpose must not be empty.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be later than start time.");
        }
    }

    /**
     *
     * @return the unique booking id
     */
    @Override
    public String getBookingId() {
        return bookingId;
    }

    /** @return the id of the user who owns the booking */
    public String getOwnerUserId() {
        return ownerUserId;
    }

    /** @return the owner id, used by the schedule view */
    @Override
    public String getUserId() {
        return getOwnerUserId();
    }

    /** @return true while the booking is stored in the session */
    @Override
    public boolean isActive() {
        return true;
    }

    /** @return the id of the booked room */
    @Override
    public String getRoomId() {
        return roomId;
    }

    /** @return the booking date */
    @Override
    public LocalDate getDate() {
        return date;
    }

    /** @return the start time */
    @Override
    public LocalTime getStartTime() {
        return startTime;
    }

    /** @return the end time */
    @Override
    public LocalTime getEndTime() {
        return endTime;
    }

    /** @return the purpose of the booking */
    @Override
    public String getPurpose() {
        return purpose;
    }

    /** @return one display line without the owner */
    public String describe() {
        return bookingId + " | " + roomId + " | " + date + " " + startTime + "-" + endTime + " | " + purpose;
    }

    /** @return one display line including the owner */
    public String describeWithOwner() {
        return bookingId + " | " + ownerUserId + " | " + roomId + " | " + date
                + " " + startTime + "-" + endTime + " | " + purpose;
    }

    /**
     * Orders bookings by date, then start time, then booking id.
     *
     * @param other the booking to compare with
     * @return a negative value, zero, or a positive value
     */
    @Override
    public int compareTo(Booking other) {
        int byDate = date.compareTo(other.getDate());
        if (byDate != 0) {
            return byDate;
        }
        int byStart = startTime.compareTo(other.getStartTime());
        if (byStart != 0) {
            return byStart;
        }
        return bookingId.compareTo(other.getBookingId());
    }
}
