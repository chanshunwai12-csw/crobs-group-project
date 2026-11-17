package hk.edu.polyu.comp.comp2021.crobs.controller;

import hk.edu.polyu.comp.comp2021.crobs.model.CommandLogger;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsContext;
import hk.edu.polyu.comp.comp2021.crobs.model.ScheduleService;
import hk.edu.polyu.comp.comp2021.crobs.model.TimeUtil;
import hk.edu.polyu.comp.comp2021.crobs.view.CLIView;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

/** Runs the shared command loop and dispatches the C2 commands. */
public final class CrobsController {
    private static final int SCHEDULE_TOKEN_COUNT = 3;
    private final CrobsContext context;
    private final ScheduleService schedules;
    private final CLIView view;
    private final Consumer<String> otherCommands;
    private final BiConsumer<String, Runnable> scheduleHistory;

    /**
     * @param context the shared Crobs session
     * @param view output for command results and errors
     */
    public CrobsController(CrobsContext context, CLIView view) {
        this(context, view, line -> view.showError("Unknown command: " + CommandParser.parse(line)[0]));
    }

    /**
     * @param context the shared Crobs session
     * @param view output for command results and errors
     * @param otherCommands handler for user, room and booking commands
     */
    public CrobsController(CrobsContext context, CLIView view, Consumer<String> otherCommands) {
        this(context, view, otherCommands, (line, replay) -> { });
    }

    /**
     * @param context the shared Crobs session
     * @param view output for command results and errors
     * @param commands user, room, booking, and history handler
     */
    public CrobsController(CrobsContext context, CLIView view, ProjectCommands commands) {
        this(context, view, commands::execute, commands::recordSchedule);
    }

    private CrobsController(CrobsContext context, CLIView view, Consumer<String> otherCommands,
                            BiConsumer<String, Runnable> scheduleHistory) {
        this.context = Objects.requireNonNull(context);
        schedules = new ScheduleService(context);
        this.view = Objects.requireNonNull(view);
        this.otherCommands = Objects.requireNonNull(otherCommands);
        this.scheduleHistory = scheduleHistory;
    }

    /**
     * Logs every nonblank line before dispatch and ends on quit or end of input.
     *
     * @param input the command input stream
     * @throws IOException if the input or either log fails
     */
    public void run(BufferedReader input) throws IOException {
        try (CommandLogger logger = new CommandLogger(context.getHtmlLogPath(), context.getTxtLogPath())) {
            String line;
            while ((line = input.readLine()) != null) {
                if (!executeLine(line, logger)) {
                    return;
                }
            }
        }
    }

    /**
     * Logs and dispatches one line for either user interface.
     * @param line the submitted command
     * @param logger the active session log
     * @return whether the session should continue
     * @throws IOException if either log write fails
     */
    public boolean executeLine(String line, CommandLogger logger) throws IOException {
        String[] command = CommandParser.parse(line);
        if (command.length == 0) {
            return true;
        }
        logger.log(line);
        return dispatch(command, line);
    }

    private boolean dispatch(String[] command, String line) {
        try {
            switch (command[0]) {
                case "schedule":
                    showSchedule(command);
                    scheduleHistory.accept(line, () -> showSchedule(command));
                    break;
                case "quit":
                    if (command.length != 1) {
                        throw new IllegalArgumentException("Usage: quit");
                    }
                    view.showGoodbye();
                    return false;
                default:
                    otherCommands.accept(line);
            }
        } catch (IllegalArgumentException | IllegalStateException | SecurityException
                 | NoSuchElementException exception) {
            view.showError(exception.getMessage());
        }
        return true;
    }

    private void showSchedule(String[] command) {
        if (command.length != SCHEDULE_TOKEN_COUNT) {
            throw new IllegalArgumentException("Usage: schedule roomId weekStartDate");
        }
        view.showSchedule(command[1], schedules.schedule(command[1], TimeUtil.parseDate(command[2])));
    }
}
