package hk.edu.polyu.comp.comp2021.crobs.model;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsController;
import hk.edu.polyu.comp.comp2021.crobs.controller.ProjectCommands;
import hk.edu.polyu.comp.comp2021.crobs.view.CLIView;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises bonus commands through the shared controller and its real log files. */
class BonusIntegrationTest {
    /** Capacity used by the first successful update. */
    private static final int UPDATED_CAPACITY = 20;
    /** Capacity used after branching away from redo history. */
    private static final int NEW_CAPACITY = 30;
    /** Isolated directory populated by JUnit. */
    @TempDir private Path temp;
    /** Application model shared by the controller under test. */
    private CROBS crobs;
    /** Normal command entry point for both interfaces. */
    private CrobsController controller;
    /** Paired real-file logs for each test session. */
    private CommandLogger logger;
    /** Captured command confirmations and errors. */
    private StringWriter transcript;
    /** Current schedule panel content. */
    private AtomicReference<String> display;

    /**
     * Creates an isolated application, view, controller, and paired command logs.
     * @throws IOException if the command log or input stream fails
     */
    @BeforeEach
    void setUp() throws IOException {
        crobs = new CROBS();
        transcript = new StringWriter();
        display = new AtomicReference<>("No schedule selected.");
        CLIView view = new CLIView(new PrintWriter(transcript, true), display::set);
        CrobsContext context = new CrobsContext(temp.resolve("log.html"), temp.resolve("log.txt"),
                id -> crobs.getRoomManager().getRoom(id) != null,
                crobs.getBookings().values(), crobs::getCurrentUser);
        controller = new CrobsController(context, view, new ProjectCommands(crobs, view));
        logger = new CommandLogger(context.getHtmlLogPath(), context.getTxtLogPath());
        command("login U0001");
    }

    /**
     * Closes the paired logs after each test.
     * @throws IOException if the command log or input stream fails
     */
    @AfterEach
    void closeLogs() throws IOException {
        logger.close();
    }

    /**
     * Executes one command and verifies that the session remains open.
     * @param text the full command to execute
     * @throws IOException if the command log or input stream fails
     */
    private void command(String text) throws IOException {
        assertTrue(controller.executeLine(text, logger), text);
    }

    /**
     * Verifies empty feature lists across creation, updates, search, history, and logging.
     * @throws IOException if the command log or input stream fails
     */
    @Test
    void emptyFeaturesWorkForCreationClearingAndUnrestrictedSearch() throws IOException {
        command("addRoom meetingRoom 12 \"\"");
        assertTrue(crobs.getRoomManager().getRoom("R001").getFeatures().isEmpty());
        command("addRoom meetingRoom 20 screen");
        command("search 2027-01-01 09:00 10:00 0 \"\"");
        assertTrue(transcript.toString().contains("R001 | meetingRoom"));
        assertTrue(transcript.toString().contains("R002 | meetingRoom"));
        command("modifyRoom R002 features \"\"");
        assertTrue(crobs.getRoomManager().getRoom("R002").getFeatures().isEmpty());
        command("undo");
        assertEquals(Collections.singleton("screen"), crobs.getRoomManager().getRoom("R002").getFeatures());
        command("redo");
        assertTrue(crobs.getRoomManager().getRoom("R002").getFeatures().isEmpty());
        List<String> logs = Files.readAllLines(temp.resolve("log.txt"));
        assertTrue(logs.contains("modifyRoom R002 features \"\""));
    }

    /**
     * Verifies undoing the first schedule clears it and redo restores its display.
     * @throws IOException if the command log or input stream fails
     */
    @Test
    void firstScheduleUndoClearsSelectionAndRedoRestoresIt() throws IOException {
        command("addRoom meetingRoom 12 \"\"");
        command("schedule R001 2027-01-01");
        String shown = display.get();
        assertTrue(shown.contains("Schedule for room R001"));
        command("undo");
        assertTrue(display.get().isEmpty());
        command("redo");
        assertEquals(shown, display.get());
        assertEquals(1, crobs.getRoomManager().getAllRooms().size());
    }

    /**
     * Verifies schedule history restores earlier selections in order.
     * @throws IOException if the command log or input stream fails
     */
    @Test
    void consecutiveScheduleUndoAndRedoRestoreThePreviousSelection() throws IOException {
        command("addRoom meetingRoom 12 \"\"");
        command("addRoom seminarRoom 20 screen");
        command("schedule R001 2027-01-01");
        String first = display.get();
        command("schedule R002 2027-01-01");
        String second = display.get();
        command("undo");
        assertEquals(first, display.get());
        command("redo");
        assertEquals(second, display.get());
        command("undo");
        command("undo");
        assertTrue(display.get().isEmpty());
        command("redo");
        assertEquals(first, display.get());
    }

