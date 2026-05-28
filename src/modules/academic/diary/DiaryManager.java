package modules.academic.diary;

import database.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DiaryManager {

    public static boolean addEntry(String username, String title, String content) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO diary (username, title, content) VALUES (?,?,?)")) {
            ps.setString(1, username);
            ps.setString(2, title);
            ps.setString(3, content);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<Object[]> getEntries(String username) {
        List<Object[]> entries = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT id, title, is_locked, created_at FROM diary WHERE username = ? ORDER BY created_at DESC")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                entries.add(new Object[]{
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getInt("is_locked") == 1 ? "LOCKED" : "OPEN",
                    rs.getString("created_at")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entries;
    }

    public static String getEntryContent(int id) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT content FROM diary WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("content");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean updateEntry(int id, String title, String content) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("UPDATE diary SET title = ?, content = ? WHERE id = ?")) {
            ps.setString(1, title);
            ps.setString(2, content);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteEntry(int id) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM diary WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean setLock(int id, boolean lock) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("UPDATE diary SET is_locked = ? WHERE id = ?")) {
            ps.setInt(1, lock ? 1 : 0);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean hasDiaryPassword(String username) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM diary_passwords WHERE username = ?")) {
            ps.setString(1, username);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean createDiaryPassword(String username, String password) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO diary_passwords (username, password) VALUES (?,?)")) {
            ps.setString(1, username);
            ps.setString(2, password);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean verifyDiaryPassword(String username, String password) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT password FROM diary_passwords WHERE username = ?")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("password").equals(password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
