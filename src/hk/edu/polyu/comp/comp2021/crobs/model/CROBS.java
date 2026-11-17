package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** Model facade for REQ2-REQ6. */
public final class CROBS {
    private static final String USER_ID_PREFIX = "U";
    private static final int USER_ID_WIDTH = 4;

    private final Map<String, User> users;
    private final Session session;
    private final BookingCancellationService bookingCancellationService;
    private final roomManager rooms = new roomManager();
    private final Map<String, Booking> bookings = new LinkedHashMap<>();
    private final BookingIds bookingIds = new BookingIds();
    private final ConflictChecker conflictChecker = new ConflictChecker(bookings.values());
    private final BookingManager bookingManager = new BookingManager(this);
    private final String superadminId;
    private int nextUserNumber;

    /**
     * Creates a new CROBS instance with built-in booking cancellation.
     */
    public CROBS() {
        this(userId -> {
            // The built-in booking manager handles cancellation.
        });
    }

    /**
     * Creates a new CROBS instance with an additional booking cancellation service.
     *
     * @param bookingCancellationService the service called when a user is removed
     * @throws IllegalArgumentException if the service is {@code null}
     */
    public CROBS(BookingCancellationService bookingCancellationService) {
        if (bookingCancellationService == null) {
            throw new IllegalArgumentException("Booking cancellation service must not be null.");
        }
        users = new LinkedHashMap<String, User>();
        session = new Session();
        this.bookingCancellationService = bookingCancellationService;
        nextUserNumber = 1;

        superadminId = generateUserId();
        users.put(superadminId, new User(superadminId, "Super Admin", Role.SUPERADMIN));
    }
    /**
     * Returns the ID of the built-in superadmin account.
     *
     * @return the superadmin user's ID
     */
    public String getSuperadminId() {
        return superadminId;
    }
    /**
     * Gets the user signed in to the current session.
     *
     * @return the signed-in user, or {@code null} if nobody is signed in
     */
    public User getCurrentUser() {
        return session.getCurrentUser();
    }
    /**
     * Looks up a user by ID.
     *
     * @param userId the ID to look up
     * @return the matching user, or {@code null} if no user has that ID
     */
    public User findUser(String userId) {
        return users.get(userId);
    }
    /**
     * Counts the user accounts currently registered in CROBS.
     *
     * @return the number of registered accounts, including the superadmin
     */
    public int getUserCount() {
        return users.size();
    }

    /** @return the room manager for this session */
    public roomManager getRoomManager() {
        return rooms;
    }

    /** @return the booking manager for this session */
    public BookingManager getBookingManager() {
        return bookingManager;
    }

    /** @return the live bookings keyed by ID */
    public Map<String, Booking> getBookings() {
        return bookings;
    }

    /** @return a new booking ID reserved for this session */
    String nextBookingId() {
        return bookingIds.next(bookings.keySet());
    }

    /** @return the conflict checker used by both search and booking */
    public ConflictChecker getConflictChecker() {
        return conflictChecker;
    }

    /**
     * Adds a new regular user when the signed-in user has admin privileges.
     *
     * @param name the name of the new user
     * @return the result of the request, including the new user ID on success
     */
    public CommandResult addUser(String name) {
        CommandResult authorizationFailure = requireAdmin();
        if (authorizationFailure != null) {
            return authorizationFailure;
        }
        if (name == null || name.trim().isEmpty()) {
            return CommandResult.failure("User name must not be empty.");
        }

        String userId = generateUserId();
        User user = new User(userId, name, Role.REGULAR);
        users.put(userId, user);
        return CommandResult.success("User created: " + userId + " (" + user.getName() + ", REGULAR)");
    }

    /**
     * Removes a regular user and cancels their bookings.
     * The signed-in user must have admin privileges.
     *
     * @param targetUserId the ID of the regular user to remove
     * @return the result indicating whether the removal succeeded
     */
    public CommandResult removeUser(String targetUserId) {
        CommandResult authorizationFailure = requireAdmin();
        if (authorizationFailure != null) {
            return authorizationFailure;
        }

        User target = users.get(targetUserId);
        if (target == null) {
            return CommandResult.failure("User ID does not exist: " + targetUserId);
        }
        if (target.getRole() != Role.REGULAR) {
            return CommandResult.failure("Only regular users can be removed.");
        }

        try {
            bookingCancellationService.cancelAllBookingsForUser(targetUserId);
            bookingManager.cancelAllBookingsForUser(targetUserId);
        } catch (RuntimeException exception) {
            return CommandResult.failure("Could not cancel the user's bookings: "
                    + exception.getMessage());
        }

        users.remove(targetUserId);
        return CommandResult.success("User removed: " + targetUserId);
    }

