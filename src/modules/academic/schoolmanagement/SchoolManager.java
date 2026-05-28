package modules.academic.schoolmanagement;

import database.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SchoolManager {


    public static List<String> getSchools() {
        List<String> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM schools")) {
            while (rs.next()) list.add(rs.getString("name"));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static int getSchoolId(String name) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM schools WHERE name = ?")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }


    public static class StaffInfo {
        public String username, role, lastSalaryPaid;
        public int schoolId;
        public double salary;
        public StaffInfo(String u, int sid, String r, double s, String lsp) {
            username = u; schoolId = sid; role = r; salary = s; lastSalaryPaid = lsp;
        }
    }

    public static StaffInfo getStaffInfo(String username) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM school_staff WHERE username = ?")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new StaffInfo(rs.getString("username"), rs.getInt("school_id"), rs.getString("role"), rs.getDouble("salary"), rs.getString("last_salary_paid"));
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public static boolean appointStaff(int schoolId, String username, String role, double salary) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO school_staff(school_id, username, role, salary) VALUES(?, ?, ?, ?)")) {
            ps.setInt(1, schoolId);
            ps.setString(2, username);
            ps.setString(3, role);
            ps.setDouble(4, salary);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }


    public static class StudentInfo {
        public String username, section, status;
        public int id, schoolId, classNumber, repeatClass;
        public double totalFees, feesPaid;
        public StudentInfo() {}
        public StudentInfo(int id, int sid, String u, int c, String s, String st, double tf, double fp, int rc) {
            this.id = id; this.schoolId = sid; this.username = u; this.classNumber = c; this.section = s; this.status = st; this.totalFees = tf; this.feesPaid = fp; this.repeatClass = rc;
        }
    }

    public static StudentInfo getStudentInfo(String username) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM school_students WHERE username = ?")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new StudentInfo(rs.getInt("id"), rs.getInt("school_id"), rs.getString("username"), rs.getInt("class_number"), rs.getString("section"), rs.getString("status"), rs.getDouble("total_fees"), rs.getDouble("fees_paid"), rs.getInt("repeat_class"));
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public static List<StudentInfo> getStudentsByClass(int schoolId, int classNum, String section) {
        List<StudentInfo> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM school_students WHERE school_id = ? AND class_number = ? AND section = ?")) {
            ps.setInt(1, schoolId);
            ps.setInt(2, classNum);
            ps.setString(3, section);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new StudentInfo(rs.getInt("id"), rs.getInt("school_id"), rs.getString("username"), rs.getInt("class_number"), rs.getString("section"), rs.getString("status"), rs.getDouble("total_fees"), rs.getDouble("fees_paid"), rs.getInt("repeat_class")));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static boolean enrollStudent(int schoolId, String username, int classNum, String section, double totalFees) {

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement psCheck = conn.prepareStatement("SELECT role FROM users WHERE username = ?")) {
            psCheck.setString(1, username);
            ResultSet rs = psCheck.executeQuery();
            if (!rs.next() || !rs.getString("role").equals("STUDENT")) {
                return false;
            }
        } catch (SQLException e) { e.printStackTrace(); return false; }

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO school_students(school_id, username, class_number, section, total_fees) VALUES(?, ?, ?, ?, ?)")) {
            ps.setInt(1, schoolId);
            ps.setString(2, username);
            ps.setInt(3, classNum);
            ps.setString(4, section);
            ps.setDouble(5, totalFees);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }


    public static class TimetableEntry {
        public int id, period;
        public String day, subject, teacher;
        public TimetableEntry(int id, String d, int p, String s, String t) {
            this.id = id; this.day = d; this.period = p; this.subject = s; this.teacher = t;
        }
    }

    public static List<TimetableEntry> getTimetable(int schoolId, int classNum, String section) {
        List<TimetableEntry> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM school_timetables WHERE school_id = ? AND class_number = ? AND section = ? ORDER BY day_of_week, period_number")) {
            ps.setInt(1, schoolId);
            ps.setInt(2, classNum);
            ps.setString(3, section);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new TimetableEntry(rs.getInt("id"), rs.getString("day_of_week"), rs.getInt("period_number"), rs.getString("subject"), rs.getString("teacher_username")));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static boolean setTimetableEntry(int schoolId, int classNum, String section, String day, int period, String subject, String teacher) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO school_timetables(school_id, class_number, section, day_of_week, period_number, subject, teacher_username) VALUES(?, ?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, schoolId);
            ps.setInt(2, classNum);
            ps.setString(3, section);
            ps.setString(4, day);
            ps.setInt(5, period);
            ps.setString(6, subject);
            ps.setString(7, teacher);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }


    public static class ExamEntry {
        public String subject, date, time;
        public ExamEntry(String s, String d, String t) { subject = s; date = d; time = t; }
    }

    public static List<ExamEntry> getExamTimetable(int schoolId, int classNum, String section) {
        List<ExamEntry> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM school_exam_timetables WHERE school_id = ? AND class_number = ? AND section = ? ORDER BY exam_date")) {
            ps.setInt(1, schoolId);
            ps.setInt(2, classNum);
            ps.setString(3, section);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new ExamEntry(rs.getString("subject"), rs.getString("exam_date"), rs.getString("exam_time")));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static boolean setExamEntry(int schoolId, int classNum, String section, String subject, String date, String time) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO school_exam_timetables(school_id, class_number, section, subject, exam_date, exam_time) VALUES(?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, schoolId);
            ps.setInt(2, classNum);
            ps.setString(3, section);
            ps.setString(4, subject);
            ps.setString(5, date);
            ps.setString(6, time);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }


    public static boolean assignHomework(int schoolId, int classNum, String section, String subject, String content) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO school_homework(school_id, class_number, section, subject, content, assigned_date) VALUES(?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, schoolId);
            ps.setInt(2, classNum);
            ps.setString(3, section);
            ps.setString(4, subject);
            ps.setString(5, content);
            ps.setString(6, new java.util.Date().toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }


    public static boolean updateStudentStatus(String username, String status) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("UPDATE school_students SET status = ? WHERE username = ?")) {
            ps.setString(1, status);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean setStudentRepeat(String username, boolean repeat) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("UPDATE school_students SET repeat_class = ? WHERE username = ?")) {
            ps.setInt(1, repeat ? 1 : 0);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean payFees(String username, double amount) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("UPDATE school_students SET fees_paid = fees_paid + ? WHERE username = ?")) {
            ps.setDouble(1, amount);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean requestTransfer(String username, int fromSid, int toSid) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO school_transfer_requests(student_username, from_school_id, to_school_id) VALUES(?, ?, ?)")) {
            ps.setString(1, username);
            ps.setInt(2, fromSid);
            ps.setInt(3, toSid);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }


    public static boolean applyToSchool(String username, int schoolId) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO school_applications(username, school_id) VALUES(?, ?)")) {
            ps.setString(1, username);
            ps.setInt(2, schoolId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static List<StudentInfo> getPendingApplications(int schoolId) {
        List<StudentInfo> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM school_applications WHERE school_id = ? AND status = 'PENDING'")) {
            ps.setInt(1, schoolId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                StudentInfo s = new StudentInfo();
                s.username = rs.getString("username");
                s.id = rs.getInt("id");
                list.add(s);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static boolean approveApplication(int appId, int schoolId, String username, int classNum, String section, double fees) {
        try (Connection conn = DatabaseManager.connect()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement("UPDATE school_applications SET status = 'APPROVED' WHERE id = ?");
                 PreparedStatement ps2 = conn.prepareStatement("INSERT INTO school_students(school_id, username, class_number, section, total_fees) VALUES(?, ?, ?, ?, ?)")) {
                ps1.setInt(1, appId);
                ps1.executeUpdate();

                ps2.setInt(1, schoolId);
                ps2.setString(2, username);
                ps2.setInt(3, classNum);
                ps2.setString(4, section);
                ps2.setDouble(5, fees);
                ps2.executeUpdate();

                conn.commit();
                core.NotificationManager.sendNotification(username, "Your application to " + getSchoolName(schoolId) + " has been APPROVED!", "SCHOOL");
                return true;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static String getSchoolName(int id) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT name FROM schools WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("name");
        } catch (SQLException e) { e.printStackTrace(); }
        return "Unknown School";
    }

    public static String getPrincipalUsername(int schoolId) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT username FROM school_staff WHERE school_id = ? AND role = 'PRINCIPAL'")) {
            ps.setInt(1, schoolId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("username");
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }


    public static boolean approveTransfer(int requestId) {
        try (Connection conn = DatabaseManager.connect()) {
            conn.setAutoCommit(false);
            try {
                int toSid = -1; String username = null;
                try (PreparedStatement ps = conn.prepareStatement("SELECT student_username, to_school_id FROM school_transfer_requests WHERE id = ?")) {
                    ps.setInt(1, requestId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        username = rs.getString("student_username");
                        toSid = rs.getInt("to_school_id");
                    }
                }
                if (username != null) {
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE school_students SET school_id = ?, status = 'ACTIVE' WHERE username = ?")) {
                        ps.setInt(1, toSid);
                        ps.setString(2, username);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE school_transfer_requests SET status = 'APPROVED' WHERE id = ?")) {
                        ps.setInt(1, requestId);
                        ps.executeUpdate();
                    }
                    conn.commit();
                    return true;
                }
            } catch (SQLException ex) { conn.rollback(); ex.printStackTrace(); }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }


    public static boolean autoPaySalaries() {
        try (Connection conn = DatabaseManager.connect()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM school_staff WHERE salary > 0")) {
                while (rs.next()) {
                    String username = rs.getString("username");
                    double salary = rs.getDouble("salary");
                    if (modules.finance.banking.BankAccountManager.transferMoney("SYSTEM_SCHOOL", username, salary, "system_pass")) {
                        try (PreparedStatement ps = conn.prepareStatement("UPDATE school_staff SET last_salary_paid = ? WHERE username = ?")) {
                            ps.setString(1, new java.util.Date().toString());
                            ps.setString(2, username);
                            ps.executeUpdate();
                        }
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException ex) { conn.rollback(); ex.printStackTrace(); }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }
}
