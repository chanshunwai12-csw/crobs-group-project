package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** Owns session data and enforces the user-management permissions for REQ2-REQ6. */
public final class CROBS {
    /** Prefix used for automatically generated account IDs. */
    private static final String USER_ID_PREFIX = "U";
    /** Minimum number of digits in an account ID. */
    private static final int USER_ID_WIDTH = 4;

    /** Accounts keyed by their unique IDs. */
    private final Map<String, User> users;
    /** Current login state for this application instance. */
    private final Session session;
    /** Additional cancellation callback run before account removal. */
    private final BookingCancellationService bookingCancellationService;
    /** Room collection belonging to this session. */
    private final roomManager rooms = new roomManager();
    /** Live reservations used by operations and availability queries. */
    private final Map<String, Booking> bookings = new LinkedHashMap<>();
    /** Booking ID allocator retained for the entire session. */
    private final BookingIds bookingIds = new BookingIds();
    /** Shared availability checker over the live booking collection. */
    private final ConflictChecker conflictChecker = new ConflictChecker(bookings.values());
    /** Reservation operations connected to this application instance. */
    private final BookingManager bookingManager = new BookingManager(this);
    /** ID of the unique automatically created superadmin. */
    private final String superadminId;
    /** Next number used to allocate an account ID. */
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
        users = new LinkedHashMap<>();
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

    /**
     * Supplies access to this session's room data.
     * @return the room manager for this session
     */
    public roomManager getRoomManager() {
        return rooms;
    }

    /**
     * Supplies the booking operations connected to this session.
     * @return the booking manager for this session
     */
    public BookingManager getBookingManager() {
        return bookingManager;
    }

    /**
     * Supplies the shared booking store used by the booking manager and queries.
     * @return the live bookings keyed by ID
     */
    public Map<String, Booking> getBookings() {
        return bookings;
    }

    /**
     * Reserves a booking ID that will not be reused by this allocator.
     * @return a new booking ID reserved for this session
     */
    String nextBookingId() {
        return bookingIds.next(bookings.keySet());
    }

    /**
     * Supplies availability checking against the live bookings.
     * @return the conflict checker used by both search and booking
     */
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

        int cancelledBookings;
        try {
            bookingCancellationService.cancelAllBookingsForUser(targetUserId);
            cancelledBookings = bookingManager.cancelAllBookingsForUser(targetUserId);
        } catch (RuntimeException exception) {
            return CommandResult.failure("Could not cancel the user's bookings: "
                    + exception.getMessage());
        }

        users.remove(targetUserId);
        return CommandResult.success("User removed: " + targetUserId
                + " (bookings cancelled: " + cancelledBookings + ")");
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
        User currentUser = session.getCurrentUser();
        if (currentUser == null) {
            return CommandResult.failure("No user is currently logged in.");
        }

        String userId = currentUser.getId();
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
            if (hasInvalidSingleArgument(arguments)) {
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
            if (hasInvalidSingleArgument(arguments)) {
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

    /**
     * Checks administrative privileges for a user-management operation.
     * @return an authorization failure, or {@code null} when the current user is an admin
     */
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

    /**
     * Allocates the next account ID for this session.
     * @return an ID with the user prefix and a zero-padded number
     */
    private String generateUserId() {
        String number = Integer.toString(nextUserNumber++);
        StringBuilder id = new StringBuilder(USER_ID_PREFIX);
        for (int i = number.length(); i < USER_ID_WIDTH; i++) {
            id.append('0');
        }
        id.append(number);
        return id.toString();
    }

    /**
     * Validates commands that accept exactly one whitespace-free argument.
     * @param arguments the text following the command name
     * @return whether the argument is absent or contains whitespace
     */
    private boolean hasInvalidSingleArgument(String arguments) {
        return !arguments.matches("\\S+");
    }
}