    /**
     * Changes another user's role when the signed-in user is the superadmin.
     *
     * @param targetUserId the ID of the user whose role will change
     * @param newRoleText the requested role, {@code REGULAR} or {@code ADMIN}
     * @return the result indicating whether the role change succeeded
     */
    public CommandResult changeRole(String targetUserId, String newRoleText) {
        User actor = session.getCurrentUser();
        if (actor == null) {
            return CommandResult.failure("Login is required.");
        }
        if (actor.getRole() != Role.SUPERADMIN) {
            return CommandResult.failure("Only the superadmin can change roles.");
        }

        User target = users.get(targetUserId);
        if (target == null) {
            return CommandResult.failure("User ID does not exist: " + targetUserId);
        }
        if (target.getRole() == Role.SUPERADMIN) {
            return CommandResult.failure("The unique superadmin's role cannot be changed.");
        }

        final Role newRole;
        try {
            newRole = Role.fromCommand(newRoleText);
        } catch (IllegalArgumentException exception) {
            return CommandResult.failure("Role must be REGULAR or ADMIN.");
        }
        if (newRole == Role.SUPERADMIN) {
            return CommandResult.failure("A second superadmin cannot be created.");
        }

        target.changeRole(newRole);
        return CommandResult.success("Role changed: " + targetUserId + " is now " + newRole);
    }

    /**
     * Signs in as an existing user when no user is currently signed in.
     *
     * @param userId the ID of the account to sign in as
     * @return the result indicating whether sign-in succeeded
     */
    public CommandResult login(String userId) {
        if (session.isLoggedIn()) {
            return CommandResult.failure("A user is already logged in; logout first.");
        }

        User user = users.get(userId);
        if (user == null) {
            return CommandResult.failure("User ID does not exist: " + userId);
        }

        session.login(user);
        return CommandResult.success("Logged in as " + user.getId() + " (" + user.getRole() + ")");
    }

    /**
     * Signs out the current user and clears the session.
     *
     * @return a success result if a user was signed out, or a failure result
     *         if nobody was signed in
     */
    public CommandResult logout() {
        if (!session.isLoggedIn()) {
            return CommandResult.failure("No user is currently logged in.");
        }

        String userId = session.getCurrentUser().getId();
        session.logout();
        return CommandResult.success("Logged out: " + userId);
    }

    /**
     * Parses and executes a user-management or session command (REQ2–REQ6).
     *
     * @param commandLine the full command line, including any arguments
     * @return the command result, or a failure result if the input is
     *         blank, malformed, or unrecognized
     */
    public CommandResult execute(String commandLine) {
        if (commandLine == null || commandLine.trim().isEmpty()) {
            return CommandResult.failure("Command must not be empty.");
        }

        String[] tokens = commandLine.trim().split("\\s+", 2);
        String command = tokens[0];
        String arguments = tokens.length == 1 ? "" : tokens[1];

        UserCommand userCommand;
        if ("addUser".equals(command)) {
            if (arguments.isEmpty()) {
                return CommandResult.failure("Usage: addUser name");
            }
            userCommand = new UserCommand.AddUserCommand(arguments);
        } else if ("removeUser".equals(command)) {
            if (!isOneArgument(arguments)) {
                return CommandResult.failure("Usage: removeUser targetUserID");
            }
            userCommand = new UserCommand.RemoveUserCommand(arguments);
        } else if ("changeRole".equals(command)) {
            String[] parts = arguments.split("\\s+");
            if (parts.length != 2) {
                return CommandResult.failure("Usage: changeRole targetUserID newRole");
            }
            userCommand = new UserCommand.ChangeRoleCommand(parts[0], parts[1]);
        } else if ("login".equals(command)) {
            if (!isOneArgument(arguments)) {
                return CommandResult.failure("Usage: login userID");
            }
            userCommand = new UserCommand.LoginCommand(arguments);
        } else if ("logout".equals(command)) {
            if (!arguments.isEmpty()) {
                return CommandResult.failure("Usage: logout");
            }
            userCommand = new UserCommand.LogoutCommand();
        } else {
            return CommandResult.failure("Unknown command: " + command);
        }
        return userCommand.execute(this);
    }

    private CommandResult requireAdmin() {
        User actor = session.getCurrentUser();
        if (actor == null) {
            return CommandResult.failure("Login is required.");
        }
        if (!actor.getRole().isAdmin()) {
            return CommandResult.failure("Admin privileges are required.");
        }
        return null;
    }

    private String generateUserId() {
        String number = Integer.toString(nextUserNumber++);
        StringBuilder id = new StringBuilder(USER_ID_PREFIX);
        for (int i = number.length(); i < USER_ID_WIDTH; i++) {
            id.append('0');
        }
        id.append(number);
        return id.toString();
    }

    private boolean isOneArgument(String arguments) {
        return arguments.matches("\\S+");
    }
}
