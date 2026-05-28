package modules.management.couriertracking;

import database.DatabaseManager;
import java.sql.*;
import java.util.*;

public class CourierManager {

    public static final Map<String, double[]> CITY_COORDS = new LinkedHashMap<>();
    static {
        CITY_COORDS.put("Mumbai", new double[]{19.0760, 72.8777});
        CITY_COORDS.put("Delhi", new double[]{28.6139, 77.2090});
        CITY_COORDS.put("Bangalore", new double[]{12.9716, 77.5946});
        CITY_COORDS.put("Hyderabad", new double[]{17.3850, 78.4867});
        CITY_COORDS.put("Ahmedabad", new double[]{23.0225, 72.5714});
        CITY_COORDS.put("Chennai", new double[]{13.0827, 80.2707});
        CITY_COORDS.put("Kolkata", new double[]{22.5726, 88.3639});
        CITY_COORDS.put("Surat", new double[]{21.1702, 72.8311});
        CITY_COORDS.put("Pune", new double[]{18.5204, 73.8567});
        CITY_COORDS.put("Jaipur", new double[]{26.9124, 75.7873});
        CITY_COORDS.put("Lucknow", new double[]{26.8467, 80.9462});
        CITY_COORDS.put("Kanpur", new double[]{26.4499, 80.3319});
        CITY_COORDS.put("Nagpur", new double[]{21.1458, 79.0882});
        CITY_COORDS.put("Indore", new double[]{22.7196, 75.8577});
        CITY_COORDS.put("Thane", new double[]{19.2183, 72.9781});
        CITY_COORDS.put("Bhopal", new double[]{23.2599, 77.4126});
        CITY_COORDS.put("Visakhapatnam", new double[]{17.6868, 83.2185});
        CITY_COORDS.put("Pimpri-Chinchwad", new double[]{18.6298, 73.7997});
        CITY_COORDS.put("Patna", new double[]{25.5941, 85.1376});
        CITY_COORDS.put("Vadodara", new double[]{22.3072, 73.1812});
    }

    public static class Courier {
        public int id;
        public String sender, receiver, fromAddr, toAddr, location, status, paymentStatus, route;
        public double distance, amount;
        public int estimatedTime;

        public Courier(int id, String s, String r, String fa, String ta, String l, double d, double a, String st, String ps, String route, int et) {
            this.id = id; this.sender = s; this.receiver = r; this.fromAddr = fa; this.toAddr = ta;
            this.location = l; this.distance = d; this.amount = a; this.status = st; this.paymentStatus = ps;
            this.route = route; this.estimatedTime = et;
        }
    }

    public static double calculateDistance(String city1, String city2) {
        double[] c1 = CITY_COORDS.get(city1);
        double[] c2 = CITY_COORDS.get(city2);
        if (c1 == null || c2 == null) return 500.0;
        double d = Math.sqrt(Math.pow(c1[0] - c2[0], 2) + Math.pow(c1[1] - c2[1], 2)) * 111.0;
        return Math.round(d * 100.0) / 100.0;
    }

