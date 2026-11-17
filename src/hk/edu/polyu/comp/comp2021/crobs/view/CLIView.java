package hk.edu.polyu.comp.comp2021.crobs.view;

import hk.edu.polyu.comp.comp2021.crobs.model.ScheduleEntry;

import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;

/** Prints command responses to the console. */
public final class CLIView {
    private final PrintWriter output;

    /**
     * @param output the command-output writer
     */
    public CLIView(PrintWriter output) {
        this.output = Objects.requireNonNull(output);
    }

    /**
     * @param roomId room shown in the schedule
     * @param entries booking rows with any restricted user IDs already removed
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
     * @param message the user-facing error message
     */
    public void showError(String message) {
        showMessage("Error: " + message);
    }

    /**
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
