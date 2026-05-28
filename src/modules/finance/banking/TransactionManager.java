package modules.finance.banking;

import database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TransactionManager {

    // ================= ADD TRANSACTION =================

    public static void addTransaction(
            String username,
            String type,
            double amount,
            String description
    ) {
        try (Connection conn = DatabaseManager.connect()) {
            addTransaction(conn, username, type, amount, description);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addTransaction(
            Connection conn,
            String username,
            String type,
            double amount,
            String description
    ) throws SQLException {
        try (
                PreparedStatement ps =
                        conn.prepareStatement(
                                """
                                INSERT INTO transactions
                                (
                                    username,
                                    type,
                                    amount,
                                    description
                                )
                                VALUES(?,?,?,?)
                                """
                        )
        ) {
            ps.setString(1, username);
            ps.setString(2, type);
            ps.setDouble(3, amount);
            ps.setString(4, description);
            ps.executeUpdate();
        }
    }

    // ================= PURGE TRANSACTIONS =================

    public static boolean purgeUserTransactions(String username) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM transactions WHERE username = ?")) {
            ps.setString(1, username);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
