package modules.finance.banking;

import core.SessionManager;
import core.UserRole;
import database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DepositManager {

    public static boolean requestDeposit(String username, double amount) {
        if (amount <= 0 || amount > BankAccountManager.MAX_DEPOSIT) return false;


        String accountStatus = BankAccountManager.getStatus(username);
        if (accountStatus.equals("FROZEN") || accountStatus.equals("FROZEN_APPEAL")) return false;

        String status = "PENDING";
        String approvedBy = null;


        if (SessionManager.getCurrentRole() == UserRole.OWNER) {
            status = "APPROVED";
            approvedBy = "SYSTEM";
        }

        try (Connection conn = DatabaseManager.connect()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO deposit_requests (username, amount, status, approved_by) VALUES (?,?,?,?)")) {

                ps.setString(1, username);
                ps.setDouble(2, amount);
                ps.setString(3, status);
                ps.setString(4, approvedBy);
                ps.executeUpdate();

                if (status.equals("APPROVED")) {
                    updateBalance(conn, username, amount);
                    TransactionManager.addTransaction(conn, username, "DEPOSIT", amount, "Instant deposit (Owner)");
                    LogManager.addLog(conn, "SYSTEM", "DEPOSIT_AUTO_APPROVED", username, "Amount: " + amount);
                } else {
                    TransactionManager.addTransaction(conn, username, "DEPOSIT_REQUEST", amount, "Deposit request submitted");
                    LogManager.addLog(conn, username, "DEPOSIT_REQUESTED", "SYSTEM", "Amount: " + amount);
                }
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

    public static double getPendingDeposits(String username) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT SUM(amount) as total FROM deposit_requests WHERE username = ? AND status = 'PENDING'")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("total");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public static boolean approveDeposit(int requestId, String adminUser) {
        try (Connection conn = DatabaseManager.connect()) {
            conn.setAutoCommit(false);
            
            String requester = null;
            double amount = 0;
            String currentStatus = null;

            try (PreparedStatement ps = conn.prepareStatement("SELECT username, amount, status FROM deposit_requests WHERE id = ?")) {
                ps.setInt(1, requestId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    requester = rs.getString("username");
                    amount = rs.getDouble("amount");
                    currentStatus = rs.getString("status");
                }
            }

            if (requester == null || !"PENDING".equals(currentStatus)) {
                conn.rollback();
                return false;
            }


            String requesterStatus = BankAccountManager.getStatus(requester);
            if (requesterStatus.equals("FROZEN") || requesterStatus.equals("FROZEN_APPEAL")) {
                conn.rollback();
                return false;
            }


            String adminRole = getRole(adminUser);
            if (!adminRole.equalsIgnoreCase("ADMIN") && !adminRole.equalsIgnoreCase("OWNER")) {
                conn.rollback();
                return false;
            }


            String requesterRole = getRole(requester);
            if (requesterRole.equalsIgnoreCase("ADMIN") && adminUser.equalsIgnoreCase(requester) && !adminRole.equalsIgnoreCase("OWNER")) {
                conn.rollback();
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE deposit_requests SET status = 'APPROVED', approved_by = ? WHERE id = ?")) {
                ps.setString(1, adminUser);
                ps.setInt(2, requestId);
                if (ps.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
            }

            updateBalance(conn, requester, amount);
            TransactionManager.addTransaction(conn, requester, "DEPOSIT", amount, "Deposit approved by " + adminUser);
            LogManager.addLog(conn, adminUser, "DEPOSIT_APPROVED", requester, "Amount: " + amount);
            
            conn.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean rejectDeposit(int requestId, String adminUser) {
        try (Connection conn = DatabaseManager.connect()) {
            conn.setAutoCommit(false);


            String adminRole = getRole(adminUser);
            if (!adminRole.equalsIgnoreCase("ADMIN") && !adminRole.equalsIgnoreCase("OWNER")) {
                conn.rollback();
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement("UPDATE deposit_requests SET status = 'REJECTED', approved_by = ? WHERE id = ? AND status = 'PENDING'")) {
                ps.setString(1, adminUser);
                ps.setInt(2, requestId);
                if (ps.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
            }
            
            LogManager.addLog(conn, adminUser, "DEPOSIT_REJECTED", "ID:" + requestId, "Request rejected");
            conn.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }





    public static void updateBalance(
            Connection conn,
            String username,
            double amount
    ) throws SQLException {

        PreparedStatement ps =
                conn.prepareStatement(
                        """
                        UPDATE bank_accounts
                        SET balance = balance + ?
                        WHERE username = ?
                        """
                );

        ps.setDouble(1, amount);

        ps.setString(2, username);

        ps.executeUpdate();
    }





    public static String getRole(
            String username
    ) {

        try (
                Connection conn =
                        DatabaseManager.connect();

                PreparedStatement ps =
                        conn.prepareStatement(
                                """
                                SELECT role
                                FROM users
                                WHERE username=?
                                """
                        )
        ) {

            ps.setString(1, username);

            ResultSet rs =
                    ps.executeQuery();

            if(rs.next()) {

                return rs.getString("role");
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return "USER";
    }
}
