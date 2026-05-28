package modules.academic.attendance;

import database.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttendanceManager {

    public static boolean takeAttendance(String studentUsername, String date, String status) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO school_attendance(student_username, date, status) VALUES(?, ?, ?)")) {
            ps.setString(1, studentUsername);
            ps.setString(2, date);
            ps.setString(3, status);
            boolean success = ps.executeUpdate() > 0;
            if (success && status.equals("ABSENT")) {
                core.NotificationManager.sendNotification(studentUsername, "You were marked ABSENT for " + date, "ATTENDANCE");
            }
            return success;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static List<String[]> getDailyAttendance(String date) {
        List<String[]> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT student_username, status FROM school_attendance WHERE date = ?")) {
            ps.setString(1, date);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new String[]{rs.getString(1), rs.getString(2)});
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static double getAttendancePercentage(String username) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement psTotal = conn.prepareStatement("SELECT COUNT(*) FROM school_attendance WHERE student_username = ?");
             PreparedStatement psPresent = conn.prepareStatement("SELECT COUNT(*) FROM school_attendance WHERE student_username = ? AND status = 'PRESENT'")) {
            psTotal.setString(1, username);
            psPresent.setString(1, username);
            ResultSet rsTotal = psTotal.executeQuery();
            ResultSet rsPresent = psPresent.executeQuery();
            if (rsTotal.next() && rsPresent.next()) {
                int total = rsTotal.getInt(1);
                int present = rsPresent.getInt(1);
                if (total == 0) return 0;
                return (double) present / total * 100.0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
}
