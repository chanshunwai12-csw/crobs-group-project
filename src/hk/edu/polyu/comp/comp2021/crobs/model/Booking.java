package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/** Immutable reservation with an owner, room, date, time slot and purpose. */
public class Booking implements Comparable<Booking>, BookingInfo {

    /** Identifier of the represented reservation. */
    private final String bookingId;
    /** Account ID of the reservation owner. */
    private final String ownerUserId;
    /** Identifier of the reserved or represented room. */
    private final String roomId;
    /** Calendar date of the reservation. */
    private final LocalDate date;
    /** Inclusive beginning of the reservation time slot. */
    private final LocalTime startTime;
    /** Exclusive end of the reservation time slot. */
    private final LocalTime endTime;
    /** Purpose displayed with the reservation. */
    private final String purpose;

    /**
     * Creates a reservation whose end time is after its start time.
     * @param bookingId the reservation's unique ID
     * @param ownerUserId the owning account ID
     * @param roomId the reserved room ID
     * @param date the reservation date
     * @param startTime the inclusive beginning of the slot
     * @param endTime the exclusive end of the slot
     * @param purpose the nonblank reservation purpose
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if the purpose is blank or the time range is invalid
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

    /** {@inheritDoc} */
    @Override
    public String getBookingId() {
        return bookingId;
    }

    /**
     * Identifies the account that owns this reservation.
     * @return the owner account ID
     */
    public String getOwnerUserId() {
        return ownerUserId;
    }

    /** {@inheritDoc} */
    @Override
    public String getUserId() {
        return getOwnerUserId();
    }

    /**
     * Reports that a reservation in the live store participates in availability queries.
     * Cancellation removes the reservation from that store.
     * @return {@code true}
     */
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

    /**
     * Formats the reservation for its owner's booking list.
     * @return the ID, room, date, time range and purpose
     */
    public String describe() {
        return bookingId + " | " + roomId + " | " + date + " " + startTime + "-" + endTime + " | " + purpose;
    }

    /**
     * Formats the reservation with its owner for authorized administrative callers.
     * @return the owner ID together with the reservation details
     */
    public String describeWithOwner() {
        return bookingId + " | " + ownerUserId + " | " + roomId + " | " + date
                + " " + startTime + "-" + endTime + " | " + purpose;
    }

    /**
     * Orders reservations by date, start time and then booking ID.
     * @param other the reservation to compare with
     * @return a negative, zero or positive result according to the ordering
     * @throws NullPointerException if the other reservation is null
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
