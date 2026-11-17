package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Selects weekly bookings and applies the schedule's owner visibility rule. */
public final class ScheduleService {
    private static final int DAYS_PER_WEEK = 7;
    private final CrobsContext context;

    /**
     * @param context the shared Crobs session
     */
    public ScheduleService(CrobsContext context) {
        this.context = Objects.requireNonNull(context);
    }

    /**
     * @param roomId room to display
     * @param weekStartDate first date to include
     * @return visible schedule entries sorted by date and start time
     * @throws IllegalStateException if no user is logged in
     * @throws IllegalArgumentException if the room ID does not exist
     */
    public List<ScheduleEntry> schedule(String roomId, LocalDate weekStartDate) {
        SessionUser viewer = context.getCurrentLoginUser();
        if (viewer == null) {
            throw new IllegalStateException("Log in before viewing a schedule.");
        }
        if (!context.hasRoom(roomId)) {
            throw new IllegalArgumentException("Unknown room ID: " + roomId);
        }

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
