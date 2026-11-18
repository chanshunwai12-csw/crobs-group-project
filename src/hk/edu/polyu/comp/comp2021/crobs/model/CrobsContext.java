package hk.edu.polyu.comp.comp2021.crobs.model;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Shared session data and paths consumed by the Crobs modules. */
public final class CrobsContext {
    /** Destination configured for the HTML command log. */
    private final Path htmlLogPath;
    /** Destination configured for the plain-text command log. */
    private final Path txtLogPath;
    /** Lookup against the live room collection. */
    private final Predicate<String> roomExists;
    /** Read-only live view of the session reservations. */
    private final Collection<? extends BookingInfo> allBookings;
    /** Lookup of the current login state. */
    private final Supplier<? extends SessionUser> currentLoginUser;

    /**
     * Creates an empty session for the C2 entry point.
     *
     * @param htmlLogPath destination for the HTML log
     * @param txtLogPath destination for the TXT log
     * @throws NullPointerException if either log path is {@code null}
     */
    public CrobsContext(Path htmlLogPath, Path txtLogPath) {
        this(htmlLogPath, txtLogPath, roomId -> false, Collections.emptyList(), () -> null);
    }

    /**
     * Connects the session to the same live data used by the other modules.
     *
     * @param htmlLogPath destination for the HTML log
     * @param txtLogPath destination for the TXT log
     * @param roomExists lookup against the shared room collection
     * @param allBookings live collection of bookings
     * @param currentLoginUser lookup of the current user, which may return null
     * @throws NullPointerException if any argument is {@code null}
     */
    public CrobsContext(Path htmlLogPath, Path txtLogPath, Predicate<String> roomExists,
                        Collection<? extends BookingInfo> allBookings,
                        Supplier<? extends SessionUser> currentLoginUser) {
        this.htmlLogPath = Objects.requireNonNull(htmlLogPath);
        this.txtLogPath = Objects.requireNonNull(txtLogPath);
        this.roomExists = Objects.requireNonNull(roomExists);
        this.allBookings = Collections.unmodifiableCollection(allBookings);
        this.currentLoginUser = Objects.requireNonNull(currentLoginUser);
    }

    /**
     * Supplies the destination for the session's HTML command table.
     * @return the configured HTML log path
     */
    public Path getHtmlLogPath() {
        return htmlLogPath;
    }

    /**
     * Supplies the destination for the session's plain-text command log.
     * @return the configured TXT log path
     */
    public Path getTxtLogPath() {
        return txtLogPath;
    }

    /**
     * Checks the room collection supplied by the shared application model.
     *
     * @param roomId the requested room ID
     * @return whether the room exists in the live room data
     */
    public boolean hasRoom(String roomId) {
        return roomExists.test(roomId);
    }

    /**
     * Exposes current bookings so later model updates are visible to queries.
     * @return a read-only, live view of the session's bookings
     */
    public Collection<? extends BookingInfo> getAllBookings() {
        return allBookings;
    }

    /**
     * Reads the current login state from the shared user model.
     * @return the current user, or {@code null} if no user is logged in
     */
    public SessionUser getCurrentLoginUser() {
        return currentLoginUser.get();
    }
}
