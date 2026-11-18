package hk.edu.polyu.comp.comp2021.crobs.controller;

import hk.edu.polyu.comp.comp2021.crobs.model.*;
import hk.edu.polyu.comp.comp2021.crobs.view.CLIView;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class CommandIntegrationTest {
    @TempDir Path directory;
    CROBS model;
    CrobsController controller;
    CommandLogger logger;
    StringWriter transcript;
    AtomicReference<String> panel;

    @BeforeEach void setup() throws Exception {
        model = new CROBS();
        transcript = new StringWriter();
        panel = new AtomicReference<>("");
        CLIView view = new CLIView(new PrintWriter(transcript, true), panel::set);
        CrobsContext context = new CrobsContext(directory.resolve("log.html"), directory.resolve("log.txt"),
                id -> model.getRoomManager().getRoom(id) != null, model.getBookings().values(), model::getCurrentUser);
        controller = new CrobsController(context, view, new ProjectCommands(model, view));
        logger = new CommandLogger(context.getHtmlLogPath(), context.getTxtLogPath());
        command("login U0001");
        command("addRoom meetingRoom 10 wifi");
        command("addRoom seminarRoom 20 projector");
    }
    @AfterEach void close() throws Exception { logger.close(); }
    void command(String line) throws Exception { assertTrue(controller.executeLine(line, logger)); }

    @Test void emptyFeaturesWorkForCreateModifySearchAndHistory() throws Exception {
        command("addRoom meetingRoom 5 \"\"");
        assertTrue(model.getRoomManager().getRoom("R003").getFeatures().isEmpty());
        command("modifyRoom R001 features \"\"");
        assertTrue(model.getRoomManager().getRoom("R001").getFeatures().isEmpty());
        command("undo");
        assertTrue(model.getRoomManager().getRoom("R001").getFeatures().contains("wifi"));
        command("redo");
        assertTrue(model.getRoomManager().getRoom("R001").getFeatures().isEmpty());
        command("book R001 2099-01-04 10:00 11:00 Reserved");
        transcript.getBuffer().setLength(0);
        command("search 2099-01-04 10:30 11:30 0 \"\"");
        assertFalse(transcript.toString().contains("R001"));
        assertTrue(transcript.toString().contains("R002"));
        assertTrue(transcript.toString().contains("R003"));
    }

    @Test void scheduleUndoRestoresPreviousThenClearsAndRedoRestoresBoth() throws Exception {
        command("schedule R001 2099-01-04");
        String first = panel.get();
        command("schedule R002 2099-01-04");
        String second = panel.get();
        assertNotEquals(first, second);
        command("undo"); assertEquals(first, panel.get());
        command("undo"); assertEquals("", panel.get());
        assertTrue(transcript.toString().contains("No schedule selected."));
        command("redo"); assertEquals(first, panel.get());
        command("redo"); assertEquals(second, panel.get());
    }

    @Test void failedScheduleDoesNotReplaceSelectionOrHistory() throws Exception {
        command("schedule R001 2099-01-04");
        String first = panel.get();
        command("schedule R002 2099-02-30");
        assertEquals(first, panel.get());
        command("undo"); assertEquals("", panel.get());
        command("redo"); assertEquals(first, panel.get());
    }

    @Test void restoredScheduleUsesCurrentPermissionsAndClearsOnLogout() throws Exception {
        command("addUser Alice"); command("changeRole U0002 ADMIN");
        command("book R001 2099-01-04 10:00 11:00 Admin booking");
        command("logout"); command("login U0002");
        command("schedule R001 2099-01-04");
        assertTrue(panel.get().contains("user=U0001"));
        command("logout"); assertEquals("", panel.get());
        command("login U0001"); command("changeRole U0002 REGULAR");
        command("schedule R002 2099-01-04");
        command("undo");
        command("logout"); command("login U0002");
        assertTrue(panel.get().contains("Admin booking"));
        assertFalse(panel.get().contains("user=U0001"));
    }

    @Test void selectedScheduleTracksDeletedRoomsAndTheirRestoration() throws Exception {
        command("schedule R001 2099-01-04"); String initial = panel.get();
        command("deleteRoom R001"); assertEquals("", panel.get());
        command("undo"); assertEquals(initial, panel.get());
        command("redo"); assertEquals("", panel.get());
    }
}
