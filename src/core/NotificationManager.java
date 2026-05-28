package core;

import database.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationManager {

    public static class Notification {
        public int id;
        public String message, type, time;
        public boolean isRead;

        public Notification(int id, String message, String type, boolean isRead, String time) {
            this.id = id;
            this.message = message;
            this.type = type;
            this.isRead = isRead;
            this.time = time;
        }
    }

    public static void sendNotification(String username, String message, String type) {
        try (Connection conn = database.DatabaseManager.connect()) {
            sendNotification(conn, username, message, type);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void sendNotification(Connection conn, String username, String message, String type) {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO notifications(username, message, type) VALUES(?, ?, ?)")) {
            ps.setString(1, username);
            ps.setString(2, message);
            ps.setString(3, type);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<Notification> getNotifications(String username) {
        List<Notification> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM notifications WHERE username = ? ORDER BY created_at DESC")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Notification(
                        rs.getInt("id"),
                        rs.getString("message"),
                        rs.getString("type"),
                        rs.getInt("is_read") == 1,
                        rs.getString("created_at")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static int getUnreadCount(String username) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM notifications WHERE username = ? AND is_read = 0")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void markAllAsRead(String username) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("UPDATE notifications SET is_read = 1 WHERE username = ?")) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
