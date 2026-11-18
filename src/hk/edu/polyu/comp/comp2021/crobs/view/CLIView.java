package hk.edu.polyu.comp.comp2021.crobs.view;

import hk.edu.polyu.comp.comp2021.crobs.model.ScheduleEntry;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;
import java.util.List;
import java.util.Objects;

/** Prints command responses to the console. */
public final class CLIView {
    /** Writer used to display command responses. */
    private final PrintWriter output;

    /** Current rendered schedule, separate from the immutable command transcript. */
    private String scheduleText = "";
    /** Receives the current schedule for an optional graphical panel. */
    private final Consumer<String> scheduleDisplay;

    /**
     * Creates a view that writes command responses to the supplied writer.
     *
     * @param output the command-output writer
     * @throws NullPointerException if the writer is {@code null}
     */
    public CLIView(PrintWriter output) {
        this(output, text -> { });
    }

    /**
     * Connects output and a replaceable schedule panel.
     * @param output command transcript writer
     * @param scheduleDisplay consumer of the current schedule text
     */
    public CLIView(PrintWriter output, Consumer<String> scheduleDisplay) {
        this.output = Objects.requireNonNull(output);
        this.scheduleDisplay = Objects.requireNonNull(scheduleDisplay);
    }

    /**
     * Prints a weekly schedule using the owner IDs allowed by the model.
     *
     * @param roomId room shown in the schedule
     * @param entries booking rows with any restricted user IDs already removed
     * @throws NullPointerException if the entries list or one of its entries is {@code null}
     */
    public void showSchedule(String roomId, List<ScheduleEntry> entries) {
        updateSchedule(roomId, entries);
        announceSchedule();
    }

    /**
     * Refreshes the replaceable schedule without appending to the transcript.
     * @param roomId selected room
     * @param entries bookings filtered for the currently logged-in user
     */
    public void updateSchedule(String roomId, List<ScheduleEntry> entries) {
        StringWriter buffer = new StringWriter();
        PrintWriter rendered = new PrintWriter(buffer);
        rendered.println("Schedule for room " + roomId);
        if (entries.isEmpty()) {
            rendered.println("No active bookings in this week.");
        }
        for (ScheduleEntry entry : entries) {
            rendered.print(entry.getDate() + " " + entry.getStartTime() + "-" + entry.getEndTime()
                    + " | booking=" + entry.getBookingId() + " | purpose=" + entry.getPurpose());
            if (entry.getVisibleUserId() != null) {
                rendered.print(" | user=" + entry.getVisibleUserId());
            }
            rendered.println();
        }
        rendered.flush();
        scheduleText = buffer.toString();
        scheduleDisplay.accept(scheduleText);
    }

    /** Clears the current schedule while preserving the command transcript. */
    public void clearSchedule() {
        scheduleText = "";
        scheduleDisplay.accept(scheduleText);
    }

    /** Announces the restored schedule, or that no schedule is selected. */
    public void announceSchedule() {
        showMessage(scheduleText.isEmpty() ? "No schedule selected." : scheduleText.trim());
    }

    /**
     * Prints a validation or access error and flushes the output.
     *
     * @param message the user-facing error message
     */
    public void showError(String message) {
        showMessage("Error: " + message);
    }

    /**
     * Prints a command result and flushes the output.
     *
     * @param message the command result or session information
     */
    public void showMessage(String message) {
        output.println(message);
        output.flush();
    }

    /** Prints confirmation that the session is ending. */
    public void showGoodbye() {
        output.println("Goodbye.");
        output.flush();
    }
}
