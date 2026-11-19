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

/** Logs commands and dispatches schedules, quit and other operations. */
public final class CrobsController {
    /** Schedule command token count. */
    private static final int SCHEDULE_TOKEN_COUNT = 3;
    /** Shared session context. */
    private final CrobsContext context;
    /** Weekly schedule service. */
    private final ScheduleService schedules;
    /** Command output. */
    private final CLIView view;
    /** Handler for other commands. */
    private final Consumer<String> otherCommands;

    /**
     * Handles schedules, quit and unknown commands.
     *
     * @param context shared session
     * @param view command output
     * @throws NullPointerException if either argument is null
     */
    public CrobsController(CrobsContext context, CLIView view) {
        this(context, view, line -> view.showError("Unknown command: " + CommandParser.parse(line)[0]));
    }

    /**
     * Connects the loop to the other command handlers.
     *
     * @param context shared session
     * @param view command output
     * @param otherCommands handler for user, room and booking commands
     * @throws NullPointerException if any argument is null
     */
    public CrobsController(CrobsContext context, CLIView view, Consumer<String> otherCommands) {
        this.context = Objects.requireNonNull(context);
        schedules = new ScheduleService(context);
        this.view = Objects.requireNonNull(view);
        this.otherCommands = Objects.requireNonNull(otherCommands);
    }

    /**
     * Logs and dispatches nonblank lines until quit or EOF.
     *
     * @param input command input
     * @throws IOException if reading or logging fails
     */
    public void run(BufferedReader input) throws IOException {
        try (CommandLogger logger = new CommandLogger(context.getHtmlLogPath(), context.getTxtLogPath())) {
            String line = input.readLine();
            while (line != null) {
                String[] command = CommandParser.parse(line);
                if (command.length > 0) {
                    logger.log(line);
                    if (!dispatch(command, line)) {
                        return;
                    }
                }
                line = input.readLine();
            }
        }
    }

    /**
     * Dispatches a logged command and reports validation errors.
     *
     * @param command parsed tokens
     * @param line original input
     * @return whether to continue the loop
     */
    private boolean dispatch(String[] command, String line) {
        try {
            switch (command[0]) {
                case "schedule":
                    showSchedule(command);
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

    /**
     * Validates and displays a weekly schedule.
     *
     * @param command schedule command tokens
     * @throws IllegalArgumentException if syntax, room or date is invalid
     * @throws IllegalStateException if nobody is logged in
     */
    private void showSchedule(String[] command) {
        if (command.length != SCHEDULE_TOKEN_COUNT) {
            throw new IllegalArgumentException("Usage: schedule roomId weekStartDate");
        }
        view.showSchedule(command[1], schedules.schedule(command[1], TimeUtil.parseDate(command[2])));
    }
}
