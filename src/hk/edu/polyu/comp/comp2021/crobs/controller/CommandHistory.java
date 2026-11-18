package hk.edu.polyu.comp.comp2021.crobs.controller;

import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import java.util.function.Consumer;
import hk.edu.polyu.comp.comp2021.crobs.model.User;

import java.util.ArrayDeque;
import java.util.Deque;

/** Session-wide history of successful reversible Crobs commands. */
final class CommandHistory {
    /** Successful changes that can currently be undone. */
    private final Deque<Change> undo = new ArrayDeque<>();
    /** Undone changes that can currently be redone. */
    private final Deque<Change> redo = new ArrayDeque<>();

    /** Creates empty undo and redo histories for one session. */
    CommandHistory() { }

    /**
     * Records a completed model change and clears the old redo history.
     * @param actorId the user who performed the command
     * @param command the original command line
     * @param before independent model state before execution
     * @param after independent model state after execution
     */
    void record(String actorId, String command, CROBS.State before, CROBS.State after) {
        record(new Change(actorId, command, before, after, null));
    }

    /**
     * Records a schedule selection with reversible display actions.
     * @param actorId the user who requested the schedule
     * @param command the original command line
     * @param replay restores the next selection for true and the previous selection for false
     */
    void recordSchedule(String actorId, String command, Consumer<Boolean> replay) {
        record(new Change(actorId, command, null, null, replay));
    }

    /**
     * Pushes a successful command and discards obsolete redo entries.
     * @param change the completed operation
     */
    private void record(Change change) {
        undo.push(change);
        redo.clear();
    }

    /**
     * Restores the state preceding the latest permitted operation.
     * @param crobs the active model
     * @return confirmation identifying the undone command
     * @throws SecurityException if the current user cannot undo the latest entry
     * @throws IllegalStateException if no operation can be undone
     */
    String undo(CROBS crobs) {
        Change change = requireChange(undo, crobs, "undo");
        String message = change.undo(crobs);
        redo.push(undo.pop());
        return message;
    }

    /**
     * Restores or repeats the latest permitted undone operation.
     * @param crobs the active model
     * @return confirmation identifying the redone command
     * @throws SecurityException if the current user cannot redo the latest entry
     * @throws IllegalStateException if no operation can be redone
     */
    String redo(CROBS crobs) {
        Change change = requireChange(redo, crobs, "redo");
        String message = change.redo(crobs);
        undo.push(redo.pop());
        return message;
    }

    /**
     * Selects the next entry only after checking its user's permission.
     * @param history the source history stack
     * @param crobs the active model
     * @param action the requested history operation
     * @return the permitted entry at the top of the stack
     * @throws SecurityException if nobody is logged in or the entry is unauthorized
     * @throws IllegalStateException if the stack is empty
     */
    private static Change requireChange(Deque<Change> history, CROBS crobs, String action) {
        User user = crobs.getCurrentUser();
        if (user == null) {
            throw new SecurityException("Login is required.");
        }
        Change change = history.peek();
        if (change == null) {
            throw new IllegalStateException("Nothing to " + action + ".");
        }
        change.requirePermission(crobs, user, action);
        return change;
    }

    /** A reversible model change or a repeatable schedule display. */
    private static final class Change {
        /** ID of the account that performed the operation. */
        private final String actorId;
        /** Original command line displayed in confirmations. */
        private final String command;
        /** State before a model change, absent for schedule displays. */
        private final CROBS.State before;
        /** State after a model change, absent for schedule displays. */
        private final CROBS.State after;
        /** Reversible display callback, absent for model changes. */
        private final Consumer<Boolean> replay;

        /**
         * Captures either a model change or a schedule display.
         * @param actorId the original user's ID
         * @param command the submitted command line
         * @param before state before a change, or null for a display
         * @param after state after a change, or null for a display
         * @param replay reversible display callback, or null for a model change
         */
        private Change(String actorId, String command, CROBS.State before, CROBS.State after, Consumer<Boolean> replay) {
            this.actorId = actorId;
            this.command = command;
            this.before = before;
            this.after = after;
            this.replay = replay;
        }

        /**
         * Checks whether this user may replay or reverse this entry.
         * @param crobs the active model
         * @param user the signed-in account
         * @param action undo or redo, for the error message
         * @throws SecurityException if the user is neither the actor nor superadmin
         */
        private void requirePermission(CROBS crobs, User user, String action) {
            if (!actorId.equals(user.getId()) && !crobs.getSuperadminId().equals(user.getId())) {
                throw new SecurityException("Only the original user or the superadmin can " + action
                        + " the latest change.");
            }
        }

        /**
         * Restores model data or the previous schedule selection.
         * @param crobs the active model
         * @return confirmation identifying the reversed command
         */
        private String undo(CROBS crobs) {
            if (replay != null) {
                replay.accept(false);
            } else if (before != null) {
                crobs.restore(before);
            }
            return "Undid: " + command;
        }

        /**
         * Restores a changed model or repeats a schedule display.
         * @param crobs the active model
         * @return confirmation identifying the repeated command
         */
        private String redo(CROBS crobs) {
            if (replay != null) {
                replay.accept(true);
            } else {
                crobs.restore(after);
            }
            return "Redid: " + command;
        }
    }
}
