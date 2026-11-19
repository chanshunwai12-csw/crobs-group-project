package hk.edu.polyu.comp.comp2021.crobs.model;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Log paths and live session data. */
public final class CrobsContext {
    /** HTML log path. */
    private final Path htmlLogPath;
    /** TXT log path. */
    private final Path txtLogPath;
    /** Live room lookup. */
    private final Predicate<String> roomExists;
    /** Read-only live bookings. */
    private final Collection<? extends BookingInfo> allBookings;
    /** Current-user lookup. */
    private final Supplier<? extends SessionUser> currentLoginUser;

    /**
     * Creates an empty session.
     *
     * @param htmlLogPath HTML log path
     * @param txtLogPath TXT log path
     * @throws NullPointerException if either path is null
     */
    public CrobsContext(Path htmlLogPath, Path txtLogPath) {
        this(htmlLogPath, txtLogPath, roomId -> false, Collections.emptyList(), () -> null);
    }

    /**
     * Connects log paths to live model data.
     *
     * @param htmlLogPath HTML log path
     * @param txtLogPath TXT log path
     * @param roomExists live room lookup
     * @param allBookings live bookings
     * @param currentLoginUser current-user lookup, returning null when logged out
     * @throws NullPointerException if any argument is null
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

    /** HTML destination.
     * @return the HTML log path
     */
    public Path getHtmlLogPath() {
        return htmlLogPath;
    }

    /** TXT destination.
     * @return the TXT log path
     */
    public Path getTxtLogPath() {
        return txtLogPath;
    }

    /**
     * Checks the live room collection.
     *
     * @param roomId requested room
     * @return whether the room exists
     */
    public boolean hasRoom(String roomId) {
        return roomExists.test(roomId);
    }

    /** Current reservations.
     * @return a read-only live view of bookings
     */
    public Collection<? extends BookingInfo> getAllBookings() {
        return allBookings;
    }

    /** Current login.
     * @return the current user, or null when logged out
     */
    public SessionUser getCurrentLoginUser() {
        return currentLoginUser.get();
    }
}
