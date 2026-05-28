package core;

public class SessionManager {

    private static String currentUser = "Phoenix";
    private static UserRole currentRole = UserRole.OWNER;

    public static String getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(String user) {
        currentUser = user;
    }

    public static UserRole getCurrentRole() {
        return currentRole;
    }

    public static void setCurrentRole(UserRole role) {
        currentRole = role;
    }
}
