package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public class Booking implements Comparable<Booking>, BookingInfo {

    private final String bookingId;
    private final String ownerUserId;
    private final String roomId;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String purpose;

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

    public String getOwnerUserId() {
        return ownerUserId;
    }

    /** @return the owner ID used by the schedule */
    @Override
    public String getUserId() {
        return getOwnerUserId();
    }

    /** @return whether this booking is active while present in the booking map */
    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public String getRoomId() {
        return roomId;
    }

    @Override
    public LocalDate getDate() {
        return date;
    }

    @Override
    public LocalTime getStartTime() {
        return startTime;
    }

    @Override
    public LocalTime getEndTime() {
        return endTime;
    }

    @Override
    public String getPurpose() {
        return purpose;
    }

    public String describe() {
        return bookingId + " | " + roomId + " | " + date + " " + startTime + "-" + endTime + " | " + purpose;
    }

    public String describeWithOwner() {
        return bookingId + " | " + ownerUserId + " | " + roomId + " | " + date
                + " " + startTime + "-" + endTime + " | " + purpose;
    }

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
