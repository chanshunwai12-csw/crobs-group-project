package hk.edu.polyu.comp.comp2021.crobs.model;

/** Immutable success/failure result returned by a Crobs command. */
public final class CommandResult {
    private final boolean successful;
    private final String message;

    private CommandResult(boolean successful, String message) {
        this.successful = successful;
        this.message = message;
    }

    public static CommandResult success(String message) {
        return new CommandResult(true, message);
    }

    public static CommandResult failure(String message) {
        return new CommandResult(false, message);
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getMessage() {
        return message;
    }
}
