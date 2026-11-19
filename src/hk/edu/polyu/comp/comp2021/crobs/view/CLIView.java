package hk.edu.polyu.comp.comp2021.crobs.view;

import hk.edu.polyu.comp.comp2021.crobs.model.ScheduleEntry;

import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;

/** Console output for command results. */
public final class CLIView {
    /** Output writer. */
    private final PrintWriter output;

    /**
     * Sets the command output writer.
     *
     * @param output output writer
     * @throws NullPointerException if the writer is null
     */
    public CLIView(PrintWriter output) {
        this.output = Objects.requireNonNull(output);
    }

    /**
     * Prints schedule rows with the supplied owner visibility.
     *
     * @param roomId displayed room
     * @param entries permission-filtered schedule rows
     * @throws NullPointerException if the list or an entry is null
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
     * Prints and flushes an error.
     *
     * @param message error message
     */
    public void showError(String message) {
        showMessage("Error: " + message);
    }

    /**
     * Prints and flushes a message.
     *
     * @param message output text
     */
    public void showMessage(String message) {
        output.println(message);
        output.flush();
    }

    /** Prints the quit confirmation. */
    public void showGoodbye() {
        output.println("Goodbye.");
        output.flush();
    }
}
