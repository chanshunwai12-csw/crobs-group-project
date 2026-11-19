package hk.edu.polyu.comp.comp2021.crobs.model;

/** User identity and schedule permissions. */
public interface SessionUser {
    /** User identifier.
     * @return the user ID
     */
    String getUserId();

    /** Administrative access.
     * @return whether the user is an admin or superadmin
     */
    boolean isAdmin();
}
