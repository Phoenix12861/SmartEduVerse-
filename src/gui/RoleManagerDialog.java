package gui;

import core.*;
import database.DatabaseManager;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

public class RoleManagerDialog extends JDialog {

    private DefaultTableModel model;
    private JTable table;

    public RoleManagerDialog(JFrame parent) {
        super(parent, "Role Manager", true);
        setSize(900, 600);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(15,15));

        getContentPane().setBackground(new Color(245,245,245));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(new Color(245,245,245));
        topPanel.setBorder(BorderFactory.createEmptyBorder(20,20,0,20));

        JLabel title = new JLabel("User / Role Management");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        JLabel subtitle = new JLabel("Create, edit, review and manage system users");
        subtitle.setForeground(Color.GRAY);

        topPanel.add(title);
        topPanel.add(Box.createVerticalStrut(5));
        topPanel.add(subtitle);
        add(topPanel, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{"Username", "Role"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setRowHeight(38);
        table.getColumnModel().getColumn(1).setCellRenderer(new RoleRenderer());
        refreshTable();

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10,0,10,20));

        JButton deleteBtn = createButton("Delete User");
        JButton changeRoleBtn = createButton("Change Role");
        JButton resetPassBtn = createButton("Reset Password");

        rightPanel.add(deleteBtn);
        rightPanel.add(Box.createVerticalStrut(12));
        rightPanel.add(changeRoleBtn);
        rightPanel.add(Box.createVerticalStrut(12));
        rightPanel.add(resetPassBtn);
        add(rightPanel, BorderLayout.EAST);

        // ================= BOTTOM CREATE PANEL =================

        JPanel bottomPanel = new JPanel(new GridLayout(2,4,12,12));
        bottomPanel.setBackground(new Color(245,245,245));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10,20,20,20));

        JTextField userField = new JTextField();
        JTextField passField = new JTextField();
        
        JComboBox<UserRole> roleBox;
        if(SessionManager.getCurrentRole() == UserRole.ADMIN) {
            roleBox = new JComboBox<>(new UserRole[]{UserRole.STUDENT, UserRole.USER});
        } else {
            roleBox = new JComboBox<>(UserRole.values());
        }

        JButton createBtn = createButton("Create User");

        bottomPanel.add(new JLabel("Username"));
        bottomPanel.add(new JLabel("Password"));
        bottomPanel.add(new JLabel("Role"));
        bottomPanel.add(new JLabel(""));

        bottomPanel.add(userField);
        bottomPanel.add(passField);
        bottomPanel.add(roleBox);
        bottomPanel.add(createBtn);

        add(bottomPanel, BorderLayout.SOUTH);

        // ================= ACTIONS =================

        createBtn.addActionListener(e -> {
            if(userField.getText().isBlank() || passField.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "Fields cannot be empty");
                return;
            }
            UserManager.createUser(userField.getText(), passField.getText(), (UserRole) roleBox.getSelectedItem());
            refreshTable();
            userField.setText("");
            passField.setText("");
            JOptionPane.showMessageDialog(this, "User Created");
        });

        changeRoleBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) return;
            String username = model.getValueAt(row, 0).toString();
            String currentRoleStr = model.getValueAt(row, 1).toString();

            if (SessionManager.getCurrentRole() == UserRole.ADMIN && (currentRoleStr.equals("ADMIN") || currentRoleStr.equals("OWNER"))) {
                JOptionPane.showMessageDialog(this, "Admins cannot modify higher roles.");
                return;
            }

            UserRole[] options = (SessionManager.getCurrentRole() == UserRole.ADMIN) ? new UserRole[]{UserRole.STUDENT, UserRole.USER} : UserRole.values();
            UserRole newRole = (UserRole) JOptionPane.showInputDialog(this, "Select New Role", "Change Role", JOptionPane.PLAIN_MESSAGE, null, options, UserRole.USER);

            if (newRole != null) {
                UserManager.updateRole(username, newRole);
                refreshTable();
            }
        });

        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) return;
            String username = model.getValueAt(row, 0).toString();
            if (username.equalsIgnoreCase("owner")) {
                JOptionPane.showMessageDialog(this, "Owner account protected.");
                return;
            }
            if (JOptionPane.showConfirmDialog(this, "Delete " + username + "?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                UserManager.deleteUser(username);
                refreshTable();
            }
        });
    }

    private void refreshTable() {
        model.setRowCount(0);
        for (Map.Entry<String, UserRole> entry : UserManager.getAllUsers().entrySet()) {
            model.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }
    }

    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(Color.BLACK);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(180,45));
        return btn;
    }

    private static class RoleRenderer extends DefaultTableCellRenderer {
        @Override protected void setValue(Object value) {
            setText(value.toString());
            switch(value.toString()) {
                case "OWNER" -> setForeground(new Color(180,0,0));
                case "ADMIN" -> setForeground(new Color(0,70,180));
                case "STUDENT" -> setForeground(new Color(0,140,70));
                default -> setForeground(Color.DARK_GRAY);
            }
        }
    }
}
