package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.Locale;
import java.util.Set;

/** Allocates booking IDs for one session. */
final class BookingIds {

    private static final String FORMAT = "B%03d";
    private long nextNumber = 1;

    /**
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
