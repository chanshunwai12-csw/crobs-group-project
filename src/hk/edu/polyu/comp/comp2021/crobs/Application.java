package hk.edu.polyu.comp.comp2021.crobs;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsController;
import hk.edu.polyu.comp.comp2021.crobs.controller.ProjectCommands;
import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsContext;
import hk.edu.polyu.comp.comp2021.crobs.view.CLIView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/** Entry point for the Crobs command-line application. */
public final class Application {
    private static final int ARGUMENTS_PER_OPTION = 2;
    private static final int LOG_OPTION_COUNT = 2;
    private static final String USAGE = "Usage: java hk.edu.polyu.comp.comp2021.crobs.Application"
            + " -html <log.html> -txt <log.txt>";

    private Application() { }

    /**
     * Starts a command-line session and writes commands to the two log files.
     * Reports invalid launch options or log failures on standard error.
     *
     * @param args command-line options specifying the HTML and TXT log paths
     */
    public static void main(String[] args) {
        try {
            CROBS crobs = new CROBS();
            CrobsContext context = parseContext(args, crobs);
            CLIView view = new CLIView(new PrintWriter(System.out, true));
            ProjectCommands commands = new ProjectCommands(crobs, view);
            CrobsController controller = new CrobsController(context, view, commands::execute);
            view.showMessage("Crobs started.");
            view.showMessage("Superadmin user ID: " + crobs.getSuperadminId());
            view.showMessage("Supported commands: addUser, removeUser, changeRole, login, logout, "
                    + "addRoom, modifyRoom, deleteRoom, listRoom, listAllRooms, search, "
                    + "book, modifyBooking, deleteBooking, listBookings, schedule, quit");
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

        Map<String, Path> logPaths = new HashMap<>();
        for (int index = 0; index < args.length; index += ARGUMENTS_PER_OPTION) {
            String option = args[index];
            String path = args[index + 1];
            if (path.trim().isEmpty()) {
                throw new IllegalArgumentException("Missing file path for " + option);
            }
            if (!("-html".equals(option) || "-txt".equals(option)) || logPaths.containsKey(option)) {
                throw new IllegalArgumentException("Unknown or duplicate option: " + option);
            }
            logPaths.put(option, Paths.get(path));
        }
        return createContext(logPaths.get("-html"), logPaths.get("-txt"), crobs);
    }

    private static CrobsContext createContext(Path htmlPath, Path txtPath, CROBS crobs) {
        return new CrobsContext(htmlPath, txtPath, roomId -> crobs.getRoomManager().getRoom(roomId) != null,
                crobs.getBookings().values(), crobs::getCurrentUser);
    }
}
