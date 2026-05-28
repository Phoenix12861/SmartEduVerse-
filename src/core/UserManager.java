package core;

import database.DatabaseManager;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class UserManager {



    public static boolean authenticate(
            String username,
            String password
    ) {

        try (
                Connection conn = DatabaseManager.connect();

                PreparedStatement ps =
                        conn.prepareStatement(
                                "SELECT * FROM users WHERE username=? AND password=?"
                        )
        ) {

            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            return rs.next();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }



    public static UserRole getRole(String username) {

        try (
                Connection conn = DatabaseManager.connect();

                PreparedStatement ps =
                        conn.prepareStatement(
                                "SELECT role FROM users WHERE username=?"
                        )
        ) {

            ps.setString(1, username);

            ResultSet rs = ps.executeQuery();

            if(rs.next()) {

                return UserRole.valueOf(
                        rs.getString("role")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return UserRole.USER;
    }



    public static void createUser(
            String username,
            String password,
            UserRole role
    ) {

        try (
                Connection conn = DatabaseManager.connect();

                PreparedStatement ps =
                        conn.prepareStatement(
                                "INSERT INTO users(username,password,role) VALUES(?,?,?)"
                        )
        ) {

            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role.name());

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    public static void deleteUser(String username) {

        try (
                Connection conn = DatabaseManager.connect();

                PreparedStatement ps =
                        conn.prepareStatement(
                                "DELETE FROM users WHERE username=?"
                        )
        ) {

            ps.setString(1, username);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    public static void updateRole(
            String username,
            UserRole role
    ) {

        try (
                Connection conn = DatabaseManager.connect();

                PreparedStatement ps =
                        conn.prepareStatement(
                                "UPDATE users SET role=? WHERE username=?"
                        )
        ) {

            ps.setString(1, role.name());
            ps.setString(2, username);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    public static void resetPassword(
            String username,
            String newPassword
    ) {

        try (
                Connection conn = DatabaseManager.connect();

                PreparedStatement ps =
                        conn.prepareStatement(
                                "UPDATE users SET password=? WHERE username=?"
                        )
        ) {

            ps.setString(1, newPassword);
            ps.setString(2, username);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    public static Map<String, UserRole> getAllUsers() {

        Map<String, UserRole> users =
                new LinkedHashMap<>();

        try (
                Connection conn = DatabaseManager.connect();

                Statement stmt = conn.createStatement()
        ) {

            ResultSet rs =
                    stmt.executeQuery(
                            "SELECT username, role FROM users"
                    );

            while(rs.next()) {

                users.put(
                        rs.getString("username"),
                        UserRole.valueOf(
                                rs.getString("role")
                        )
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }
}
