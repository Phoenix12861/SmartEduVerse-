package modules.finance.passwordchecker;

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

public class PasswordCheckerPanel extends JPanel {

    private CardLayout cardLayout;
    private JPanel cards;
    private DefaultTableModel vaultModel;
    private JTable vaultTable;

    public PasswordCheckerPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);
        cards.setOpaque(false);

        cards.add(createVaultPanel(), "VAULT");
        cards.add(createAddAppPanel(), "ADD_APP");

        add(cards, BorderLayout.CENTER);
    }





    private JPanel createVaultPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(40, 60, 40, 60));


        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Password Vault");
        title.setFont(new Font("SansSerif", Font.BOLD, 42));
        header.add(title, BorderLayout.WEST);

        JPanel headerBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        headerBtns.setOpaque(false);

        if (SessionManager.getCurrentRole() == UserRole.ADMIN || SessionManager.getCurrentRole() == UserRole.OWNER) {
            JButton logBtn = createStyledButton("VIEW AUDIT LOGS", new Color(70, 70, 70), Color.WHITE);
            logBtn.addActionListener(e -> showVaultLogs());
            headerBtns.add(logBtn);
        }

        header.add(headerBtns, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);


        vaultModel = new DefaultTableModel(new String[]{"Application", "Last Updated"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        vaultTable = new JTable(vaultModel);
        vaultTable.setRowHeight(32);
        vaultTable.setFont(new Font("SansSerif", Font.PLAIN, 15));
        vaultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        vaultTable.setShowVerticalLines(false);
        vaultTable.setGridColor(new Color(240, 240, 240));
        vaultTable.setPreferredScrollableViewportSize(new Dimension(960, 330));

        JScrollPane scroll = new JScrollPane(vaultTable);
        scroll.setBorder(new LineBorder(new Color(230, 230, 230), 1));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 25));
        actions.setOpaque(false);

        JButton addBtn = createStyledButton("ADD NEW APP", Color.BLACK, Color.WHITE);
        addBtn.addActionListener(e -> cardLayout.show(cards, "ADD_APP"));
        
        JButton viewBtn = createStyledButton("VIEW/UPDATE", Color.DARK_GRAY, Color.WHITE);
        viewBtn.addActionListener(e -> handleViewUpdate());

        JButton deleteBtn = createStyledButton("DELETE", new Color(211, 47, 47), Color.WHITE);
        deleteBtn.addActionListener(e -> handleDelete());

        actions.add(addBtn);
        actions.add(viewBtn);
        actions.add(deleteBtn);

        JPanel mainContent = new JPanel(new GridBagLayout());
        mainContent.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1.0; gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTH;
        mainContent.add(scroll, gbc);
        

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        mainContent.add(actions, gbc);
        

        gbc.gridy = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        mainContent.add(Box.createGlue(), gbc);
        
        panel.add(mainContent, BorderLayout.CENTER);

        refreshVault();
        return panel;
    }





    private JPanel createAddAppPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new EmptyBorder(40, 60, 40, 60));

        JButton back = new JButton("← BACK TO VAULT");
        back.setFont(new Font("SansSerif", Font.BOLD, 12));
        back.setContentAreaFilled(false);
        back.setBorderPainted(false);
        back.setFocusPainted(false);
        back.setCursor(new Cursor(Cursor.HAND_CURSOR));
        back.addActionListener(e -> cardLayout.show(cards, "VAULT"));
        
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);
        top.add(back);
        wrapper.add(top, BorderLayout.NORTH);

        JPanel main = new JPanel(new GridBagLayout());
        main.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(220, 220, 220), 1), new EmptyBorder(30, 40, 30, 40)));
        card.setPreferredSize(new Dimension(550, 650));

        JLabel title = new JLabel("Link New Application");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField appField = new JTextField();
        appField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        appField.setAlignmentX(Component.CENTER_ALIGNMENT);
        appField.setBorder(BorderFactory.createTitledBorder("APPLICATION NAME"));

        JTextField passField = new JTextField();
        passField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        passField.setAlignmentX(Component.CENTER_ALIGNMENT);
        passField.setBorder(BorderFactory.createTitledBorder("PASSWORD"));


        JPanel genBox = new JPanel();
        genBox.setLayout(new BoxLayout(genBox, BoxLayout.Y_AXIS));
        genBox.setBackground(new Color(250, 250, 250));
        genBox.setBorder(new EmptyBorder(20, 20, 20, 20));
        genBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        genBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));

        JLabel genTitle = new JLabel("PASSWORD GENERATOR");
        genTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        genTitle.setForeground(Color.GRAY);

        JSlider lenSlider = new JSlider(8, 32, 16);
        lenSlider.setBackground(new Color(250, 250, 250));
        JLabel lenLabel = new JLabel("Length: 16");
        lenSlider.addChangeListener(e -> lenLabel.setText("Length: " + lenSlider.getValue()));

        JCheckBox upper = new JCheckBox("Uppercase", true);
        JCheckBox digit = new JCheckBox("Digits", true);
        JCheckBox symbol = new JCheckBox("Symbols", true);
        upper.setBackground(new Color(250, 250, 250));
        digit.setBackground(new Color(250, 250, 250));
        symbol.setBackground(new Color(250, 250, 250));

        JButton genBtn = new JButton("GENERATE PASSWORD");
        genBtn.setBackground(Color.BLACK);
        genBtn.setForeground(Color.WHITE);
        genBtn.setFocusPainted(false);
        genBtn.addActionListener(e -> {
            passField.setText(PasswordGenerator.generate(lenSlider.getValue(), upper.isSelected(), digit.isSelected(), symbol.isSelected()));
        });

        genBox.add(genTitle);
        genBox.add(Box.createVerticalStrut(15));
        genBox.add(lenLabel);
        genBox.add(lenSlider);
        genBox.add(upper);
        genBox.add(digit);
        genBox.add(symbol);
        genBox.add(Box.createVerticalStrut(15));
        genBox.add(genBtn);

        JButton saveBtn = new JButton("SAVE TO VAULT");
        saveBtn.setBackground(new Color(0, 150, 136));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        saveBtn.setFocusPainted(false);
        saveBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveBtn.setMaximumSize(new Dimension(300, 45));
        saveBtn.addActionListener(e -> {
            String app = appField.getText().trim();
            String pass = passField.getText().trim();
            if (app.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields.");
                return;
            }
            if (saveApp(app, pass)) {
                JOptionPane.showMessageDialog(this, "Application saved!");
                appField.setText("");
                passField.setText("");
                refreshVault();
                cardLayout.show(cards, "VAULT");
            }
        });

        card.add(title);
        card.add(Box.createVerticalStrut(30));
        card.add(appField);
        card.add(Box.createVerticalStrut(20));
        card.add(passField);
        card.add(Box.createVerticalStrut(30));
        card.add(genBox);
        card.add(Box.createVerticalStrut(40));
        card.add(saveBtn);

        main.add(card, gbc);
        wrapper.add(main, BorderLayout.CENTER);
        return wrapper;
    }





    private void refreshVault() {
        if (vaultModel == null) return;
        vaultModel.setRowCount(0);
        String username = SessionManager.getCurrentUser();
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT app_name, updated_at FROM vault WHERE username = ? ORDER BY app_name ASC")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                vaultModel.addRow(new Object[]{rs.getString("app_name"), rs.getString("updated_at")});
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private boolean saveApp(String app, String pass) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO vault (username, app_name, app_password) VALUES (?,?,?)")) {
            ps.setString(1, SessionManager.getCurrentUser());
            ps.setString(2, app);
            ps.setString(3, pass);
            ps.executeUpdate();
            logVaultAction("ADD_APP", app, "New app linked");
            return true;
        } catch (Exception e) { 
            e.printStackTrace();
            return false;
        }
    }

    private void handleViewUpdate() {
        int row = vaultTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select an application.");
            return;
        }

        String app = vaultModel.getValueAt(row, 0).toString();
        String username = SessionManager.getCurrentUser();

        String masterPass = JOptionPane.showInputDialog(this, "Confirm identity: Enter SmartEduVerse password");
        if (masterPass == null) return;

        if (!verifyMasterPassword(username, masterPass)) {
            JOptionPane.showMessageDialog(this, "Access Denied: Incorrect password.");
            logVaultAction("UNAUTHORIZED_ACCESS", app, "Failed security check");
            return;
        }


        String currentPass = "";
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT app_password FROM vault WHERE username = ? AND app_name = ?")) {
            ps.setString(1, username);
            ps.setString(2, app);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) currentPass = rs.getString("app_password");
        } catch (Exception e) { e.printStackTrace(); }

        String newPass = (String) JOptionPane.showInputDialog(this, "Application: " + app + "\n\nCurrent Password: " + currentPass + "\n\nEnter new password to update:", "Vault Update", JOptionPane.PLAIN_MESSAGE, null, null, currentPass);
        
        if (newPass != null && !newPass.isBlank() && !newPass.equals(currentPass)) {
            try (Connection conn = DatabaseManager.connect();
                 PreparedStatement ps = conn.prepareStatement("UPDATE vault SET app_password = ?, updated_at = CURRENT_TIMESTAMP WHERE username = ? AND app_name = ?")) {
                ps.setString(1, newPass);
                ps.setString(2, username);
                ps.setString(3, app);
                ps.executeUpdate();
                logVaultAction("UPDATE_PASSWORD", app, "Password modified");
                JOptionPane.showMessageDialog(this, "Updated successfully.");
                refreshVault();
            } catch (Exception e) { e.printStackTrace(); }
        } else if (newPass != null) {
            logVaultAction("VIEW_PASSWORD", app, "Password viewed");
        }
    }

    private void handleDelete() {
        int row = vaultTable.getSelectedRow();
        if (row == -1) return;

        String app = vaultModel.getValueAt(row, 0).toString();
        int confirm = JOptionPane.showConfirmDialog(this, "Permanently delete entry for " + app + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseManager.connect();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM vault WHERE username = ? AND app_name = ?")) {
                ps.setString(1, SessionManager.getCurrentUser());
                ps.setString(2, app);
                ps.executeUpdate();
                logVaultAction("DELETE_APP", app, "App removed from vault");
                refreshVault();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private boolean verifyMasterPassword(String username, String password) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT password FROM users WHERE username = ?")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("password").equals(password);
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    private void logVaultAction(String action, String target, String details) {
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO vault_logs (actor, action, target, details) VALUES (?,?,?,?)")) {
            ps.setString(1, SessionManager.getCurrentUser());
            ps.setString(2, action);
            ps.setString(3, target);
            ps.setString(4, details);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showVaultLogs() {
        JDialog logDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Vault Audit Trail", true);
        logDialog.setSize(900, 600);
        logDialog.setLocationRelativeTo(this);
        logDialog.setLayout(new BorderLayout());

        DefaultTableModel model = new DefaultTableModel(new String[]{"Actor", "Action", "Target", "Details", "Time"}, 0);
        JTable table = new JTable(model);
        table.setRowHeight(30);

        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM vault_logs ORDER BY timestamp DESC")) {
            while (rs.next()) {
                model.addRow(new Object[]{rs.getString("actor"), rs.getString("action"), rs.getString("target"), rs.getString("details"), rs.getString("timestamp")});
            }
        } catch (Exception e) { e.printStackTrace(); }

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(new EmptyBorder(15, 20, 15, 20));
        JLabel lTitle = new JLabel("Audit Logs");
        lTitle.setFont(new Font("SansSerif", Font.BOLD, 24));
        top.add(lTitle, BorderLayout.WEST);

        if (SessionManager.getCurrentRole() == UserRole.OWNER) {
            JButton purge = new JButton("PURGE LOGS");
            purge.setBackground(new Color(211, 47, 47));
            purge.setForeground(Color.WHITE);
            purge.setFocusPainted(false);
            purge.addActionListener(e -> {
                if (JOptionPane.showConfirmDialog(logDialog, "Purge all vault activity logs?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    try (Connection conn = DatabaseManager.connect(); Statement st = conn.createStatement()) {
                        st.execute("DELETE FROM vault_logs");
                        model.setRowCount(0);
                    } catch (Exception ex) { ex.printStackTrace(); }
                }
            });
            top.add(purge, BorderLayout.EAST);
        }

        logDialog.add(top, BorderLayout.NORTH);
        logDialog.add(new JScrollPane(table), BorderLayout.CENTER);
        logDialog.setVisible(true);
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(180, 40));
        return btn;
    }
}
