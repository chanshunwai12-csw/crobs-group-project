package hk.edu.polyu.comp.comp2021.crobs.model;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsController;
import hk.edu.polyu.comp.comp2021.crobs.view.CLIView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingQueriesTest {
    private static final LocalDate WEEK_START = LocalDate.of(2026, 11, 16);
    private static final String ROOM = "R101";
    private static final String OTHER_ROOM = "R102";
    private static final String OWN_ID = "U100";
    private static final String OTHER_ID = "U200";
    private final List<BookingInfo> bookings = new ArrayList<>();
    private SessionUser viewer;

    @TempDir
    private Path temp;

    @Test
    void detectsOverlapsButAllowsAdjacentBookings() {
        add("B1", ROOM, OWN_ID, WEEK_START, 10, true);
        ConflictChecker checker = new ConflictChecker(context());
        assertTrue(checker.isTimeConflict(ROOM, WEEK_START, time(10), time(11)));
        assertTrue(checker.isTimeConflict(ROOM, WEEK_START, time(9), time(10).plusMinutes(1)));
        assertTrue(checker.isTimeConflict(ROOM, WEEK_START, time(10).plusMinutes(30), time(12)));
        assertTrue(checker.isTimeConflict(ROOM, WEEK_START, time(9), time(12)));
        assertFalse(checker.isTimeConflict(ROOM, WEEK_START, time(9), time(10)));
        assertFalse(checker.isTimeConflict(ROOM, WEEK_START, time(11), time(12)));
    }

    @Test
    void ignoresInactiveBookingsOtherRoomsAndOtherDates() {
        add("B1", ROOM, OWN_ID, WEEK_START, 10, false);
        add("B2", OTHER_ROOM, OWN_ID, WEEK_START, 10, true);
        add("B3", ROOM, OWN_ID, WEEK_START.plusDays(1), 10, true);
        assertFalse(new ConflictChecker(context()).isTimeConflict(ROOM, WEEK_START, time(10), time(11)));
    }

    @Test
    void modifyingBookingIgnoresOnlyThatBookingAndReadsLiveChanges() {
        ConflictChecker checker = new ConflictChecker(context());
        add("B1", ROOM, OWN_ID, WEEK_START, 10, true);
        assertFalse(checker.isTimeConflict(ROOM, WEEK_START, time(10), time(11), "B1"));
        add("B2", ROOM, OTHER_ID, WEEK_START, 11, true);
        assertTrue(checker.isTimeConflict(ROOM, WEEK_START, time(10), time(12), "B1"));
        assertThrows(IllegalArgumentException.class,
                () -> checker.isTimeConflict(ROOM, WEEK_START, time(11), time(10)));
    }

    @Test
    void scheduleIncludesSevenDaysInDateAndStartTimeOrder() {
        populateSchedule();
        viewer = user(OWN_ID, false);
        List<ScheduleEntry> entries = new ScheduleService(context()).schedule(ROOM, WEEK_START);

        assertEquals(3, entries.size());
        assertEquals("B1", entries.get(0).getBookingId());
        assertEquals("B2", entries.get(1).getBookingId());
        assertEquals("B6", entries.get(2).getBookingId());
        assertEquals(WEEK_START.plusDays(6), entries.get(2).getDate());
        assertEquals(time(9), entries.get(0).getStartTime());
        assertEquals(time(10), entries.get(0).getEndTime());
        assertEquals("Study", entries.get(0).getPurpose());
        assertEquals(OWN_ID, entries.get(0).getVisibleUserId());
        assertNull(entries.get(1).getVisibleUserId());
    }

    @Test
    void adminCanSeeOwnersAndReceivesAnEmptyListForAWeekWithNoBookings() {
        populateSchedule();
        viewer = user("ADMIN", true);
        ScheduleService schedules = new ScheduleService(context());
        List<ScheduleEntry> entries = schedules.schedule(ROOM, WEEK_START);
        assertEquals(OWN_ID, entries.get(0).getVisibleUserId());
        assertEquals(OTHER_ID, entries.get(1).getVisibleUserId());
        assertTrue(schedules.schedule(ROOM, WEEK_START.plusWeeks(2)).isEmpty());
    }

    @Test
    void scheduleRequiresLoginAndAKnownRoom() {
        ScheduleService schedules = new ScheduleService(context());
        assertThrows(IllegalStateException.class, () -> schedules.schedule(ROOM, WEEK_START));
        viewer = user(OWN_ID, false);
        assertThrows(IllegalArgumentException.class, () -> schedules.schedule("missing", WEEK_START));
    }

    @Test
    void scheduleRejectsANullStartDateEvenWhenThereAreNoBookings() {
        viewer = user(OWN_ID, false);
        ScheduleService schedules = new ScheduleService(context());
        assertThrows(NullPointerException.class, () -> schedules.schedule(ROOM, null));
    }

    @Test
    void controllerLogsErrorsAndQuitAndDoesNotDispatchLaterInput() throws Exception {
        populateSchedule();
        viewer = user(OWN_ID, false);
        StringWriter output = new StringWriter();
        CrobsContext session = context();
        CrobsController controller = new CrobsController(session, new CLIView(new PrintWriter(output)));
        controller.run(new BufferedReader(new StringReader("   \n"
                + "schedule R101\n"
                + "schedule R101 2026-02-30\n"
                + "schedule missing 2026-11-16\n"
                + "  schedule   R101  2026-11-16  \n"
                + "quit later\nquit\nschedule R101 2026-11-16\n")));

        String text = output.toString();
        assertTrue(text.contains("Usage: schedule roomId weekStartDate"));
        assertTrue(text.contains("Invalid date. Use yyyy-MM-dd."));
        assertTrue(text.contains("Unknown room ID: missing"));
        assertTrue(text.contains("booking=B1 | purpose=Study | user=" + OWN_ID));
        assertTrue(text.contains("booking=B2 | purpose=Study" + System.lineSeparator()));
        assertFalse(text.contains("user=" + OTHER_ID));
        assertTrue(text.contains("Usage: quit"));
        assertTrue(text.endsWith("Goodbye." + System.lineSeparator()));
        assertEquals(Arrays.asList("schedule R101", "schedule R101 2026-02-30", "schedule missing 2026-11-16",
                        "  schedule   R101  2026-11-16  ", "quit later", "quit"),
                Files.readAllLines(session.getTxtLogPath(), StandardCharsets.UTF_8));
        String html = new String(Files.readAllBytes(session.getHtmlLogPath()), StandardCharsets.UTF_8);
        assertTrue(html.contains("<tr><td>6</td><td><code style=\"white-space: pre-wrap\">quit</code></td></tr>"));
        assertTrue(html.endsWith("</table></body></html>\n"));
    }

    @Test
    void controllerAcceptsEofAndClosesLogs() throws Exception {
        CrobsContext session = new CrobsContext(temp.resolve("empty.html"), temp.resolve("empty.txt"));
        CrobsController controller = new CrobsController(session, new CLIView(new PrintWriter(new StringWriter())));
        controller.run(new BufferedReader(new StringReader("   \n")));
        assertTrue(Files.readAllLines(session.getTxtLogPath(), StandardCharsets.UTF_8).isEmpty());
        String html = new String(Files.readAllBytes(session.getHtmlLogPath()), StandardCharsets.UTF_8);
        assertTrue(html.contains("<tbody>\n</tbody>"));
    }

    private CrobsContext context() {
        return new CrobsContext(temp.resolve("log.html"), temp.resolve("log.txt"),
                roomId -> ROOM.equals(roomId) || OTHER_ROOM.equals(roomId), bookings, () -> viewer);
    }

    private void populateSchedule() {
        add("B6", ROOM, OTHER_ID, WEEK_START.plusDays(6), 9, true);
        add("B2", ROOM, OTHER_ID, WEEK_START, 11, true);
        add("B1", ROOM, OWN_ID, WEEK_START, 9, true);
        add("CANCELLED", ROOM, OTHER_ID, WEEK_START.plusDays(1), 9, false);
        add("BEFORE", ROOM, OTHER_ID, WEEK_START.minusDays(1), 9, true);
        add("AFTER", ROOM, OTHER_ID, WEEK_START.plusDays(7), 9, true);
        add("OTHER_ROOM", OTHER_ROOM, OTHER_ID, WEEK_START, 9, true);
    }

    private void add(String id, String room, String owner, LocalDate date, int hour, boolean active) {
        bookings.add(new TestBooking(id, room, owner, date, time(hour), active));
    }

    private static SessionUser user(String id, boolean admin) {
        return new SessionUser() {
            @Override public String getUserId() { return id; }
            @Override public boolean isAdmin() { return admin; }
        };
    }

    private static LocalTime time(int hour) {
        return LocalTime.of(hour, 0);
    }

    private static final class TestBooking implements BookingInfo {
        private final String id;
        private final String room;
        private final String owner;
        private final LocalDate date;
        private final LocalTime start;
        private final boolean active;

        private TestBooking(String id, String room, String owner, LocalDate date, LocalTime start, boolean active) {
            this.id = id;
            this.room = room;
            this.owner = owner;
            this.date = date;
            this.start = start;
            this.active = active;
        }

        @Override public String getBookingId() { return id; }
        @Override public String getRoomId() { return room; }
        @Override public String getUserId() { return owner; }
        @Override public LocalDate getDate() { return date; }
        @Override public LocalTime getStartTime() { return start; }
        @Override public LocalTime getEndTime() { return start.plusHours(1); }
        @Override public String getPurpose() { return "Study"; }
        @Override public boolean isActive() { return active; }
    }
}
