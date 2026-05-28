package modules.finance.electricitybill;

import database.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ElectricityManager {

    public static class Bill {
        public int id;
        public String username;
        public double units, amount, fine, totalAmount;
        public String dueDate, status, createdBy;

        public Bill(int id, String u, double un, double a, double f, double ta, String dd, String s, String cb) {
            this.id = id; this.username = u; this.units = un; this.amount = a; this.fine = f;
            this.totalAmount = ta; this.dueDate = dd; this.status = s; this.createdBy = cb;
        }
    }

    public static boolean createBill(String user, double units, String dueDate, String admin) {
        double rate = 8.5; // Example rate per unit
        double amount = units * rate;

        // Limit to 3 bills logic
        List<Bill> currentBills = getBills(user);
        if (currentBills.size() >= 3) {
            // Find a paid bill to delete (oldest first)
            for (int i = currentBills.size() - 1; i >= 0; i--) {
                if ("PAID".equals(currentBills.get(i).status)) {
                    deleteBill(currentBills.get(i).id);
                    break; 
                }
            }
        }

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO electricity_bills(username, units, amount, total_amount, due_date, created_by) VALUES(?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, user);
            ps.setDouble(2, units);
            ps.setDouble(3, amount);
            ps.setDouble(4, amount);
            ps.setString(5, dueDate);
            ps.setString(6, admin);
            boolean success = ps.executeUpdate() > 0;
            if (success) {
                core.NotificationManager.sendNotification(user, "New Electricity Bill generated: ₹" + amount + " (Due: " + dueDate + ")", "ELECTRICITY");
            }
            return success;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean deleteBill(int id) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM electricity_bills WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static List<Bill> getBills(String user) {
        List<Bill> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM electricity_bills WHERE username = ? ORDER BY id DESC")) {
            ps.setString(1, user);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Bill(rs.getInt("id"), rs.getString("username"), rs.getDouble("units"),
                        rs.getDouble("amount"), rs.getDouble("fine"), rs.getDouble("total_amount"),
                        rs.getString("due_date"), rs.getString("status"), rs.getString("created_by")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static List<Bill> getAllBills() {
        List<Bill> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM electricity_bills ORDER BY id DESC")) {
            while (rs.next()) {
                list.add(new Bill(rs.getInt("id"), rs.getString("username"), rs.getDouble("units"),
                        rs.getDouble("amount"), rs.getDouble("fine"), rs.getDouble("total_amount"),
                        rs.getString("due_date"), rs.getString("status"), rs.getString("created_by")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static boolean payBill(int id) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps1 = conn.prepareStatement("SELECT username, total_amount FROM electricity_bills WHERE id = ?");
             PreparedStatement ps2 = conn.prepareStatement("UPDATE electricity_bills SET status = 'PAID' WHERE id = ?")) {
            
            ps1.setInt(1, id);
            ResultSet rs = ps1.executeQuery();
            if (rs.next()) {
                String user = rs.getString(1);
                double amt = rs.getDouble(2);
                ps2.setInt(1, id);
                if (ps2.executeUpdate() > 0) {
                    core.NotificationManager.sendNotification(user, "Electricity Bill of ₹" + amt + " paid successfully.", "ELECTRICITY");
                    return true;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }
}
