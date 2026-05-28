package modules.finance.banking;

import core.SessionManager;
import core.UserRole;
import database.DatabaseManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class BankingPanel extends JPanel {

    private CardLayout cardLayout;
    private JPanel cards;
    private JLabel balanceLabel;
    private JLabel statusLabel;
    private JLabel lastPurchaseLabel;
    private JTextField depositField;
    private AdminBankPanel adminPanel;
    private JButton quickActionsBtn;
    private DefaultTableModel transactionModel;
    private JTable transactionTable;


    private boolean canAccessAdminDashboard() {

    return
            SessionManager.getCurrentRole()
                    == UserRole.OWNER

            ||

            (
                SessionManager.getCurrentRole()
                        == UserRole.ADMIN

                &&

                BankAccountManager
                        .getStatus(
                                SessionManager.getCurrentUser()
                        )
                        .equals("APPROVED")
            );
}

    public BankingPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);
        cards.setOpaque(false);

        String username = SessionManager.getCurrentUser();

        if (BankAccountManager.accountExists(username)) {
            refreshAllViews(username);
        } else {
            cards.add(createCreateAccountPanel(username), "CREATE");
            cardLayout.show(cards, "CREATE");
        }

        add(cards, BorderLayout.CENTER);
    }

    private void refreshAllViews(String username) {
        cards.removeAll();
        
        String status = BankAccountManager.getStatus(username);
        if (status.equals("TERMINATED") || status.equals("TERMINATED_APPEAL")) {
            cards.add(createTerminatedPanel(username), "TERMINATED");
            cardLayout.show(cards, "TERMINATED");
        } else if (status.equals("FROZEN") || status.equals("FROZEN_APPEAL")) {
            cards.add(createFrozenPanel(username), "FROZEN");
            cardLayout.show(cards, "FROZEN");
        } else {
            cards.add(createDashboard(username), "DASHBOARD");
            cards.add(createQuickActionsPanel(username), "QUICK_ACTIONS");
            cards.add(createChangePasswordPanel(username), "CHANGE_PASSWORD");
            cards.add(createTransferPanel(username), "TRANSFER");

            if (canAccessAdminDashboard()) {
                adminPanel = new AdminBankPanel(this);
                cards.add(adminPanel, "ADMIN");
            }

            cardLayout.show(cards, "DASHBOARD");
        }
        revalidate();
        repaint();
    }

    public void showDashboard() {
        cardLayout.show(cards, "DASHBOARD");
        refreshDashboard();
    }

    private void refreshDashboard() {
        String username = SessionManager.getCurrentUser();
        double balance = BankAccountManager.getBalance(username);
        String lastActivity = BankAccountManager.getLastActivity(username);
        String status = BankAccountManager.getStatus(username);
        double pending = DepositManager.getPendingDeposits(username);

        balanceLabel.setText("₹ " + BankAccountManager.formatAmount(balance));
        lastPurchaseLabel.setText(lastActivity);

        String statusText = status;
        if (pending > 0) {
            statusText += " (Pending: ₹" + BankAccountManager.formatAmount(pending) + ")";
        }
        statusLabel.setText(statusText);

        if (quickActionsBtn != null) {
            quickActionsBtn.setVisible(!status.equals("PENDING"));
        }

        updateTransactionTable(username);

switch(status) {

    case "APPROVED" ->
        statusLabel.setForeground(
                new Color(0,150,80)
        );

    case "PENDING" ->
        statusLabel.setForeground(
                new Color(255,140,0)
        );

    case "FROZEN" ->
        statusLabel.setForeground(
                Color.RED
        );

    default ->
        statusLabel.setForeground(
                Color.GRAY
        );
}
    }

    private JPanel createCreateAccountPanel(String username) {
    return createAuthStylePanel(
        "Create Bank Account",
        "Initial Setup",
        "CREATE ACCOUNT",
        (oldPass, p1, p2) -> {

            if (
                p1.isBlank()
                || p2.isBlank()
                || !p1.equals(p2)
            ) {

                JOptionPane.showMessageDialog(
                        this,
                        "Passwords must match and not be empty"
                );

                return;
            }

            if (
                BankAccountManager.createAccount(
                        username,
                        p1,
                        SessionManager.getCurrentRole().name()
                )
            ) {

                refreshAllViews(username);

            } else {

                JOptionPane.showMessageDialog(
                        this,
                        "Failed to create account"
                );
            }

        },
        false
    );
}
    private JPanel createChangePasswordPanel(String username) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        JButton back = createBorderlessBackButton();
        back.addActionListener(e -> cardLayout.show(cards, "QUICK_ACTIONS"));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.setOpaque(false);
        topBar.add(back);
        wrapper.add(topBar, BorderLayout.NORTH);

        JPanel mainAuth = createAuthStylePanel("Update Password", "Secure your account", "UPDATE PASSWORD", (oldP, newP1, newP2) -> {
            if (!BankAccountManager.verifyBankPassword(username, oldP)) {
                JOptionPane.showMessageDialog(this, "Current password incorrect.");
                return;
            }
            if (newP1.isBlank() || !newP1.equals(newP2)) {
                JOptionPane.showMessageDialog(this, "New passwords must match.");
                return;
            }
            if (newP1.equals(oldP)) {
                JOptionPane.showMessageDialog(this, "New password cannot be the same as old password.");
                return;
            }
            if (BankAccountManager.changePassword(username, newP1, username)) {
                JOptionPane.showMessageDialog(this, "Password updated successfully.");
                showDashboard();
            }
        }, true);

        wrapper.add(mainAuth, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createAuthStylePanel(String titleStr, String sub, String btnStr, AuthAction action, boolean showOld) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);

        JPanel card = new JPanel();
        card.setPreferredSize(new Dimension(450, 500));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(30, 40, 30, 40));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(titleStr);
        title.setFont(new Font("SansSerif", Font.BOLD, 32));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPasswordField oldF = createStyledPasswordField();
        JPasswordField p1 = createStyledPasswordField();
        JPasswordField p2 = createStyledPasswordField();

        JButton btn = new JButton(btnStr);
        btn.setBackground(Color.BLACK);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(Box.createVerticalGlue());
        card.add(title);
        card.add(Box.createVerticalStrut(30));

        if(showOld) {
            card.add(createLabel("CURRENT PASSWORD"));
            card.add(oldF);
            card.add(Box.createVerticalStrut(15));
        }

        card.add(createLabel("NEW PASSWORD"));
        card.add(p1);
        card.add(Box.createVerticalStrut(15));
        card.add(createLabel("CONFIRM PASSWORD"));
        card.add(p2);
        card.add(Box.createVerticalStrut(30));
        card.add(btn);
        card.add(Box.createVerticalGlue());

        btn.addActionListener(e -> action.run(new String(oldF.getPassword()), new String(p1.getPassword()), new String(p2.getPassword())));
        wrapper.add(card);
        return wrapper;
    }

    private JPanel createDashboard(String username) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 245));
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));


        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Banking Dashboard");
        title.setFont(new Font("SansSerif", Font.BOLD, 36));
        header.add(title, BorderLayout.WEST);

        JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        headerButtons.setOpaque(false);

        quickActionsBtn = createHeaderButton("QUICK ACTIONS", new Color(70, 70, 70));
        quickActionsBtn.addActionListener(e -> cardLayout.show(cards, "QUICK_ACTIONS"));
        headerButtons.add(quickActionsBtn);

        if (canAccessAdminDashboard()) {
            JButton adminBtn = createHeaderButton("ADMIN DASHBOARD", Color.BLACK);
            adminBtn.addActionListener(e -> {
                if(adminPanel != null) adminPanel.refresh();
                cardLayout.show(cards, "ADMIN");
            });
            headerButtons.add(adminBtn);
        }
        header.add(headerButtons, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);


        JPanel contentArea = new JPanel(new GridBagLayout());
        contentArea.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();

        JPanel grid = new JPanel(new GridLayout(2, 2, 25, 25));
        grid.setOpaque(false);

        balanceLabel = new JLabel("₹ 0.00");
        balanceLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        grid.add(createDashboardBox("CURRENT BALANCE", balanceLabel));

        statusLabel = new JLabel("UNKNOWN");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        statusLabel.setForeground(new Color(0, 150, 136));
        grid.add(createDashboardBox("ACCOUNT STATUS", statusLabel));

        depositField = new JTextField();
        depositField.setFont(new Font("SansSerif", Font.BOLD, 18));
        JButton submitDeposit = new JButton("SUBMIT");
        submitDeposit.setBackground(Color.BLACK);
        submitDeposit.setForeground(Color.WHITE);
        submitDeposit.addActionListener(e -> handleDeposit(username));
        JPanel depContent = new JPanel(new BorderLayout(5, 0));
        depContent.setOpaque(false);
        depContent.add(depositField, BorderLayout.CENTER);
        depContent.add(submitDeposit, BorderLayout.EAST);
        grid.add(createDashboardBox("REQUEST DEPOSIT", depContent));

        lastPurchaseLabel = new JLabel("₹ 0.00");
        lastPurchaseLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        grid.add(createDashboardBox("LAST TRANSACTION", lastPurchaseLabel));


        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1.0; gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(35, 0, 0, 0);
        contentArea.add(grid, gbc);

        gbc.gridy = 1; gbc.weighty = 0;
        gbc.insets = new Insets(30, 0, 10, 0);
        
        JPanel transHeader = new JPanel(new BorderLayout());
        transHeader.setOpaque(false);
        
        JLabel transTitle = new JLabel("RECENT TRANSACTIONS");
        transTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        transTitle.setForeground(Color.GRAY);
        transHeader.add(transTitle, BorderLayout.WEST);
        
        JButton purgeBtn = new JButton("PURGE HISTORY");
        purgeBtn.setFont(new Font("SansSerif", Font.BOLD, 10));
        purgeBtn.setBackground(new Color(211, 47, 47));
        purgeBtn.setForeground(Color.WHITE);
        purgeBtn.setFocusPainted(false);
        purgeBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to purge your transaction history?", "Confirm Purge", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (TransactionManager.purgeUserTransactions(username)) {
                    JOptionPane.showMessageDialog(this, "Transaction history purged.");
                    refreshDashboard();
                }
            }
        });
        transHeader.add(purgeBtn, BorderLayout.EAST);
        
        contentArea.add(transHeader, gbc);

        gbc.gridy = 2; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);
        transactionModel = new DefaultTableModel(new String[]{"Type", "Amount", "Details", "Time"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        transactionTable = new JTable(transactionModel);
        transactionTable.setRowHeight(35);
        transactionTable.setShowVerticalLines(false);
        transactionTable.setGridColor(new Color(240, 240, 240));
        JScrollPane transScroll = new JScrollPane(transactionTable);
        transScroll.setBorder(new LineBorder(new Color(230, 230, 230), 1));
        contentArea.add(transScroll, gbc);

        panel.add(contentArea, BorderLayout.CENTER);
        refreshDashboard();
        return panel;
    }

    private JPanel createQuickActionsPanel(String username) {

    JPanel panel =
            new JPanel(new BorderLayout());

    panel.setBackground(
            new Color(245,245,245)
    );

    panel.setBorder(
            new EmptyBorder(30,40,30,40)
    );





    JPanel header =
            new JPanel(new BorderLayout());

    header.setOpaque(false);

    JLabel title =
            new JLabel("Quick Actions");

    title.setFont(
            new Font(
                    "SansSerif",
                    Font.BOLD,
                    42
            )
    );

    header.add(title, BorderLayout.WEST);

    JButton back =
            createBorderlessBackButton();

    back.addActionListener(
            e -> showDashboard()
    );

    header.add(back, BorderLayout.EAST);

    panel.add(header, BorderLayout.NORTH);





    JPanel actionsWrapper =
            new JPanel(
                    new GridLayout(
                            2,
                            2,
                            15,
                            15
                    )
            );

    actionsWrapper.setOpaque(false);





    actionsWrapper.add(
            createQuickActionBtn(
                    "UPDATE PASSWORD",
                    e -> cardLayout.show(
                            cards,
                            "CHANGE_PASSWORD"
                    )
            )
    );





    actionsWrapper.add(
            createQuickActionBtn(
                    "TRANSFER MONEY",
                    e -> cardLayout.show(
                            cards,
                            "TRANSFER"
                    )
            )
    );





    String currentStatus =
            BankAccountManager.getStatus(username);

    String actor = BankAccountManager.getApprovedBy(username);

    boolean frozen =
            currentStatus.equals("FROZEN");

    boolean frozenByAdmin = frozen && actor != null && !actor.equals(username);

    JButton freezeBtn =
            createQuickActionBtn(
                    frozen
                            ? "UNFREEZE ACCOUNT"
                            : "FREEZE ACCOUNT",
                    e -> {
                        if (frozenByAdmin) {
                            JOptionPane.showMessageDialog(this, "Account frozen by Admin. Please appeal to unfreeze.");
                            return;
                        }

                        String password =
                                JOptionPane.showInputDialog(
                                        this,
                                        frozen
                                                ? "Enter password to unfreeze"
                                                : "Enter password to freeze"
                                );

                        if(password == null)
                            return;

                        if(
                                !BankAccountManager
                                        .verifyBankPassword(
                                                username,
                                                password
                                        )
                        ) {

                            JOptionPane.showMessageDialog(
                                    this,
                                    "Incorrect password"
                            );

                            return;
                        }

                        String newStatus =
                                frozen
                                        ? "APPROVED"
                                        : "FROZEN";

                        BankAccountManager.setStatus(
                                username,
                                newStatus,
                                username
                        );

                        LogManager.addLog(
                                username,
                                frozen
                                        ? "UNFREEZE_ACCOUNT"
                                        : "FREEZE_ACCOUNT",
                                username,
                                frozen
                                        ? "User unfroze their account"
                                        : "User froze their account"
                        );

                        JOptionPane.showMessageDialog(
                                this,
                                frozen
                                        ? "Account Unfrozen"
                                        : "Account Frozen"
                        );

                        refreshDashboard();
                        refreshAllViews(username);
                    }
            );

    actionsWrapper.add(freezeBtn);





    actionsWrapper.add(
            createQuickActionBtn(
                    "TERMINATE ACCOUNT",
                    e -> handleStatusAction(
                            username,
                            "TERMINATED",
                            "TERMINATE"
                    )
            )
    );





    JPanel centerContainer =
            new JPanel(
                    new GridBagLayout()
            );

    centerContainer.setOpaque(false);

    GridBagConstraints gbc =
            new GridBagConstraints();

    gbc.gridx = 0;
    gbc.gridy = 0;

    gbc.insets =
            new Insets(
                    50,
                    0,
                    0,
                    0
            );

    centerContainer.add(
            actionsWrapper,
            gbc
    );





    if(
            SessionManager.getCurrentRole()
                    == UserRole.ADMIN

            ||

            SessionManager.getCurrentRole()
                    == UserRole.OWNER
    ) {

        JButton logBtn =
                createQuickActionBtn(
                        "VIEW SYSTEM LOGS",
                        e -> showLogs()
                );

        logBtn.setPreferredSize(
                new Dimension(
                        515,
                        60
                )
        );

        gbc.gridy = 1;

        gbc.insets =
                new Insets(
                        15,
                        0,
                        0,
                        0
                );

        centerContainer.add(
                logBtn,
                gbc
        );
    }

    panel.add(
            centerContainer,
            BorderLayout.CENTER
    );

    return panel;
}

    private JButton createQuickActionBtn(String text, java.awt.event.ActionListener al) {
        JButton btn = new JButton(text);
        btn.setBackground(Color.BLACK);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setPreferredSize(new Dimension(250, 60));
        btn.setFocusPainted(false);
        btn.addActionListener(al);
        return btn;
    }

    private void updateTransactionTable(String username) {
        if (transactionModel == null) return;
        transactionModel.setRowCount(0);
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT type, amount, description, time FROM transactions WHERE username = ? ORDER BY time DESC LIMIT 10")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                transactionModel.addRow(new Object[]{
                        rs.getString("type"),
                        "₹ " + BankAccountManager.formatAmount(rs.getDouble("amount")),
                        rs.getString("description"),
                        rs.getString("time")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private JPanel createFrozenPanel(String username) {
        String status = BankAccountManager.getStatus(username);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(50, 50, 50, 50));

        JLabel title = new JLabel(status.equals("FROZEN_APPEAL") ? "Appeal Requested" : "Account Frozen");
        title.setFont(new Font("SansSerif", Font.BOLD, 32));
        title.setForeground(Color.ORANGE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msg = new JLabel(status.equals("FROZEN_APPEAL") 
            ? "Your appeal is being reviewed by the administration." 
            : "Your bank account has been frozen by an admin.");
        msg.setFont(new Font("SansSerif", Font.PLAIN, 16));
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton appealBtn = new JButton("APPEAL FREEZE");
        appealBtn.setBackground(Color.BLACK);
        appealBtn.setForeground(Color.WHITE);
        appealBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        appealBtn.setFocusPainted(false);
        appealBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        appealBtn.setMaximumSize(new Dimension(300, 50));
        appealBtn.setEnabled(!status.equals("FROZEN_APPEAL"));
        
        appealBtn.addActionListener(e -> {
            BankAccountManager.setStatus(username, "FROZEN_APPEAL", username);
            LogManager.addLog(username, "FREEZE_APPEAL_SUBMITTED", "SYSTEM", "User requested appeal for frozen account");
            JOptionPane.showMessageDialog(this, "Appeal submitted to Administration.");
            refreshAllViews(username);
        });

        card.add(title);
        card.add(Box.createVerticalStrut(20));
        card.add(msg);
        card.add(Box.createVerticalStrut(40));
        card.add(appealBtn);

        panel.add(card);
        return panel;
    }

    private JPanel createTerminatedPanel(String username) {
        String status = BankAccountManager.getStatus(username);
        String[] info = BankAccountManager.getTerminationInfo(username);
        String admin = (info != null) ? info[0] : "SYSTEM";
        String time = (info != null) ? info[1] : "UNKNOWN";

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(50, 50, 50, 50));

        JLabel titleLabel = new JLabel(status.equals("TERMINATED_APPEAL") ? "Appeal Requested" : "Account Terminated");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 32));
        titleLabel.setForeground(Color.RED);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msg = new JLabel(status.equals("TERMINATED_APPEAL") 
            ? "Your appeal is being reviewed by the administration." 
            : "Your bank account has been permanently terminated.");
        msg.setFont(new Font("SansSerif", Font.PLAIN, 16));
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel detail1 = new JLabel("Admin Responsible: " + admin);
        detail1.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel detail2 = new JLabel("Termination Time: " + time);
        detail2.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton appealBtn = new JButton("APPEAL TERMINATION");
        appealBtn.setBackground(Color.BLACK);
        appealBtn.setForeground(Color.WHITE);
        appealBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        appealBtn.setFocusPainted(false);
        appealBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        appealBtn.setMaximumSize(new Dimension(300, 50));
        appealBtn.setEnabled(!status.equals("TERMINATED_APPEAL"));
        
        appealBtn.addActionListener(e -> {
            BankAccountManager.setStatus(username, "TERMINATED_APPEAL", username);
            LogManager.addLog(username, "TERMINATED_APPEAL_SUBMITTED", admin, "User requested appeal for termination");
            JOptionPane.showMessageDialog(this, "Appeal submitted to " + admin + " and Owner.");
            refreshAllViews(username);
        });

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(20));
        card.add(msg);
        card.add(Box.createVerticalStrut(30));
        card.add(detail1);
        card.add(detail2);
        card.add(Box.createVerticalStrut(40));
        card.add(appealBtn);

        panel.add(card);
        return panel;
    }

    private JPanel createTransferPanel(String username) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        JButton back = createBorderlessBackButton();
        back.addActionListener(e -> cardLayout.show(cards, "QUICK_ACTIONS"));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.setOpaque(false);
        topBar.add(back);
        wrapper.add(topBar, BorderLayout.NORTH);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        JPanel card = new JPanel();
        card.setPreferredSize(new Dimension(450, 550));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(30, 40, 30, 40));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Transfer Money");
        title.setFont(new Font("SansSerif", Font.BOLD, 32));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField targetF = new JTextField();
        targetF.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        JTextField amountF = new JTextField();
        amountF.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        JPasswordField passF = createStyledPasswordField();

        JButton btn = new JButton("TRANSFER NOW");
        btn.setBackground(Color.BLACK);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);

        btn.addActionListener(e -> {
            String target = targetF.getText().trim();
            String pass = new String(passF.getPassword());
            try {
                double amount = Double.parseDouble(amountF.getText());
                if (BankAccountManager.transferMoney(username, target, amount, pass)) {
                    JOptionPane.showMessageDialog(this, "Transfer Successful!");
                    targetF.setText("");
                    amountF.setText("");
                    passF.setText("");
                    showDashboard();
                } else {
                    JOptionPane.showMessageDialog(this, "Transfer Failed. Check details and balance.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid amount.");
            }
        });

        card.add(Box.createVerticalGlue());
        card.add(title);
        card.add(Box.createVerticalStrut(30));
        card.add(createLabel("RECIPIENT USERNAME"));
        card.add(targetF);
        card.add(Box.createVerticalStrut(15));
        card.add(createLabel("AMOUNT"));
        card.add(amountF);
        card.add(Box.createVerticalStrut(15));
        card.add(createLabel("BANK PASSWORD"));
        card.add(passF);
        card.add(Box.createVerticalStrut(30));
        card.add(btn);
        card.add(Box.createVerticalGlue());

        panel.add(card);
        wrapper.add(panel, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createDashboardBox(String title, Component content) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);

        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(230, 230, 230), 1),
            new EmptyBorder(12, 18, 12, 18)
        ));

        card.setPreferredSize(new Dimension(0, 95));

        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, 11));
        t.setForeground(Color.GRAY);
        card.add(t);
        card.add(Box.createVerticalStrut(10));
        card.add(content);
        return card;
    }

    private void handleDeposit(String username) {
        String status = BankAccountManager.getStatus(username);

    if (!status.equals("APPROVED")) {

    JOptionPane.showMessageDialog(
            this,
            "Account not approved or is frozen."
    );

    return;
}

        try {
            double amount = Double.parseDouble(depositField.getText());
            if (amount > 0 && amount <= BankAccountManager.MAX_DEPOSIT) {
                if (DepositManager.requestDeposit(username, amount)) {
                    JOptionPane.showMessageDialog(this, "Request submitted.");
                    depositField.setText("");
                    refreshDashboard();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Invalid amount (Max 1T).");
            }
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Enter a valid number."); }
    }

    private void handleStatusAction(String username, String status, String action) {
        String pass = JOptionPane.showInputDialog(this, "Enter password to " + action + ":");
        if (BankAccountManager.verifyBankPassword(username, pass)) {
            BankAccountManager.setStatus(username, status, username);
            JOptionPane.showMessageDialog(this, "Account " + status + ".");
            refreshDashboard();
        } else if (pass != null) {
            JOptionPane.showMessageDialog(this, "Incorrect password.");
        }
    }

    private void showLogs() {
        JDialog logDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "System Logs", true);
        logDialog.setSize(900, 600);
        logDialog.setLocationRelativeTo(this);

        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Actor", "Action", "Target", "Details", "Time"}, 0);
        JTable table = new JTable(model);
        table.setRowHeight(30);

        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM bank_logs ORDER BY timestamp ASC")) {
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("id"), rs.getString("actor"), rs.getString("action"), rs.getString("target"), rs.getString("details"), rs.getString("timestamp")});
            }
        } catch (Exception e) { e.printStackTrace(); }

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel lTitle = new JLabel("Audit Logs");
        lTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        top.add(lTitle, BorderLayout.WEST);

        if (SessionManager.getCurrentRole() == UserRole.OWNER) {
            JButton clear = new JButton("PURGE LOGS");
            clear.setBackground(new Color(211, 47, 47));
            clear.setForeground(Color.WHITE);
            clear.addActionListener(e -> {
                if (LogManager.deleteLogs()) {
                    model.setRowCount(0);
                    JOptionPane.showMessageDialog(logDialog, "Logs purged.");
                }
            });
            top.add(clear, BorderLayout.EAST);
        }

        logDialog.add(top, BorderLayout.NORTH);
        logDialog.add(new JScrollPane(table), BorderLayout.CENTER);
        logDialog.setVisible(true);
    }

    private JButton createHeaderButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(10, 20, 10, 20));
        return btn;
    }

    private JButton createBorderlessBackButton() {
        JButton btn = new JButton("← BACK");
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setForeground(Color.BLACK);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(Color.GRAY);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private JPasswordField createStyledPasswordField() {
        JPasswordField pf = new JPasswordField();
        pf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        pf.setAlignmentX(Component.CENTER_ALIGNMENT);
        pf.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(220, 220, 220), 1), new EmptyBorder(5, 10, 5, 10)));
        return pf;
    }

    @FunctionalInterface
    interface AuthAction { void run(String p1, String p2, String p3); }
}
