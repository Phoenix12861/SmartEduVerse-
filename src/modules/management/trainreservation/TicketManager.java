package modules.management.trainreservation;

import database.DatabaseManager;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TicketManager {

    public static void refreshDailyCapacity() {
        String today = LocalDate.now().toString();
        try (Connection conn = DatabaseManager.connect()) {
            String lastReset = "";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_train_reset FROM settings LIMIT 1")) {
                if (rs.next()) {
                    lastReset = rs.getString("last_train_reset");
                }
            }

            if (lastReset == null || !lastReset.equals(today)) {
                System.out.println("New day detected. Refreshing train capacities...");
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("UPDATE trains SET current_occupancy = 0");

                    stmt.executeUpdate("DELETE FROM train_tickets");
                    
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE settings SET last_train_reset = ?")) {
                        ps.setString(1, today);
                        ps.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<StationInfo> getStations() {
        List<StationInfo> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM stations")) {
            while (rs.next()) {
                list.add(new StationInfo(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Fetched " + list.size() + " stations.");
        return list;
    }

    public static List<TrainInfo> getTrains(int fromId, int toId) {
        List<TrainInfo> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT t.id, t.name, s1.name as from_name, s2.name as to_name, t.departure_time, t.max_capacity, t.current_occupancy, t.price, t.status " +
                     "FROM trains t JOIN stations s1 ON t.from_station_id = s1.id " +
                     "JOIN stations s2 ON t.to_station_id = s2.id " +
                     "WHERE t.from_station_id = ? AND t.to_station_id = ?")) {
            ps.setInt(1, fromId);
            ps.setInt(2, toId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new TrainInfo(rs.getInt("id"), rs.getString("name"), rs.getString("from_name"), rs.getString("to_name"), 
                        rs.getString("departure_time"), rs.getInt("max_capacity"), rs.getInt("current_occupancy"), rs.getDouble("price"), rs.getString("status")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static boolean bookTicket(int trainId, String username, double price) {
        try (Connection conn = DatabaseManager.connect()) {
            conn.setAutoCommit(false);
            try {
                int seat = 0;
                try (PreparedStatement psCheck = conn.prepareStatement("SELECT current_occupancy, max_capacity FROM trains WHERE id = ?")) {
                    psCheck.setInt(1, trainId);
                    ResultSet rs = psCheck.executeQuery();
                    if (rs.next()) {
                        int current = rs.getInt("current_occupancy");
                        int max = rs.getInt("max_capacity");
                        if (current >= max) return false;
                        seat = current + 1;
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO train_tickets(train_id, username, seat_number) VALUES(?, ?, ?)")) {
                    ps.setInt(1, trainId);
                    ps.setString(2, username);
                    ps.setInt(3, seat);
                    ps.executeUpdate();
                }

                try (PreparedStatement psUpdate = conn.prepareStatement("UPDATE trains SET current_occupancy = current_occupancy + 1 WHERE id = ?")) {
                    psUpdate.setInt(1, trainId);
                    psUpdate.executeUpdate();
                }

                core.NotificationManager.sendNotification(conn, username, "Train Ticket booked successfully! Seat: " + seat, "TRAIN");

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean updateTrainStatus(int trainId, String status) {
        try (Connection conn = DatabaseManager.connect()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement("UPDATE trains SET status = ? WHERE id = ?")) {
                ps.setString(1, status);
                ps.setInt(2, trainId);
                ps.executeUpdate();

                if (status.equals("CANCELLED")) {
                    try (PreparedStatement psTickets = conn.prepareStatement("SELECT username, id FROM train_tickets WHERE train_id = ? AND status = 'PAID'")) {
                        psTickets.setInt(1, trainId);
                        ResultSet rs = psTickets.executeQuery();
                        while (rs.next()) {
                            String user = rs.getString("username");
                            int ticketId = rs.getInt("id");
                            try (PreparedStatement psRefund = conn.prepareStatement("UPDATE train_tickets SET status = 'REFUNDED' WHERE id = ?")) {
                                psRefund.setInt(1, ticketId);
                                psRefund.executeUpdate();
                            }
                            core.NotificationManager.sendNotification(conn, user, "Train " + trainId + " has been CANCELLED. Your refund is being processed.", "TRAIN");
                        }
                    }
                } else {

                    try (PreparedStatement psTickets = conn.prepareStatement("SELECT username FROM train_tickets WHERE train_id = ? AND status = 'PAID'")) {
                        psTickets.setInt(1, trainId);
                        ResultSet rs = psTickets.executeQuery();
                        while (rs.next()) {
                            core.NotificationManager.sendNotification(conn, rs.getString("username"), "Train " + trainId + " status updated: " + status, "TRAIN");
                        }
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static class StationInfo {
        public int id;
        public String name;
        public StationInfo(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }

    public static class TrainInfo {
        public int id, maxCapacity, currentOccupancy;
        public String name, from, to, departureTime, status;
        public double price;
        public TrainInfo(int id, String name, String from, String to, String departureTime, int maxCapacity, int currentOccupancy, double price, String status) {
            this.id = id; this.name = name; this.from = from; this.to = to; this.departureTime = departureTime;
            this.maxCapacity = maxCapacity; this.currentOccupancy = currentOccupancy; this.price = price; this.status = status;
        }
    }
}
