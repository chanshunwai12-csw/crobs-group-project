package hk.edu.polyu.comp.comp2021.crobs.view;

import hk.edu.polyu.comp.comp2021.crobs.model.ScheduleEntry;

import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;

/** Prints command responses to the console. */
public final class CLIView {
    /** Writer used to display command responses. */
    private final PrintWriter output;

    /**
     * Creates a view that writes command responses to the supplied writer.
     *
     * @param output the command-output writer
     * @throws NullPointerException if the writer is {@code null}
     */
    public CLIView(PrintWriter output) {
        this.output = Objects.requireNonNull(output);
    }

    /**
     * Prints a weekly schedule using the owner IDs allowed by the model.
     *
     * @param roomId room shown in the schedule
     * @param entries booking rows with any restricted user IDs already removed
     * @throws NullPointerException if the entries list or one of its entries is {@code null}
     */
    public void showSchedule(String roomId, List<ScheduleEntry> entries) {
        output.println("Schedule for room " + roomId);
        if (entries.isEmpty()) {
            output.println("No active bookings in this week.");
        }
        for (ScheduleEntry entry : entries) {
            output.print(entry.getDate() + " " + entry.getStartTime() + "-" + entry.getEndTime()
                    + " | booking=" + entry.getBookingId() + " | purpose=" + entry.getPurpose());
            if (entry.getVisibleUserId() != null) {
                output.print(" | user=" + entry.getVisibleUserId());
            }
            output.println();
        }
        output.flush();
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
