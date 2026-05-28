package modules.academic.resultanalyzer;

import database.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResultAnalyzer {

    public static class MarkEntry {
        public String subject;
        public double marks, totalMarks;
        public MarkEntry(String s, double m, double tm) { subject = s; marks = m; totalMarks = tm; }
    }

    public static List<MarkEntry> getResults(String username) {
        List<MarkEntry> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM school_results WHERE student_username = ?")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new MarkEntry(rs.getString("subject"), rs.getDouble("marks"), rs.getDouble("total_marks")));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static boolean setMarks(String username, String subject, double marks, double totalMarks) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO school_results(student_username, subject, marks, total_marks) VALUES(?, ?, ?, ?)")) {
            ps.setString(1, username);
            ps.setString(2, subject);
            ps.setDouble(3, marks);
            ps.setDouble(4, totalMarks);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean deleteMarks(String username, String subject) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM school_results WHERE student_username = ? AND subject = ?")) {
            ps.setString(1, username);
            ps.setString(2, subject);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }
}
