package modules.management.parking;

import database.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParkingManager {

    public static List<LotInfo> getLots() {
        List<LotInfo> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, type FROM parking_lots")) {
            while (rs.next()) {
                list.add(new LotInfo(rs.getInt("id"), rs.getString("name"), rs.getString("type")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<SpotInfo> getSpots(int lotId) {
        List<SpotInfo> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT id, spot_number, is_occupied, is_available FROM parking_spots WHERE lot_id = ?")) {
            ps.setInt(1, lotId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int spotId = rs.getInt("id");
                String currentUser = getOccupant(spotId);
                list.add(new SpotInfo(spotId, rs.getInt("spot_number"), rs.getInt("is_occupied") == 1, rs.getInt("is_available") == 1, currentUser));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private static String getOccupant(int spotId) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT username FROM parking_occupancy WHERE spot_id = ? AND vacated_at IS NULL")) {
            ps.setInt(1, spotId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("username");
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public static boolean park(int spotId, String username) {

        if (isUserParking(username)) return false;

        try (Connection conn = DatabaseManager.connect()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE parking_spots SET is_occupied = 1 WHERE id = ? AND is_occupied = 0 AND is_available = 1")) {
                    ps.setInt(1, spotId);
                    if (ps.executeUpdate() == 0) return false;
                }
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO parking_occupancy(spot_id, username) VALUES(?, ?)")) {
                    ps.setInt(1, spotId);
                    ps.setString(2, username);
                    ps.executeUpdate();
                }
                logAction(username, "PARK", "Spot ID: " + spotId);
                core.NotificationManager.sendNotification(conn, username, "Vehicle parked at Spot ID: " + spotId, "PARKING");
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean unpark(int spotId, String username) {
        try (Connection conn = DatabaseManager.connect()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE parking_spots SET is_occupied = 0 WHERE id = ?")) {
                    ps.setInt(1, spotId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("UPDATE parking_occupancy SET vacated_at = CURRENT_TIMESTAMP WHERE spot_id = ? AND username = ? AND vacated_at IS NULL")) {
                    ps.setInt(1, spotId);
                    ps.setString(2, username);
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                }
                logAction(username, "UNPARK", "Spot ID: " + spotId);
                core.NotificationManager.sendNotification(conn, username, "Vehicle unparked from Spot ID: " + spotId, "PARKING");
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean adminVacate(int spotId) {
        try (Connection conn = DatabaseManager.connect()) {
            conn.setAutoCommit(false);
            try {
                String occupant = getOccupant(spotId);
                try (PreparedStatement ps = conn.prepareStatement("UPDATE parking_spots SET is_occupied = 0 WHERE id = ?")) {
                    ps.setInt(1, spotId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement("UPDATE parking_occupancy SET vacated_at = CURRENT_TIMESTAMP, username = 'TOWED_' || username WHERE spot_id = ? AND vacated_at IS NULL")) {
                    ps.setInt(1, spotId);
                    ps.executeUpdate();
                }
                logAction("ADMIN", "TOW/VACATE", "Occupant: " + occupant + ", Spot ID: " + spotId);
                if (occupant != null) {
                    core.NotificationManager.sendNotification(conn, occupant, "Your vehicle at Spot ID: " + spotId + " was VACATED/TOWED by Admin.", "PARKING");
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    private static void logAction(String user, String action, String details) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO parking_logs(username, action, details) VALUES(?, ?, ?)")) {
            ps.setString(1, user);
            ps.setString(2, action);
            ps.setString(3, details);
            ps.executeUpdate();
        } catch (Exception e) {}
    }

    public static List<ParkingLog> getLogs() {
        List<ParkingLog> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM parking_logs ORDER BY id DESC")) {
            while (rs.next()) {
                list.add(new ParkingLog(rs.getString("username"), rs.getString("action"), rs.getString("details"), rs.getString("time")));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public static boolean purgeLogs() {
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM parking_logs");
            return true;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static class ParkingLog {
        public String username, action, details, time;
        public ParkingLog(String u, String a, String d, String t) {
            username = u; action = a; details = d; time = t;
        }
    }

    public static boolean toggleAvailability(int spotId, boolean available) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("UPDATE parking_spots SET is_available = ? WHERE id = ?")) {
            ps.setInt(1, available ? 1 : 0);
            ps.setInt(2, spotId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean checkTowed(String username) {
        String towedName = "TOWED_" + username;
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM parking_occupancy WHERE username = ? AND vacated_at IS NOT NULL")) {
            ps.setString(1, towedName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");

                try (PreparedStatement psDel = conn.prepareStatement("UPDATE parking_occupancy SET username = ? WHERE id = ?")) {
                    psDel.setString(1, "WAS_TOWED_" + username);
                    psDel.setInt(2, id);
                    psDel.executeUpdate();
                }
                return true;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    private static boolean isUserParking(String username) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM parking_occupancy WHERE username = ? AND vacated_at IS NULL")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public static class LotInfo {
        public int id;
        public String name, type;
        public LotInfo(int id, String name, String type) {
            this.id = id; this.name = name; this.type = type;
        }
    }

    public static class SpotInfo {
        public int id, number;
        public boolean isOccupied, isAvailable;
        public String occupant;
        public SpotInfo(int id, int number, boolean isOccupied, boolean isAvailable, String occupant) {
            this.id = id; this.number = number; this.isOccupied = isOccupied; 
            this.isAvailable = isAvailable; this.occupant = occupant;
        }
    }
}
