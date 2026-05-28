package modules.finance.atm;

import database.DatabaseManager;
import modules.finance.banking.BankAccountManager;
import modules.finance.banking.LogManager;
import modules.finance.banking.TransactionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ATMManager {

    public static boolean withdraw(String username, String password, double amount) {
        if (amount <= 0) return false;

        if (!BankAccountManager.verifyBankPassword(username, password)) {
            LogManager.addLog(username, "ATM_WITHDRAW_FAIL", username, "Invalid password. Amount: " + amount);
            return false;
        }

        String status = BankAccountManager.getStatus(username);
        if (!"APPROVED".equals(status)) {
            LogManager.addLog(username, "ATM_WITHDRAW_FAIL", username, "Account not approved or frozen. Status: " + status);
            return false;
        }

        try (Connection conn = DatabaseManager.connect()) {
            conn.setAutoCommit(false);
            try {
                if (BankAccountManager.updateBalance(conn, username, -amount)) {
                    TransactionManager.addTransaction(conn, username, "ATM_WITHDRAW", amount, "ATM Withdrawal");
                    LogManager.addLog(conn, username, "ATM_WITHDRAW_SUCCESS", username, "Amount: " + amount);
                    

                    logToATMAudit(conn, username, "WITHDRAW", amount);
                    
                    core.NotificationManager.sendNotification(conn, username, "ATM Withdrawal of ₹" + amount + " successful.", "ATM");
                    
                    conn.commit();
                    return true;
                } else {
                    LogManager.addLog(conn, username, "ATM_WITHDRAW_FAIL", username, "Insufficient balance. Amount: " + amount);
                    conn.rollback();
                    return false;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void logToATMAudit(Connection conn, String username, String action, double amount) throws SQLException {
        String sql = "INSERT INTO atm_logs (username, action, amount) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, action);
            ps.setDouble(3, amount);
            ps.executeUpdate();
        }
    }
}
