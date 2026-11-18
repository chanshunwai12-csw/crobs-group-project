package hk.edu.polyu.comp.comp2021.crobs.model;

/** Immutable success/failure result returned by a Crobs command. */
public final class CommandResult {
    /** Whether the represented operation completed successfully. */
    private final boolean successful;
    /** Confirmation or error text displayed by the view. */
    private final String message;

    /**
     * Creates an immutable operation result.
     * @param successful whether the operation succeeded
     * @param message the text to show to the user
     */
    private CommandResult(boolean successful, String message) {
        this.successful = successful;
        this.message = message;
    }

    /**
     * Creates the result of a completed operation.
     * @param message the confirmation text
     * @return a successful result
     */
    public static CommandResult success(String message) {
        return new CommandResult(true, message);
    }

    /**
     * Creates the result of a rejected or failed operation.
     * @param message the reason the operation failed
     * @return an unsuccessful result
     */
    public static CommandResult failure(String message) {
        return new CommandResult(false, message);
    }

    /**
     * Reports the operation's outcome.
     * @return whether the operation succeeded
     */
    public boolean isSuccessful() {
        return successful;
    }

    /**
     * Supplies the response shown by the view.
     * @return the confirmation or error text
     */
    public String getMessage() {
        return message;
    }
}
