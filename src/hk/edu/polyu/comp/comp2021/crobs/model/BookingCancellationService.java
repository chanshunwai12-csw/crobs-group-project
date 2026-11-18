package hk.edu.polyu.comp.comp2021.crobs.model;
 
/** Integration point between REQ3 and the booking subsystem. */
public interface BookingCancellationService {
    /**
     * Cancels all bookings owned by the user before their account is removed.
     * @param userId the ID of the account being removed
     * @throws RuntimeException if cancellation fails
     */
    void cancelAllBookingsForUser(String userId);
}
