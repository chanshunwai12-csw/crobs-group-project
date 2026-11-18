package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.Locale;
import java.util.Set;

/** Allocates booking IDs for one session. */
final class BookingIds {

    /** Locale-independent format for generated booking IDs. */
    private static final String FORMAT = "B%03d";
    /** Next sequence number reserved by this allocator. */
    private long nextNumber = 1;

    /** Creates an allocator whose first reservation number is one. */
    BookingIds() {
    }

    /**
     * Allocates an ID while skipping reservations already in the store.
     *
     * @param existingIds IDs of bookings already in the model
     * @return the next unused booking ID
     */
    String next(Set<String> existingIds) {
        String id;
        do {
            id = String.format(Locale.ROOT, FORMAT, nextNumber++);
        } while (existingIds.contains(id));
        return id;
    }
}
