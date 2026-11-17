package hk.edu.polyu.comp.comp2021.crobs;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsController;
import hk.edu.polyu.comp.comp2021.crobs.controller.ProjectCommands;
import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsContext;
import hk.edu.polyu.comp.comp2021.crobs.view.CLIView;
import hk.edu.polyu.comp.comp2021.crobs.view.SwingView;

import javax.swing.SwingUtilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/** Entry point for the Crobs command-line and graphical application. */
public final class Application {
    private static final int ARGUMENTS_PER_OPTION = 2;
    private static final int LOG_OPTION_COUNT = 2;
    private static final String USAGE = "Usage: java hk.edu.polyu.comp.comp2021.crobs.Application"
            + " [--gui] [-html <log.html> -txt <log.txt>]";

    private Application() { }

    /**
     * @param args command-line options specifying the HTML and TXT log paths
     */
    public static void main(String[] args) {
        try {
            boolean gui = args.length > 0 && "--gui".equals(args[0]);
            CROBS crobs = new CROBS();
            CrobsContext context = parseContext(gui ? Arrays.copyOfRange(args, 1, args.length) : args, crobs);
            if (gui) {
                SwingUtilities.invokeLater(() -> SwingView.open(context, crobs));
                return;
            }
            CLIView view = new CLIView(new PrintWriter(System.out, true));
            ProjectCommands commands = new ProjectCommands(crobs, view);
            CrobsController controller = new CrobsController(context, view, commands);
            view.showMessage("Crobs started.");
            view.showMessage("Superadmin user ID: " + crobs.getSuperadminId());
            view.showMessage("Supported commands: addUser, removeUser, changeRole, login, logout, "
                    + "addRoom, modifyRoom, deleteRoom, listRoom, listAllRooms, search, "
                    + "book, modifyBooking, deleteBooking, listBookings, schedule, undo, redo, quit");
            controller.run(new BufferedReader(new InputStreamReader(System.in)));
        } catch (IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
            System.err.println(USAGE);
        } catch (IOException exception) {
            System.err.println("Session error: " + exception.getMessage());
        }
    }

    private static CrobsContext parseContext(String[] args, CROBS crobs) {
        if (args.length == 0) {
            return createContext(Paths.get("log.html"), Paths.get("log.txt"), crobs);
        }
        if (args.length != LOG_OPTION_COUNT * ARGUMENTS_PER_OPTION) {
            throw new IllegalArgumentException("Specify both log paths.");
        }

        Path htmlPath = null;
        Path txtPath = null;
        for (int index = 0; index < args.length; index += ARGUMENTS_PER_OPTION) {
            String option = args[index];
            String path = args[index + 1];
            if (path.trim().isEmpty()) {
                throw new IllegalArgumentException("Missing file path for " + option);
            }
            if ("-html".equals(option) && htmlPath == null) {
                htmlPath = Paths.get(path);
            } else if ("-txt".equals(option) && txtPath == null) {
                txtPath = Paths.get(path);
            } else {
                throw new IllegalArgumentException("Unknown or duplicate option: " + option);
            }
        }
        return createContext(htmlPath, txtPath, crobs);
    }

    private static CrobsContext createContext(Path htmlPath, Path txtPath, CROBS crobs) {
        return new CrobsContext(htmlPath, txtPath, roomId -> crobs.getRoomManager().getRoom(roomId) != null,
                crobs.getBookings().values(), crobs::getCurrentUser);
    }
}
