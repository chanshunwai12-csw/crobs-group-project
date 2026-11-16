package hk.edu.polyu.comp.comp2021.crobs.model;
 
/** Integration point between REQ3 and the booking subsystem. */
public interface BookingCancellationService {
    void cancelAllBookingsForUser(String userId);
}