    /**
     * Verifies a restored schedule respects the viewer's current access level.
     * @throws IOException if the command log or input stream fails
     */
    @Test
    void restoringScheduleRechecksCurrentPermissions() throws IOException {
        command("addRoom meetingRoom 12 \"\"");
        command("book R001 2027-01-01 09:00 10:00 Admin booking");
        command("addUser FormerAdmin");
        command("changeRole U0002 ADMIN");
        command("logout");
        command("login U0002");
        command("schedule R001 2027-01-01");
        assertTrue(display.get().contains("user=U0001"));
        command("undo");
        command("logout");
        command("login U0001");
        // Directly change privileges without replacing the schedule's redo entry.
        assertTrue(crobs.changeRole("U0002", "REGULAR").isSuccessful());
        command("logout");
        command("login U0002");
        command("redo");
        assertTrue(display.get().contains("Admin booking"));
        assertFalse(display.get().contains("user=U0001"));
    }

    /**
     * Verifies rejected commands preserve history and successful edits invalidate redo.
     * @throws IOException if the command log or input stream fails
     */
    @Test
    void failedCommandsPreserveRedoAndSuccessfulChangeClearsIt() throws IOException {
        command("addRoom meetingRoom 12 \"\"");
        command("modifyRoom R001 capacity 20");
        command("undo");
        command("modifyRoom R001 capacity invalid");
        command("redo");
        assertEquals(UPDATED_CAPACITY, crobs.getRoomManager().getRoom("R001").getCapacity());
        command("undo");
        command("modifyRoom R001 capacity 30");
        command("redo");
        assertTrue(transcript.toString().contains("Nothing to redo."));
        assertEquals(NEW_CAPACITY, crobs.getRoomManager().getRoom("R001").getCapacity());
    }

    /**
     * Verifies the CLI reads and executes every command exactly once.
     * @throws IOException if the command log or input stream fails
     */
    @Test
    void fullCliLoopLogsAndExecutesEveryLineOnce() throws IOException {
        CLIView view = new CLIView(new PrintWriter(transcript, true));
        CrobsContext context = new CrobsContext(temp.resolve("cli.html"), temp.resolve("cli.txt"),
                id -> crobs.getRoomManager().getRoom(id) != null,
                crobs.getBookings().values(), crobs::getCurrentUser);
        CrobsController cli = new CrobsController(context, view, new ProjectCommands(crobs, view));
        List<String> commands = Arrays.asList("addRoom meetingRoom 12 \"\"",
                "addRoom seminarRoom 20 screen", "schedule R001 2027-01-01",
                "undo", "redo", "quit");
        cli.run(new BufferedReader(new StringReader(String.join("\n", commands))));
        assertEquals(commands, Files.readAllLines(temp.resolve("cli.txt")));
        assertEquals(2, crobs.getRoomManager().getAllRooms().size());
        assertTrue(transcript.toString().contains("No schedule selected."));
        assertTrue(transcript.toString().contains("Goodbye."));
    }

    /**
     * Verifies malformed feature commands preserve the room collection.
     * @throws IOException if the command log or input stream fails
     */
    @Test
    void blankAndMalformedFeatureCommandsCannotMutateRooms() throws IOException {
        command("  ");
        command("addRoom meetingRoom 12");
        assertTrue(crobs.getRoomManager().getAllRooms().isEmpty());
        command("addRoom meetingRoom 12 \"\"");
        command("modifyRoom R001 features");
        command("modifyRoom R001 features a,,b");
        assertTrue(crobs.getRoomManager().getRoom("R001").getFeatures().isEmpty());
    }

    /**
     * Verifies deleting users and rooms restores their canceled bookings on undo.
     * @throws IOException if the command log or input stream fails
     */
    @Test
    void undoAndRedoKeepBookingCancellationsConsistent() throws IOException {
        command("addRoom meetingRoom 12 \"\"");
        command("addUser Student");
        command("logout");
        command("login U0002");
        command("book R001 2099-01-01 09:00 10:00 Study");
        command("logout");
        command("login U0001");
        command("removeUser U0002");
        assertNull(crobs.findUser("U0002"));
        assertTrue(crobs.getBookings().isEmpty());
        command("undo");
        assertNotNull(crobs.findUser("U0002"));
        assertEquals(1, crobs.getBookings().size());
        command("redo");
        assertNull(crobs.findUser("U0002"));
        assertTrue(crobs.getBookings().isEmpty());
        command("undo");
        command("deleteRoom R001");
        assertTrue(crobs.getBookings().isEmpty());
        command("undo");
        assertNotNull(crobs.getRoomManager().getRoom("R001"));
        assertEquals(1, crobs.getBookings().size());
    }

    /**
     * Verifies restoration clears schedules whose room no longer exists.
     * @throws IOException if the command log or input stream fails
     */
    @Test
    void unavailablePreviousScheduleDoesNotLeaveAStaleDisplay() throws IOException {
        command("addRoom meetingRoom 12 \"\"");
        command("addRoom meetingRoom 12 \"\"");
        command("schedule R001 2027-01-01");
        command("deleteRoom R001");
        command("schedule R002 2027-01-01");
        command("undo");
        assertTrue(display.get().isEmpty());
        assertTrue(transcript.toString().contains("No schedule selected."));
        command("redo");
        assertTrue(display.get().contains("Schedule for room R002"));
    }
}
