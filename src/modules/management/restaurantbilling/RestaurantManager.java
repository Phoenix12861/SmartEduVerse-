package modules.management.restaurantbilling;

import database.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class RestaurantManager {

    public static List<String> getRestaurants() {
        List<String> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM restaurants")) {
            while (rs.next()) {
                list.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static int getRestaurantId(String name) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM restaurants WHERE name = ?")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static List<TableInfo> getTables(int restaurantId) {
        List<TableInfo> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT id, table_number FROM restaurant_tables WHERE restaurant_id = ?")) {
            ps.setInt(1, restaurantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int tableId = rs.getInt("id");
                int tableNum = rs.getInt("table_number");
                
                // Get current status based on reservations
                TableStatus status = getTableStatus(tableId);
                list.add(new TableInfo(tableId, tableNum, status.isOccupied, status.isReserved, status.reservedUntil, status.reservationId, status.currentUser, status.forcePay));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private static TableStatus getTableStatus(int tableId) {
        String today = LocalDate.now().toString();
        LocalTime now = LocalTime.now();
        
        boolean isOccupied = false;
        boolean isReserved = false;
        boolean forcePay = false;
        String reservedUntil = null;
        int reservationId = -1;
        String currentUser = null;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, time, username FROM restaurant_reservations WHERE table_id = ? AND date = ? AND status = 'ACTIVE' ORDER BY time ASC")) {
            ps.setInt(1, tableId);
            ps.setString(2, today);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                reservations.add(new Reservation(rs.getInt("id"), LocalTime.parse(rs.getString("time")), rs.getString("username")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Reservation currentRes = null;
        Reservation upcomingRes = null;

        for (Reservation r : reservations) {
            // Occupied if within 15 mins before or after reservation, or up to 4 hours after (assuming stay)
            if (now.isAfter(r.time.minusMinutes(15)) && now.isBefore(r.time.plusMinutes(240))) {
                isOccupied = true;
                currentRes = r;
                currentUser = r.username;
                reservationId = r.id;
                reservedUntil = r.time.toString();
                
                // Force pay if current stay exceeds 1h 30m
                if (now.isAfter(r.time.plusMinutes(90))) {
                    forcePay = true;
                }
                break;
            }
        }

        for (Reservation r : reservations) {
            if (r.time.isAfter(now)) {
                if (upcomingRes == null || r.time.isBefore(upcomingRes.time)) {
                    upcomingRes = r;
                }
            }
        }

        if (upcomingRes != null && !isOccupied && upcomingRes.time.isAfter(now.plusMinutes(30))) {
            isReserved = true;
            reservedUntil = upcomingRes.time.toString();
            reservationId = upcomingRes.id;
        }

        return new TableStatus(isOccupied, isReserved, reservedUntil, reservationId, currentUser, forcePay);
    }

    private static class Reservation {
        int id; LocalTime time; String username;
        Reservation(int i, LocalTime t, String u) { id = i; time = t; username = u; }
    }

    public static boolean makeReservation(int tableId, String username, String date, String time) {
        // Limit 3 tables per day
        if (getUserReservationCount(username, date) >= 3) return false;

        // Check if table is available at that time
        if (!isTableAvailable(tableId, date, time)) return false;

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO restaurant_reservations(table_id, username, date, time) VALUES(?, ?, ?, ?)")) {
            ps.setInt(1, tableId);
            ps.setString(2, username);
            ps.setString(3, date);
            ps.setString(4, time);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static int getUserReservationCount(String username, String date) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM restaurant_reservations WHERE username = ? AND date = ? AND status = 'ACTIVE'")) {
            ps.setString(1, username);
            ps.setString(2, date);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    private static boolean isTableAvailable(int tableId, String date, String time) {
        LocalTime newTime = LocalTime.parse(time);
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT time FROM restaurant_reservations WHERE table_id = ? AND date = ? AND status = 'ACTIVE'")) {
            ps.setInt(1, tableId);
            ps.setString(2, date);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                LocalTime resTime = LocalTime.parse(rs.getString("time"));
                // 2 hours buffer before or after the reservation
                if (newTime.isAfter(resTime.minusMinutes(120)) && newTime.isBefore(resTime.plusMinutes(120))) {
                    return false;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return true;
    }

    public static boolean cancelReservation(int reservationId, String username) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("UPDATE restaurant_reservations SET status = 'CANCELLED' WHERE id = ? AND username = ?")) {
            ps.setInt(1, reservationId);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean purgeLogs() {
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM restaurant_reservations WHERE status != 'ACTIVE'");
            return true;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean createBillByTable(int restaurantId, int tableNumber, List<BillItem> items) {
        // Find the user who has a current reservation for this table
        String username = getUsernameByTable(restaurantId, tableNumber);
        if (username == null) return false;

        int tableId = -1;
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM restaurant_tables WHERE restaurant_id = ? AND table_number = ?")) {
            ps.setInt(1, restaurantId);
            ps.setInt(2, tableNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) tableId = rs.getInt(1);
        } catch (SQLException e) {}

        if (tableId != -1) {
            double extra = calculateExtraCharge(tableId, username);
            if (extra > 0) {
                items.add(new BillItem("Extra Stay Charge (Time Exceeded)", extra));
            }
        }

        try (Connection conn = DatabaseManager.connect()) {
            conn.setAutoCommit(false);
            double total = items.stream().mapToDouble(i -> i.price).sum();
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO restaurant_bills(username, restaurant_id, table_number, total_amount) VALUES(?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setInt(2, restaurantId);
                ps.setInt(3, tableNumber);
                ps.setDouble(4, total);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    int billId = rs.getInt(1);
                    for (BillItem item : items) {
                        try (PreparedStatement psItem = conn.prepareStatement("INSERT INTO restaurant_bill_items(bill_id, item_name, price) VALUES(?, ?, ?)")) {
                            psItem.setInt(1, billId);
                            psItem.setString(2, item.name);
                            psItem.setDouble(3, item.price);
                            psItem.executeUpdate();
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

    public static double calculateExtraCharge(int tableId, String username) {
        String today = LocalDate.now().toString();
        LocalTime now = LocalTime.now();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT time FROM restaurant_reservations WHERE table_id = ? AND username = ? AND date = ? AND status = 'ACTIVE'")) {
            ps.setInt(1, tableId);
            ps.setString(2, username);
            ps.setString(3, today);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                LocalTime resTime = LocalTime.parse(rs.getString("time"));
                long minutes = java.time.Duration.between(resTime, now).toMinutes();
                if (minutes > 90) {
                    return ((minutes - 90) / 30) * 20.0;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0.0;
    }

    private static String getUsernameByTable(int restaurantId, int tableNumber) {
        String today = LocalDate.now().toString();
        LocalTime now = LocalTime.now();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT r.username, r.time FROM restaurant_reservations r " +
                     "JOIN restaurant_tables t ON r.table_id = t.id " +
                     "WHERE t.restaurant_id = ? AND t.table_number = ? AND r.date = ? AND r.status = 'ACTIVE'")) {
            ps.setInt(1, restaurantId);
            ps.setInt(2, tableNumber);
            ps.setString(3, today);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                LocalTime resTime = LocalTime.parse(rs.getString("time"));
                if (now.isAfter(resTime.minusMinutes(15)) && now.isBefore(resTime.plusMinutes(240))) {
                    return rs.getString("username");
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public static List<BillItem> getBillItems(int billId) {
        List<BillItem> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT item_name, price FROM restaurant_bill_items WHERE bill_id = ?")) {
            ps.setInt(1, billId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new BillItem(rs.getString("item_name"), rs.getDouble("price")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<BillInfo> getPendingBills(String username) {
        List<BillInfo> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT b.id, r.name as restaurant_name, b.total_amount " +
                     "FROM restaurant_bills b JOIN restaurants r ON b.restaurant_id = r.id " +
                     "WHERE b.username = ? AND b.status = 'PENDING'")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new BillInfo(rs.getInt("id"), rs.getString("restaurant_name"), rs.getDouble("total_amount")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static boolean payBill(int billId, String username) {
        try (Connection conn = DatabaseManager.connect()) {
            conn.setAutoCommit(false);
            try {
                int restaurantId = -1, tableNumber = -1;
                try (PreparedStatement psBill = conn.prepareStatement("SELECT restaurant_id, table_number FROM restaurant_bills WHERE id = ?")) {
                    psBill.setInt(1, billId);
                    ResultSet rs = psBill.executeQuery();
                    if (rs.next()) {
                        restaurantId = rs.getInt("restaurant_id");
                        tableNumber = rs.getInt("table_number");
                    }
                }

                // Mark bill as paid
                try (PreparedStatement ps = conn.prepareStatement("UPDATE restaurant_bills SET status = 'PAID' WHERE id = ? AND username = ?")) {
                    ps.setInt(1, billId);
                    ps.setString(2, username);
                    ps.executeUpdate();
                }

                // Vacate table
                if (restaurantId != -1 && tableNumber != -1) {
                    try (PreparedStatement psRes = conn.prepareStatement(
                            "UPDATE restaurant_reservations SET status = 'COMPLETED' " +
                            "WHERE table_id = (SELECT id FROM restaurant_tables WHERE restaurant_id=? AND table_number=?) AND username=? AND status='ACTIVE'")) {
                        psRes.setInt(1, restaurantId);
                        psRes.setInt(2, tableNumber);
                        psRes.setString(3, username);
                        psRes.executeUpdate();
                    }
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

    public static List<ReservationLog> getOwnerLogs() {
        List<ReservationLog> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT r.username, res.name as restaurant_name, t.table_number, r.date, r.time, r.status, COALESCE(r.created_at, 'N/A') as created_at " +
                     "FROM restaurant_reservations r " +
                     "JOIN restaurant_tables t ON r.table_id = t.id " +
                     "JOIN restaurants res ON t.restaurant_id = res.id " +
                     "ORDER BY r.id DESC")) {
            while (rs.next()) {
                list.add(new ReservationLog(rs.getString("username"), rs.getString("restaurant_name"), rs.getInt("table_number"),
                        rs.getString("date"), rs.getString("time"), rs.getString("status"), rs.getString("created_at")));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public static class TableInfo {
        public int id, number;
        public boolean isOccupied, isReserved, forcePay;
        public String reservedUntil, currentUser;
        public int reservationId;
        public TableInfo(int id, int number, boolean isOccupied, boolean isReserved, String reservedUntil, int reservationId, String currentUser, boolean forcePay) {
            this.id = id; this.number = number; this.isOccupied = isOccupied; 
            this.isReserved = isReserved; this.reservedUntil = reservedUntil; 
            this.reservationId = reservationId; this.currentUser = currentUser;
            this.forcePay = forcePay;
        }
    }

    private static class TableStatus {
        boolean isOccupied, isReserved, forcePay;
        String reservedUntil, currentUser;
        int reservationId;
        TableStatus(boolean o, boolean r, String ru, int id, String u, boolean f) { 
            isOccupied = o; isReserved = r; reservedUntil = ru; reservationId = id; currentUser = u; forcePay = f;
        }
    }

    public static class BillItem {
        public String name;
        public double price;
        public BillItem(String name, double price) {
            this.name = name; this.price = price;
        }
    }

    public static class BillInfo {
        public int id;
        public String restaurantName;
        public double amount;
        public BillInfo(int id, String restaurantName, double amount) {
            this.id = id; this.restaurantName = restaurantName; this.amount = amount;
        }
    }

    public static class ReservationLog {
        public String username, restaurantName, date, time, status, createdAt;
        public int tableNumber;
        public ReservationLog(String u, String r, int tn, String d, String t, String s, String c) {
            username = u; restaurantName = r; tableNumber = tn; date = d; time = t; status = s; createdAt = c;
        }
    }
}
