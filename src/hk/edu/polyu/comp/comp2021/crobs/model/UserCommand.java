package hk.edu.polyu.comp.comp2021.crobs.model;

/** A parsed user command that can be executed against the Crobs model. */
abstract class UserCommand {
    /** Executes the operation represented by this command. */
    abstract CommandResult execute(CROBS crobs);

    static final class AddUserCommand extends UserCommand {
        private final String name;

        AddUserCommand(String name) {
            this.name = name;
        }

        @Override
        CommandResult execute(CROBS crobs) {
            return crobs.addUser(name);
        }
    }

    static final class RemoveUserCommand extends UserCommand {
        private final String userId;

        RemoveUserCommand(String userId) {
            this.userId = userId;
        }

        @Override
        CommandResult execute(CROBS crobs) {
            return crobs.removeUser(userId);
        }
    }

    static final class ChangeRoleCommand extends UserCommand {
        private final String userId;
        private final String role;

        ChangeRoleCommand(String userId, String role) {
            this.userId = userId;
            this.role = role;
        }

        @Override
        CommandResult execute(CROBS crobs) {
            return crobs.changeRole(userId, role);
        }
    }

    static final class LoginCommand extends UserCommand {
        private final String userId;

        LoginCommand(String userId) {
            this.userId = userId;
        }

        @Override
        CommandResult execute(CROBS crobs) {
            return crobs.login(userId);
        }
    }

    static final class LogoutCommand extends UserCommand {
        @Override
        CommandResult execute(CROBS crobs) {
            return crobs.logout();
        }
    }
}
