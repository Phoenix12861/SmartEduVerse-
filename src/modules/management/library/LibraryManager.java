package modules.management.library;

import database.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LibraryManager {

    public static boolean deleteBook(int bookId) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM books WHERE id = ?")) {
            ps.setInt(1, bookId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static int getBorrowedRecordId(int bookId, String username) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM library_records WHERE book_id = ? AND username = ? AND status = 'BORROWED'")) {
            ps.setInt(1, bookId);
            ps.setString(2, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public static boolean isBookBorrowedByUser(int bookId, String username) {
        return getBorrowedRecordId(bookId, username) != -1;
    }

    public static List<Book> getAllBooks(String category) {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books";
        if (category != null && !category.equals("All")) {
            sql += " WHERE category = ?";
        }
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (category != null && !category.equals("All")) ps.setString(1, category);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                books.add(new Book(
                    rs.getInt("id"), rs.getString("title"), rs.getString("author"),
                    rs.getString("category"), rs.getInt("quantity"), rs.getInt("available_count"),
                    rs.getDouble("price")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return books;
    }

    public static boolean addBook(String title, String author, String category, int qty, double price) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO books (title, author, category, quantity, available_count, price) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, title);
            ps.setString(2, author);
            ps.setString(3, category);
            ps.setInt(4, qty);
            ps.setInt(5, qty);
            ps.setDouble(6, price);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean updateBookPrice(int bookId, double price) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("UPDATE books SET price = ? WHERE id = ?")) {
            ps.setDouble(1, price);
            ps.setInt(2, bookId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean createBill(String username, String bookTitle, double amount) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO library_bills (username, description, amount, status) VALUES (?, ?, ?, 'UNPAID')")) {
            ps.setString(1, username);
            ps.setString(2, "Purchase/Fine for " + bookTitle);
            ps.setDouble(3, amount);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static List<String[]> getUserBills(String username) {
        List<String[]> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM library_bills WHERE username = ? ORDER BY id DESC")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new String[]{
                    String.valueOf(rs.getInt("id")), rs.getString("description"),
                    String.valueOf(rs.getDouble("amount")), rs.getString("status"),
                    rs.getString("created_at")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static boolean payBill(int billId, String username, String bankPassword) {
        double amount = -1;
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement psGet = conn.prepareStatement("SELECT amount FROM library_bills WHERE id = ? AND status = 'UNPAID'")) {
            psGet.setInt(1, billId);
            ResultSet rs = psGet.executeQuery();
            if (rs.next()) amount = rs.getDouble("amount");
        } catch (SQLException e) { e.printStackTrace(); }

        if (amount == -1) return false;

        // Money transfer happens on its own connection
        if (!modules.finance.banking.BankAccountManager.transferMoney(username, "SYSTEM_SCHOOL", amount, bankPassword)) {
            return false;
        }

        // Update library record on a fresh connection
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement psUpd = conn.prepareStatement("UPDATE library_bills SET status = 'PAID' WHERE id = ?")) {
            psUpd.setInt(1, billId);
            return psUpd.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean borrowBook(int bookId, String username) {
        if (isUserBanned(username)) return false;
        
        try (Connection conn = DatabaseManager.connect()) {
            conn.setAutoCommit(false);
            try {
                // Check availability
                PreparedStatement psCheck = conn.prepareStatement("SELECT available_count FROM books WHERE id = ? AND available_count > 0");
                psCheck.setInt(1, bookId);
                if (!psCheck.executeQuery().next()) return false;

                // Create record with 7-day due date
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.add(java.util.Calendar.DAY_OF_MONTH, 7);
                Timestamp dueDate = new Timestamp(cal.getTimeInMillis());

                PreparedStatement psRec = conn.prepareStatement("INSERT INTO library_records (book_id, username, due_date) VALUES (?, ?, ?)");
                psRec.setInt(1, bookId);
                psRec.setString(2, username);
                psRec.setTimestamp(3, dueDate);
                psRec.executeUpdate();

                // Update count
                PreparedStatement psUpd = conn.prepareStatement("UPDATE books SET available_count = available_count - 1 WHERE id = ?");
                psUpd.setInt(1, bookId);
                psUpd.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException ex) { conn.rollback(); ex.printStackTrace(); }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static void checkOverdueAndFine() {
        try (Connection conn = DatabaseManager.connect()) {
            // Calculate fine: 2 rupees per day overdue
            // Logic: fine = (current_date - due_date) in days * 2
            String sql = "UPDATE library_records SET fine_amount = (strftime('%s','now') - strftime('%s',due_date)) / 86400 * 2 " +
                         "WHERE status = 'BORROWED' AND strftime('%s','now') > strftime('%s',due_date)";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
            }
            
            // Ban users who have overdue books and haven't paid fines
            String banSql = "UPDATE users SET is_library_banned = 1 WHERE username IN (SELECT username FROM library_records WHERE status = 'BORROWED' AND strftime('%s','now') > strftime('%s',due_date))";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(banSql);
            }
            
            // Notify admins about overdue books (one notification per overdue book if not already notified? for simplicity, just once)
            // But we'll just send a general alert to notifications for each record
            ResultSet rs = conn.createStatement().executeQuery("SELECT username, id FROM library_records WHERE status = 'BORROWED' AND strftime('%s','now') > strftime('%s',due_date)");
            while (rs.next()) {
                String user = rs.getString("username");
                core.NotificationManager.sendNotification(conn, "owner", "User " + user + " has an overdue book (Record ID: " + rs.getInt("id") + ").", "LIBRARY");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static boolean isUserBanned(String username) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT is_library_banned FROM users WHERE username = ?")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) == 1;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static void banUser(String username, boolean isBanned) {
        try (Connection conn = DatabaseManager.connect()) {
            banUser(conn, username, isBanned);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void banUser(Connection conn, String username, boolean isBanned) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE users SET is_library_banned = ? WHERE username = ?")) {
            ps.setInt(1, isBanned ? 1 : 0);
            ps.setString(2, username);
            ps.executeUpdate();
        }
    }

    public static boolean payFineAndReturn(int recordId, String username, String bankPassword) {
        // Handle money transfer outside the library transaction to avoid SQLite lock contention
        try (Connection conn = DatabaseManager.connect()) {
            PreparedStatement psGet = conn.prepareStatement("SELECT fine_amount, book_id FROM library_records WHERE id = ? AND status = 'BORROWED'");
            psGet.setInt(1, recordId);
            ResultSet rs = psGet.executeQuery();
            if (!rs.next()) return false;
            
            double fine = rs.getDouble("fine_amount");
            int bookId = rs.getInt("book_id");

            if (fine > 0) {
                if (!modules.finance.banking.BankAccountManager.transferMoney(username, "SYSTEM_SCHOOL", fine, bankPassword)) {
                    return false;
                }
            }

            conn.setAutoCommit(false);
            try {
                // Update record
                PreparedStatement psRec = conn.prepareStatement("UPDATE library_records SET return_date = CURRENT_TIMESTAMP, status = 'RETURNED', fine_amount = ? WHERE id = ?");
                psRec.setDouble(1, fine);
                psRec.setInt(2, recordId);
                psRec.executeUpdate();

                // Update book count
                PreparedStatement psUpd = conn.prepareStatement("UPDATE books SET available_count = available_count + 1 WHERE id = ?");
                psUpd.setInt(1, bookId);
                psUpd.executeUpdate();

                // Check if user has any other overdue books before unbanning
                PreparedStatement psCheck = conn.prepareStatement("SELECT COUNT(*) FROM library_records WHERE username = ? AND status = 'BORROWED' AND strftime('%s','now') > strftime('%s',due_date)");
                psCheck.setString(1, username);
                ResultSet rsCheck = psCheck.executeQuery();
                if (rsCheck.next() && rsCheck.getInt(1) == 0) {
                    banUser(conn, username, false);
                }

                conn.commit();
                return true;
            } catch (SQLException ex) { conn.rollback(); ex.printStackTrace(); }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean updateBook(int bookId, String title, String author, String category, int qty, double price) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("UPDATE books SET title = ?, author = ?, category = ?, quantity = ?, available_count = ?, price = ? WHERE id = ?")) {
            ps.setString(1, title);
            ps.setString(2, author);
            ps.setString(3, category);
            ps.setInt(4, qty);
            ps.setInt(5, qty); // Reset available count to match new quantity for simplicity
            ps.setDouble(6, price);
            ps.setInt(7, bookId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static List<String[]> getAllRecords() {
        List<String[]> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT r.id, r.username, b.title, r.borrow_date, r.due_date, r.status, r.fine_amount FROM library_records r JOIN books b ON r.book_id = b.id ORDER BY r.borrow_date DESC")) {
            while (rs.next()) {
                list.add(new String[]{
                    String.valueOf(rs.getInt("id")), rs.getString("username"), rs.getString("title"),
                    rs.getString("borrow_date"), rs.getString("due_date"), rs.getString("status"),
                    String.valueOf(rs.getDouble("fine_amount"))
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static List<String[]> getBorrowedBooks(String username) {
        List<String[]> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT r.id, b.title, r.borrow_date, r.due_date, r.status, r.fine_amount FROM library_records r JOIN books b ON r.book_id = b.id WHERE r.username = ? ORDER BY r.borrow_date DESC")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new String[]{
                    String.valueOf(rs.getInt("id")), rs.getString("title"), rs.getString("borrow_date"),
                    rs.getString("due_date"), rs.getString("status"), String.valueOf(rs.getDouble("fine_amount"))
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
}
