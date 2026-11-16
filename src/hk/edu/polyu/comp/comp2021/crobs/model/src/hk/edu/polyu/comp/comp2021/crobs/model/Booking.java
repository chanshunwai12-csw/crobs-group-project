package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public class Booking implements Comparable<Booking> {

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
    public String getBookingId() {
        return bookingId;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public String getRoomId() {
        return roomId;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

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
        int byDate = date.compareTo(other.date);
        if (byDate != 0) {
            return byDate;
        }
        int byStart = startTime.compareTo(other.startTime);
        if (byStart != 0) {
            return byStart;
        }
        return bookingId.compareTo(other.bookingId);
    }
}