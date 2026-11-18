package hk.edu.polyu.comp.comp2021.crobs.model;

/** A parsed user command that can be executed against the Crobs model. */
abstract class UserCommand {
    /** Creates a parsed command whose operation is supplied by a subclass. */
    UserCommand() {
    }

    /**
     * Executes the operation represented by this command.
     * @param crobs the shared application model
     * @return the operation's success or failure result
     */
    abstract CommandResult execute(CROBS crobs);

    /** Adds a regular user through the authorized model operation. */
    static final class AddUserCommand extends UserCommand {
        /** Account display name. */
        private final String name;

        /**
         * Stores the new user's display name.
         * @param name the requested display name
         */
        AddUserCommand(String name) {
            this.name = name;
        }

        @Override
        CommandResult execute(CROBS crobs) {
            return crobs.addUser(name);
        }
    }

    /** Removes a regular user and their bookings. */
    static final class RemoveUserCommand extends UserCommand {
        /** Account ID associated with this command or display row. */
        private final String userId;

        /**
         * Stores the ID of the user to remove.
         * @param userId the target account ID
         */
        RemoveUserCommand(String userId) {
            this.userId = userId;
        }

        @Override
        CommandResult execute(CROBS crobs) {
            return crobs.removeUser(userId);
        }
    }

    /** Changes a user's role through the superadmin operation. */
    static final class ChangeRoleCommand extends UserCommand {
        /** Account ID associated with this command or display row. */
        private final String userId;
        /** Account access level or requested role name. */
        private final String role;

        /**
         * Stores the target account and requested role.
         * @param userId the target account ID
         * @param role the requested role name
         */
        ChangeRoleCommand(String userId, String role) {
            this.userId = userId;
            this.role = role;
        }

        @Override
        CommandResult execute(CROBS crobs) {
            return crobs.changeRole(userId, role);
        }
    }

    /** Selects an existing account for the current session. */
    static final class LoginCommand extends UserCommand {
        /** Account ID associated with this command or display row. */
        private final String userId;

        /**
         * Stores the account ID to log in.
         * @param userId the existing account ID
         */
        LoginCommand(String userId) {
            this.userId = userId;
        }

        @Override
        CommandResult execute(CROBS crobs) {
            return crobs.login(userId);
        }
    }

    /** Ends the current login. */
    static final class LogoutCommand extends UserCommand {
        /** Creates a command that needs no arguments. */
        LogoutCommand() {
        }

        @Override
        CommandResult execute(CROBS crobs) {
            return crobs.logout();
        }
    }
}
