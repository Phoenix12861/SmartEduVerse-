package modules.finance.banking;

import database.DatabaseManager;

import java.sql.*;
import java.text.DecimalFormat;

public class BankAccountManager {

    public static final double MAX_DEPOSIT =
            1_000_000_000_000.0;

    // =====================================================
    // ================= CREATE ACCOUNT ====================
    // =====================================================

    public static boolean createAccount(
            String username,
            String password,
            String role
    ) {

        try (
                Connection conn =
                        DatabaseManager.connect()
        ) {

            // =================================================
            // ================= CHECK EXISTING =================
            // =================================================

            PreparedStatement check =
                    conn.prepareStatement(
                            """
                            SELECT id
                            FROM bank_accounts
                            WHERE username=?
                            """
                    );

            check.setString(1, username);

            ResultSet rs =
                    check.executeQuery();

            // Account already exists

            if(rs.next()) {

                return false;
            }

            // =================================================
            // ================= ACCOUNT STATUS =================
            // =================================================

            String status = "PENDING";

            // Owner gets auto approval

            if(role.equals("OWNER")) {

                status = "APPROVED";
            }

            // =================================================
            // ================= INSERT ACCOUNT =================
            // =================================================

            PreparedStatement ps =
                    conn.prepareStatement(
                            """
                            INSERT INTO bank_accounts
                            (
                                username,
                                bank_password,
                                balance,
                                status,
                                approved_by,
                                rfid_uid
                            )
                            VALUES(?,?,?,?,?,?)
                            """
                    );

            ps.setString(1, username);

            ps.setString(2, password);

            ps.setDouble(3, 0);

            ps.setString(4, status);

            // approved_by

            if(role.equals("OWNER")) {

                ps.setString(5, "SYSTEM");

            } else {

                ps.setNull(5, Types.VARCHAR);
            }

            // RFID initially null

            ps.setNull(6, Types.VARCHAR);

            int rows =
                    ps.executeUpdate();

            // =================================================
            // ================= CREATE LOG =====================
            // =================================================

            LogManager.addLog(
                    username,
                    "CREATE_ACCOUNT",
                    username,
                    "Bank account created"
            );

            return rows > 0;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    // =====================================================
    // ================= ACCOUNT EXISTS ====================
    // =====================================================

    public static boolean accountExists(
            String username
    ) {

        try (
                Connection conn =
                        DatabaseManager.connect();

                PreparedStatement ps =
                        conn.prepareStatement(
                                """
                                SELECT id
                                FROM bank_accounts
                                WHERE username=?
                                """
                        )
        ) {

            ps.setString(1, username);

            ResultSet rs =
                    ps.executeQuery();

            return rs.next();

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    // =====================================================
    // ================= GET BALANCE =======================
    // =====================================================

    public static double getBalance(
            String username
    ) {

        try (
                Connection conn =
                        DatabaseManager.connect();

                PreparedStatement ps =
                        conn.prepareStatement(
                                """
                                SELECT balance
                                FROM bank_accounts
                                WHERE username=?
                                """
                        )
        ) {

            ps.setString(1, username);

            ResultSet rs =
                    ps.executeQuery();

            if(rs.next()) {

                return rs.getDouble("balance");
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return 0;
    }

    // =====================================================
    // ================= GET STATUS ========================
    // =====================================================

    public static String getStatus(
            String username
    ) {

        try (
                Connection conn =
                        DatabaseManager.connect();

                PreparedStatement ps =
                        conn.prepareStatement(
                                """
                                SELECT status
                                FROM bank_accounts
                                WHERE username=?
                                """
                        )
        ) {

            ps.setString(1, username);

            ResultSet rs =
                    ps.executeQuery();

            if(rs.next()) {

                return rs.getString("status");
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return "UNKNOWN";
    }

    // =====================================================
    // ================= GET APPROVED BY ===================
    // =====================================================

    public static String getApprovedBy(
            String username
    ) {

        try (
                Connection conn =
                        DatabaseManager.connect();

                PreparedStatement ps =
                        conn.prepareStatement(
                                """
                                SELECT approved_by
                                FROM bank_accounts
                                WHERE username=?
                                """
                        )
        ) {

            ps.setString(1, username);

            ResultSet rs =
                    ps.executeQuery();

            if(rs.next()) {

                return rs.getString("approved_by");
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return null;
    }

    // =====================================================
    // ================= SET STATUS ========================
    // =====================================================

    public static void setStatus(
            String username,
            String status,
            String actor
    ) {

        String query = """
            UPDATE bank_accounts
            SET status=?,
                approved_by=?
            WHERE username=?
            """;

        if (status.equals("TERMINATED")) {
            query = """
                UPDATE bank_accounts
                SET status=?,
                    terminated_by=?,
                    terminated_at=CURRENT_TIMESTAMP
                WHERE username=?
                """;
        }

        try (
                Connection conn =
                        DatabaseManager.connect();

                PreparedStatement ps =
                        conn.prepareStatement(query)
        ) {

            ps.setString(1, status);

            ps.setString(2, actor);

            ps.setString(3, username);

            ps.executeUpdate();

            LogManager.addLog(
                    actor,
                    "SET_STATUS",
                    username,
                    "Changed status to " + status
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public static String getTerminatedBy(
            String username
    ) {

        try (
                Connection conn =
                        DatabaseManager.connect();

                PreparedStatement ps =
                        conn.prepareStatement(
                                """
                                SELECT terminated_by
                                FROM bank_accounts
                                WHERE username=?
                                """
                        )
        ) {

            ps.setString(1, username);

            ResultSet rs =
                    ps.executeQuery();

            if(rs.next()) {

                return rs.getString("terminated_by");
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return null;
    }

    // =====================================================
    // ================= GET TERMINATION INFO ==============
    // =====================================================

    public static String[] getTerminationInfo(String username) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT terminated_by, terminated_at FROM bank_accounts WHERE username=?"
             )) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new String[]{rs.getString("terminated_by"), rs.getString("terminated_at")};
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // =====================================================
    // ================= TRANSFER MONEY ====================
    // =====================================================

    public static boolean transferMoney(
            String fromUser,
            String toUser,
            double amount,
            String password
    ) {
        if (amount <= 0 || fromUser.equals(toUser)) return false;

        // Check if accounts are frozen
        if (getStatus(fromUser).equals("FROZEN") || getStatus(fromUser).equals("FROZEN_APPEAL")) return false;
        if (getStatus(toUser).equals("FROZEN") || getStatus(toUser).equals("FROZEN_APPEAL")) return false;

        try (Connection conn = DatabaseManager.connect()) {
            conn.setAutoCommit(false);

            // Verify Password
            if (!verifyBankPassword(fromUser, password)) return false;

            // Check Balance
            double balance = getBalance(fromUser);
            if (balance < amount) return false;

            // Check Target Exists
            if (!accountExists(toUser)) return false;

            // Perform Transfer
            try (PreparedStatement withdraw = conn.prepareStatement(
                    "UPDATE bank_accounts SET balance = balance - ? WHERE username = ?");
                 PreparedStatement deposit = conn.prepareStatement(
                    "UPDATE bank_accounts SET balance = balance + ? WHERE username = ?")) {

                withdraw.setDouble(1, amount);
                withdraw.setString(2, fromUser);
                withdraw.executeUpdate();

                deposit.setDouble(1, amount);
                deposit.setString(2, toUser);
                deposit.executeUpdate();

                // Log Transactions
                TransactionManager.addTransaction(conn, fromUser, "TRANSFER_OUT", amount, "To: " + toUser);
                TransactionManager.addTransaction(conn, toUser, "TRANSFER_IN", amount, "From: " + fromUser);

                core.NotificationManager.sendNotification(conn, fromUser, "You transferred ₹" + formatAmount(amount) + " to " + toUser, "BANK");
                core.NotificationManager.sendNotification(conn, toUser, "You received ₹" + formatAmount(amount) + " from " + fromUser, "BANK");

                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean refundMoney(String fromUser, String toUser, double amount) {
        if (amount <= 0) return false;
        try (Connection conn = DatabaseManager.connect()) {
            conn.setAutoCommit(false);
            try {
                if (!updateBalance(conn, fromUser, -amount)) {
                    conn.rollback();
                    return false;
                }
                if (!updateBalance(conn, toUser, amount)) {
                    conn.rollback();
                    return false;
                }
                TransactionManager.addTransaction(conn, toUser, "REFUND", amount, "From: " + fromUser);
                LogManager.addLog(conn, fromUser, "REFUND", toUser, "Amount: " + amount);
                
                core.NotificationManager.sendNotification(conn, toUser, "You received a refund of ₹" + formatAmount(amount) + " from " + fromUser, "BANK");
                
                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // =====================================================
    // ================= VERIFY PASSWORD ===================
    // =====================================================

    public static boolean verifyBankPassword(
            String username,
            String password
    ) {

        try (
                Connection conn =
                        DatabaseManager.connect();

                PreparedStatement ps =
                        conn.prepareStatement(
                                """
                                SELECT bank_password
                                FROM bank_accounts
                                WHERE username=?
                                """
                        )
        ) {

            ps.setString(1, username);

            ResultSet rs =
                    ps.executeQuery();

            if(rs.next()) {

                return rs.getString(
                        "bank_password"
                ).equals(password);
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return false;
    }

    // =====================================================
    // ================= CHANGE PASSWORD ===================
    // =====================================================

    public static boolean changePassword(
            String username,
            String newPassword,
            String actor
    ) {

        try (
                Connection conn =
                        DatabaseManager.connect();

                PreparedStatement ps =
                        conn.prepareStatement(
                                """
                                UPDATE bank_accounts
                                SET bank_password=?
                                WHERE username=?
                                """
                        )
        ) {

            ps.setString(1, newPassword);

            ps.setString(2, username);

            int rows =
                    ps.executeUpdate();

            LogManager.addLog(
                    actor,
                    "CHANGE_PASSWORD",
                    username,
                    "Bank password updated"
            );

            return rows > 0;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    public static boolean updateBalance(
            Connection conn,
            String username,
            double amount
    ) throws SQLException {

        // Check current balance if withdrawing
        if (amount < 0) {
            double current = 0;
            try (PreparedStatement check = conn.prepareStatement("SELECT balance FROM bank_accounts WHERE username = ?")) {
                check.setString(1, username);
                ResultSet rs = check.executeQuery();
                if (rs.next()) {
                    current = rs.getDouble("balance");
                }
            }
            if (current + amount < 0) return false;
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE bank_accounts SET balance = balance + ? WHERE username = ?")) {
            ps.setDouble(1, amount);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        }
    }

    // =====================================================
    // ================= UPDATE BALANCE (STATIC) ===========
    // =====================================================

    public static void updateBalance(
            String username,
            double amount
    ) {

        try (
                Connection conn =
                        DatabaseManager.connect();

                PreparedStatement ps =
                        conn.prepareStatement(
                                """
                                UPDATE bank_accounts
                                SET balance=balance+?
                                WHERE username=?
                                """
                        )
        ) {

            ps.setDouble(1, amount);

            ps.setString(2, username);

            ps.executeUpdate();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // =====================================================
    // ================= LAST ACTIVITY =====================
    // =====================================================

    public static String getLastActivity(
            String username
    ) {

        try (
                Connection conn =
                        DatabaseManager.connect();

                PreparedStatement ps =
                        conn.prepareStatement(
                                """
                                SELECT amount, type, description
                                FROM transactions
                                WHERE username=?
                                AND type IN ('PURCHASE', 'TRANSFER_OUT', 'TRANSFER_IN', 'ATM_WITHDRAW')
                                ORDER BY id DESC
                                LIMIT 1
                                """
                        )
        ) {

            ps.setString(1, username);

            ResultSet rs =
                    ps.executeQuery();

            if(rs.next()) {
                double amount = rs.getDouble("amount");
                String type = rs.getString("type");
                String desc = rs.getString("description");
                
                if (type.equals("TRANSFER_OUT") || type.equals("TRANSFER_IN")) {
                    return "₹ " + formatAmount(amount) + " (" + desc + ")";
                }
                if (type.equals("ATM_WITHDRAW")) {
                    return "₹ " + formatAmount(amount) + " (WITHDRAWN)";
                }
                return "₹ " + formatAmount(amount);
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return "₹ 0.00";
    }

    // =====================================================
    // ================= FORMAT AMOUNT =====================
    // =====================================================

    public static String formatAmount(
            double amount
    ) {

        DecimalFormat df =
                new DecimalFormat(
                        "#,##0.00"
                );

        return df.format(amount);
    }
}
