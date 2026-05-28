package modules.finance.banking;

import core.SessionManager;
import core.UserRole;
import database.DatabaseManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AdminBankPanel extends JPanel {

    private final BankingPanel parent;
    private DefaultTableModel adminModel;
    private JTable adminTable;

    private JButton freezeBtn;

    public AdminBankPanel(BankingPanel parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));
        setBorder(new EmptyBorder(30, 40, 30, 40));

        // ================= HEADER =================
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Admin Panel");
        title.setFont(new Font("SansSerif", Font.BOLD, 36));
        header.add(title, BorderLayout.WEST);

        JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        headerButtons.setOpaque(false);

        JButton refreshBtn = createHeaderButton("REFRESH DATA", new Color(70, 70, 70));
        refreshBtn.addActionListener(e -> refresh());
        
        JButton backBtn = createHeaderButton("← BACK TO DASHBOARD", Color.BLACK);
        backBtn.addActionListener(e -> parent.showDashboard());
        
        headerButtons.add(refreshBtn);
        headerButtons.add(backBtn);
        header.add(headerButtons, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ================= MAIN CONTENT =================
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setOpaque(false);
        mainContent.setBorder(new EmptyBorder(30, 0, 0, 0));

        // 1. Unified Table (Edge-to-Edge)
        adminModel = new DefaultTableModel(new String[]{"Username", "Balance", "Status", "Role"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        adminTable = createStyledTable(adminModel);
        adminTable.getSelectionModel().addListSelectionListener(e -> updateButtonStates());
        JScrollPane scroll = new JScrollPane(adminTable);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(new Dimension(900, 300)); // Larger
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        mainContent.add(scroll);
        
        // 2. Centered Actions (No Label, No Box)
        JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        btnWrapper.setOpaque(false);
        btnWrapper.setBorder(new EmptyBorder(30, 0, 0, 0));

        btnWrapper.add(createActionBtn("APPROVE", new Color(0, 150, 136), e -> handleUnifiedAction("APPROVE")));
        btnWrapper.add(createActionBtn("REJECT", new Color(211, 47, 47), e -> handleUnifiedAction("REJECT")));
        
        freezeBtn = createActionBtn("FREEZE", new Color(255, 152, 0), e -> {
            int row = adminTable.getSelectedRow();
            if (row == -1) return;
            String currentStatus = adminModel.getValueAt(row, 2).toString();
            if (currentStatus.startsWith("FROZEN")) {
                handleUnifiedAction("APPROVED");
            } else {
                handleUnifiedAction("FROZEN");
            }
        });
        btnWrapper.add(freezeBtn);
        
        if (SessionManager.getCurrentRole() == UserRole.OWNER) {
            btnWrapper.add(createActionBtn("TERMINATE", Color.BLACK, e -> handleUnifiedAction("TERMINATED")));
        }
        
        btnWrapper.add(createActionBtn("ADJUST BALANCE", Color.DARK_GRAY, e -> handleAdjustBalance()));
        btnWrapper.add(createActionBtn("RESET PASS", Color.GRAY, e -> handleResetPassword()));

        mainContent.add(btnWrapper);

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.add(mainContent, BorderLayout.NORTH);
        add(new JScrollPane(centerWrapper), BorderLayout.CENTER);

        refresh();
    }

    private void updateButtonStates() {
        int row = adminTable.getSelectedRow();
        if (row == -1) return;
        String status = adminModel.getValueAt(row, 2).toString();
        if (status.startsWith("FROZEN") || status.contains("REQ appeal")) {
            freezeBtn.setText("UNFREEZE");
            freezeBtn.setBackground(new Color(0, 150, 136));
        } else {
            freezeBtn.setText("FREEZE");
            freezeBtn.setBackground(new Color(255, 152, 0));
        }
    }

    private void handleUnifiedAction(String action) {
        int row = adminTable.getSelectedRow();
        if (row == -1) return;
        
        String targetUser = adminModel.getValueAt(row, 0).toString();
        String currentStatus = adminModel.getValueAt(row, 2).toString();
        String currentUser = SessionManager.getCurrentUser();

        if (currentStatus.contains("Pending Dep.")) {
            if (action.equals("APPROVE") || action.equals("REJECT")) {
                handleDepositAction(targetUser, action.equals("APPROVE"));
                return;
            }
        }

        String targetRole = adminModel.getValueAt(row, 3).toString();
        
        if (currentStatus.equals("TERMINATED") || currentStatus.contains("REQ appeal")) {
            String terminator = BankAccountManager.getTerminatedBy(targetUser);
            if (terminator == null && currentStatus.contains("REQ appeal")) {
                // If it was frozen appeal, it might not have terminated_by set yet if it was self-frozen?
                // But user said "appeal page same like in termination page" - we should check who froze it.
                terminator = BankAccountManager.getApprovedBy(targetUser);
            }
            if (SessionManager.getCurrentRole() != UserRole.OWNER && terminator != null && !currentUser.equals(terminator)) {
                JOptionPane.showMessageDialog(this, "Only the terminator (" + terminator + ") or Owner can manage this account.");
                return;
            }
        }

        if (targetUser.equals(currentUser) && !action.equals("FROZEN") && !action.equals("APPROVED") && !action.equals("FROZEN_APPEAL")) {
            JOptionPane.showMessageDialog(this, "Denied on own account.");
            return;
        }
        if (targetRole.equalsIgnoreCase("ADMIN") && SessionManager.getCurrentRole() != UserRole.OWNER) {
            JOptionPane.showMessageDialog(this, "Only Owner can manage Admins.");
            return;
        }
        if (action.equals("TERMINATED") && SessionManager.getCurrentRole() != UserRole.OWNER) {
            JOptionPane.showMessageDialog(this, "Only Owner can terminate accounts.");
            return;
        }
        if (action.equals("APPROVE")) action = "APPROVED";
        BankAccountManager.setStatus(targetUser, action, currentUser);
        refresh();
    }

    private void handleDepositAction(String username, boolean approve) {
        int id = -1;
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM deposit_requests WHERE username = ? AND status = 'PENDING' ORDER BY requested_at ASC LIMIT 1")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                id = rs.getInt("id");
            }
        } catch (Exception e) { e.printStackTrace(); }

        if (id != -1) {
            if (approve) {
                DepositManager.approveDeposit(id, SessionManager.getCurrentUser());
            } else {
                DepositManager.rejectDeposit(id, SessionManager.getCurrentUser());
            }
            refresh();
        }
    }

    private void handleAdjustBalance() {
        int row = adminTable.getSelectedRow();
        if (row == -1) return;
        String targetUser = adminModel.getValueAt(row, 0).toString();
        String targetRole = adminModel.getValueAt(row, 3).toString();
        String currentUser = SessionManager.getCurrentUser();

        // Restriction: Only Owner can add balance to their own account.
        // Others cannot add balance to their own account.
        if (targetUser.equals(currentUser)) {
            if (SessionManager.getCurrentRole() != UserRole.OWNER) {
                JOptionPane.showMessageDialog(this, "Only Owner can adjust their own balance.");
                return;
            }
        }
        
        String input = JOptionPane.showInputDialog(this, "Amount to add/remove:");
        if (input == null) return;
        try {
            double amount = Double.parseDouble(input);
            if (amount < 0 && targetRole.equalsIgnoreCase("ADMIN") && SessionManager.getCurrentRole() != UserRole.OWNER) {
                JOptionPane.showMessageDialog(this, "Only Owner can reduce Admin balance.");
                return;
            }
            BankAccountManager.updateBalance(targetUser, amount);
            LogManager.addLog(currentUser, "BALANCE_ADJUST", targetUser, "Amount: " + amount);
            refresh();
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Invalid number."); }
    }

    private void handleResetPassword() {
        int row = adminTable.getSelectedRow();
        if (row == -1) return;
        String targetUser = adminModel.getValueAt(row, 0).toString();
        String pass = JOptionPane.showInputDialog(this, "Enter new temporary password for " + targetUser + ":");
        if (pass != null && !pass.isBlank()) {
            BankAccountManager.changePassword(targetUser, pass, SessionManager.getCurrentUser());
            JOptionPane.showMessageDialog(this, "Password reset.");
        }
    }

    public void refresh() {
        adminModel.setRowCount(0);
        try (Connection conn = DatabaseManager.connect()) {
            String sql = """
                SELECT b.username, b.status, b.balance, u.role, 
                (SELECT COUNT(*) FROM deposit_requests d WHERE d.username = b.username AND d.status = 'PENDING') as pending_deposits
                FROM bank_accounts b 
                JOIN users u ON b.username = u.username 
                ORDER BY b.username ASC
                """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String status = rs.getString("status");
                    
                    if (status.equals("FROZEN_APPEAL")) {
                        status = "Frozen (REQ appeal)";
                    } else if (status.equals("TERMINATED_APPEAL")) {
                        status = "Terminated (REQ appeal)";
                    }
                    
                    int pending = rs.getInt("pending_deposits");
                    if (pending > 0) {
                        status += " (Pending Dep.)";
                    }
                    adminModel.addRow(new Object[]{
                            rs.getString("username"),
                            "₹ " + BankAccountManager.formatAmount(rs.getDouble("balance")),
                            status,
                            rs.getString("role")
                    });
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(40); // Bigger rows for professional look
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(240, 240, 240));
        table.setBackground(Color.WHITE);
        table.setBorder(null);
        return table;
    }

    private JButton createActionBtn(String text, Color bg, java.awt.event.ActionListener al) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(140, 40)); 
        btn.addActionListener(al);
        return btn;
    }

    private JButton createHeaderButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(10, 15, 10, 15));
        return btn;
    }
}
