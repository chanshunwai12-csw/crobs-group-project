package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.Collection;

public final class BookingIds {

    private static final String PREFIX = "B";
    private static final int WIDTH = 3;

    private BookingIds() {
    }

    public static String next(Collection<String> existingIds) {
        int max = 0;
        for (String id : existingIds) {
            if (id != null && id.startsWith(PREFIX)) {
                try {
                    max = Math.max(max, Integer.parseInt(id.substring(PREFIX.length())));
                } catch (NumberFormatException ignored) {
                    max = max;
                }
            }
        }
        return PREFIX + String.format("%0" + WIDTH + "d", max + 1);
    }
}