    public static boolean createCourier(String sender, String receiver, String from, String to, double dist) {
        double amount = dist * 10.0;
        String initialRoute = from + " -> " + to;
        int estimatedTime = (int) (dist / 50.0 * 60.0); // 50km/h in minutes
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO couriers(sender_username, receiver_username, from_address, to_address, current_location, distance_km, amount, route, estimated_time) " +
                     "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.setString(3, from);
            ps.setString(4, to);
            ps.setString(5, from); // Initial location
            ps.setDouble(6, dist);
            ps.setDouble(7, amount);
            ps.setString(8, initialRoute);
            ps.setInt(9, estimatedTime);
            boolean success = ps.executeUpdate() > 0;
            if (success) {
                core.NotificationManager.sendNotification(sender, "Your courier to " + receiver + " has been booked. Charge: ₹" + amount, "COURIER");
                core.NotificationManager.sendNotification(receiver, "A courier from " + sender + " is on its way to you!", "COURIER");
            }
            return success;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static List<Courier> getMyCouriers(String user) {
        List<Courier> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM couriers WHERE sender_username = ? OR receiver_username = ? ORDER BY id DESC")) {
            ps.setString(1, user);
            ps.setString(2, user);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Courier(rs.getInt("id"), rs.getString("sender_username"), rs.getString("receiver_username"),
                        rs.getString("from_address"), rs.getString("to_address"), rs.getString("current_location"),
                        rs.getDouble("distance_km"), rs.getDouble("amount"), rs.getString("status"), rs.getString("payment_status"),
                        rs.getString("route"), rs.getInt("estimated_time")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static List<Courier> getAllCouriers() {
        List<Courier> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM couriers ORDER BY id DESC")) {
            while (rs.next()) {
                list.add(new Courier(rs.getInt("id"), rs.getString("sender_username"), rs.getString("receiver_username"),
                        rs.getString("from_address"), rs.getString("to_address"), rs.getString("current_location"),
                        rs.getDouble("distance_km"), rs.getDouble("amount"), rs.getString("status"), rs.getString("payment_status"),
                        rs.getString("route"), rs.getInt("estimated_time")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static boolean updateLocation(int id, String loc) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("UPDATE couriers SET current_location = ? WHERE id = ?")) {
            ps.setString(1, loc);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean updateStatus(int id, String status) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps1 = conn.prepareStatement("SELECT sender_username, receiver_username FROM couriers WHERE id = ?");
             PreparedStatement ps2 = conn.prepareStatement("UPDATE couriers SET status = ? WHERE id = ?")) {
            
            ps1.setInt(1, id);
            ResultSet rs = ps1.executeQuery();
            if (rs.next()) {
                String sender = rs.getString(1);
                String receiver = rs.getString(2);
                ps2.setString(1, status);
                ps2.setInt(2, id);
                if (ps2.executeUpdate() > 0) {
                    core.NotificationManager.sendNotification(sender, "Courier status updated to: " + status, "COURIER");
                    core.NotificationManager.sendNotification(receiver, "Incoming courier status: " + status, "COURIER");
                    return true;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean updateRoute(int id, String route) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("UPDATE couriers SET route = ? WHERE id = ?")) {
            ps.setString(1, route);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean updateEstimatedTime(int id, int time) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("UPDATE couriers SET estimated_time = ? WHERE id = ?")) {
            ps.setInt(1, time);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean payCourier(int id) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps1 = conn.prepareStatement("SELECT sender_username, amount FROM couriers WHERE id = ?");
             PreparedStatement ps2 = conn.prepareStatement("UPDATE couriers SET payment_status = 'PAID' WHERE id = ?")) {
            
            ps1.setInt(1, id);
            ResultSet rs = ps1.executeQuery();
            if (rs.next()) {
                String sender = rs.getString(1);
                double amt = rs.getDouble(2);
                ps2.setInt(1, id);
                if (ps2.executeUpdate() > 0) {
                    core.NotificationManager.sendNotification(sender, "Courier payment of ₹" + amt + " successful.", "COURIER");
                    return true;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean refundCourier(int id) {
        String sender = null;
        double amount = 0;
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT sender_username, amount FROM couriers WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                sender = rs.getString("sender_username");
                amount = rs.getDouble("amount");
            }
        } catch (SQLException e) { e.printStackTrace(); }

        if (sender != null && amount > 0) {
            // Transfer back from SYSTEM_COURIER to user with REFUND type
            modules.finance.banking.BankAccountManager.refundMoney("SYSTEM_COURIER", sender, amount);
        }

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("UPDATE couriers SET payment_status = 'REFUNDED' WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean hasUnpaidDeliveredCourier(String username) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM couriers WHERE sender_username = ? AND status = 'DELIVERED' AND payment_status = 'PENDING'")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean purgeLogs() {
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM couriers WHERE status = 'DELIVERED' OR status = 'CANCELLED'");
            return true;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }
}
