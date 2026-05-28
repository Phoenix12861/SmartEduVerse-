package modules.management.hospital;

import database.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

public class HospitalManager {

    public static List<String> getHospitals() {
        List<String> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM hospitals")) {
            while (rs.next()) {
                list.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static int getHospitalId(String name) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM hospitals WHERE name = ?")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static List<RoomInfo> getRooms(int hospitalId) {
        List<RoomInfo> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT id, room_number, type, is_occupied FROM hospital_rooms WHERE hospital_id = ?")) {
            ps.setInt(1, hospitalId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new RoomInfo(rs.getInt("id"), rs.getInt("room_number"), rs.getString("type"), rs.getInt("is_occupied") == 1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static boolean bookRoom(int roomId, String username, String startDate, int days) {
        try (Connection conn = DatabaseManager.connect()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO hospital_bookings(room_id, username, start_date, days, created_at) VALUES(?, ?, ?, ?, ?)")) {
                ps.setInt(1, roomId);
                ps.setString(2, username);
                ps.setString(3, startDate);
                ps.setInt(4, days);
                ps.setString(5, java.time.LocalDateTime.now().toString());
                ps.executeUpdate();

                try (PreparedStatement psUpdate = conn.prepareStatement("UPDATE hospital_rooms SET is_occupied = 1 WHERE id = ?")) {
                    psUpdate.setInt(1, roomId);
                    psUpdate.executeUpdate();
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
public static class BillItem {
    public String name;
    public double price;
    public BillItem(String name, double price) {
        this.name = name; this.price = price;
    }
}

public static boolean createBillByRoom(int hospitalId, int roomNumber, double roomCharge, List<BillItem> items) {
    String username = getUsernameByRoom(hospitalId, roomNumber);
    if (username == null) return false;

    try (Connection conn = DatabaseManager.connect()) {
        conn.setAutoCommit(false);
        double total = roomCharge + items.stream().mapToDouble(i -> i.price).sum();
        try (PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO hospital_bills(username, hospital_id, room_number, room_charge, medical_charge, total_amount, status) VALUES(?, ?, ?, ?, ?, ?, 'PENDING')", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setInt(2, hospitalId);
            ps.setInt(3, roomNumber);
            ps.setDouble(4, roomCharge);
            ps.setDouble(5, items.stream().mapToDouble(i -> i.price).sum());
            ps.setDouble(6, total);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int billId = rs.getInt(1);
                for (BillItem item : items) {
                    try (PreparedStatement psItem = conn.prepareStatement("INSERT INTO hospital_bill_items(bill_id, item_name, price) VALUES(?, ?, ?)")) {
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

public static List<BillItem> getBillItems(int billId) {
    List<BillItem> list = new ArrayList<>();
    try (Connection conn = DatabaseManager.connect();
         PreparedStatement ps = conn.prepareStatement("SELECT item_name, price FROM hospital_bill_items WHERE bill_id = ?")) {
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

    private static String getUsernameByRoom(int hospitalId, int roomNumber) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT b.username FROM hospital_bookings b " +
                     "JOIN hospital_rooms r ON b.room_id = r.id " +
                     "WHERE r.hospital_id = ? AND r.room_number = ? AND b.status = 'ACTIVE'")) {
            ps.setInt(1, hospitalId);
            ps.setInt(2, roomNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("username");
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public static class BillDetails {
        public double roomCharge, medicalCharge, totalAmount;
        public BillDetails(double r, double m, double t) { roomCharge = r; medicalCharge = m; totalAmount = t; }
    }

    public static BillDetails getBillDetails(int billId) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT room_charge, medical_charge, total_amount FROM hospital_bills WHERE id = ?")) {
            ps.setInt(1, billId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new BillDetails(rs.getDouble("room_charge"), rs.getDouble("medical_charge"), rs.getDouble("total_amount"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public static List<BillInfo> getPendingBills(String username) {
        List<BillInfo> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT b.id, h.name as hospital_name, b.total_amount " +
                     "FROM hospital_bills b JOIN hospitals h ON b.hospital_id = h.id " +
                     "WHERE b.username = ? AND b.status = 'PENDING'")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new BillInfo(rs.getInt("id"), rs.getString("hospital_name"), rs.getDouble("total_amount")));
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
                int hospitalId = -1, roomNumber = -1;
                try (PreparedStatement psBill = conn.prepareStatement("SELECT hospital_id, room_number FROM hospital_bills WHERE id = ?")) {
                    psBill.setInt(1, billId);
                    ResultSet rs = psBill.executeQuery();
                    if (rs.next()) {
                        hospitalId = rs.getInt("hospital_id");
                        roomNumber = rs.getInt("room_number");
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement("UPDATE hospital_bills SET status = 'PAID' WHERE id = ? AND username = ?")) {
                    ps.setInt(1, billId);
                    ps.setString(2, username);
                    ps.executeUpdate();
                }

                if (hospitalId != -1 && roomNumber != -1) {
                    try (PreparedStatement psRoom = conn.prepareStatement("UPDATE hospital_rooms SET is_occupied = 0 WHERE hospital_id = ? AND room_number = ?")) {
                        psRoom.setInt(1, hospitalId);
                        psRoom.setInt(2, roomNumber);
                        psRoom.executeUpdate();
                    }
                    try (PreparedStatement psBook = conn.prepareStatement(
                            "UPDATE hospital_bookings SET status = 'COMPLETED' " +
                            "WHERE room_id = (SELECT id FROM hospital_rooms WHERE hospital_id=? AND room_number=?) AND username=? AND status='ACTIVE'")) {
                        psBook.setInt(1, hospitalId);
                        psBook.setInt(2, roomNumber);
                        psBook.setString(3, username);
                        psBook.executeUpdate();
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

    public static class RoomInfo {
        public int id, number;
        public String type;
        public boolean isOccupied;
        public RoomInfo(int id, int number, String type, boolean isOccupied) {
            this.id = id; this.number = number; this.type = type; this.isOccupied = isOccupied;
        }
    }

    public static class BillInfo {
        public int id;
        public String hospitalName;
        public double amount;
        public BillInfo(int id, String hospitalName, double amount) {
            this.id = id; this.hospitalName = hospitalName; this.amount = amount;
        }
    }

    public static List<BookingLog> getLogs() {
        List<BookingLog> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT b.username, h.name as hospital_name, r.room_number, b.start_date, b.days, b.status, COALESCE(b.created_at, 'N/A') as created_at, h.id as hospital_id " +
                     "FROM hospital_bookings b " +
                     "JOIN hospital_rooms r ON b.room_id = r.id " +
                     "JOIN hospitals h ON r.hospital_id = h.id " +
                     "ORDER BY b.id DESC")) {
            while (rs.next()) {
                list.add(new BookingLog(rs.getString("username"), rs.getString("hospital_name"), rs.getInt("room_number"),
                        rs.getString("start_date"), rs.getInt("days"), rs.getString("status"), rs.getString("created_at"), rs.getInt("hospital_id")));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public static boolean purgeLogs() {
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM hospital_bookings WHERE status != 'ACTIVE'");
            return true;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static class BookingLog {
        public String username, hospitalName, startDate, status, createdAt;
        public int roomNumber, days, hospitalId;
        public BookingLog(String u, String h, int rn, String sd, int d, String s, String c, int hi) {
            username = u; hospitalName = h; roomNumber = rn; startDate = sd; days = d; status = s; createdAt = c; hospitalId = hi;
        }
    }
}
