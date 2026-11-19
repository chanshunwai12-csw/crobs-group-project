package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Weekly schedules with owner-ID visibility checks. */
public final class ScheduleService {
    /** Days in a weekly schedule. */
    private static final int DAYS_PER_WEEK = 7;
    /** Shared session context. */
    private final CrobsContext context;

    /**
     * Uses the session's live data.
     *
     * @param context shared session
     * @throws NullPointerException if the context is null
     */
    public ScheduleService(CrobsContext context) {
        this.context = Objects.requireNonNull(context);
    }

    /**
     * Lists seven days of active bookings, sorted by date and start time.
     * Admins see all owner IDs; regular users see only their own.
     *
     * @param roomId displayed room
     * @param weekStartDate first day, inclusive
     * @return the visible schedule rows
     * @throws IllegalStateException if nobody is logged in
     * @throws IllegalArgumentException if the room does not exist
     * @throws NullPointerException if the start date is null
     */
    public List<ScheduleEntry> schedule(String roomId, LocalDate weekStartDate) {
        SessionUser viewer = context.getCurrentLoginUser();
        if (viewer == null) {
            throw new IllegalStateException("Log in before viewing a schedule.");
        }
        if (!context.hasRoom(roomId)) {
            throw new IllegalArgumentException("Unknown room ID: " + roomId);
        }
        Objects.requireNonNull(weekStartDate, "weekStartDate");

        List<ScheduleEntry> entries = new ArrayList<>();
        for (BookingInfo booking : context.getAllBookings()) {
            long daysAfterStart = ChronoUnit.DAYS.between(weekStartDate, booking.getDate());
            if (booking.isActive() && booking.getRoomId().equals(roomId)
                    && daysAfterStart >= 0 && daysAfterStart < DAYS_PER_WEEK) {
                boolean showUserId = viewer.isAdmin() || viewer.getUserId().equals(booking.getUserId());
                entries.add(new ScheduleEntry(booking, showUserId));
            }
        }
        entries.sort(Comparator.comparing(ScheduleEntry::getDate).thenComparing(ScheduleEntry::getStartTime));
        return entries;
    }
}
