package gui;

import database.DatabaseManager;
import core.SessionManager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class LogsViewerPanel extends JPanel {

    private DefaultTableModel model;

    public LogsViewerPanel(DashboardFrame frame) {
        setLayout(new BorderLayout(15, 15));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("System Administrative Logs");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        add(title, BorderLayout.NORTH);

        String[] cols = {"Admin", "Action", "Details", "Timestamp"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(35);
        
        refreshLogs();

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);

        JButton purge = new JButton("Purge Logs (Owner Only)");
        purge.setBackground(new Color(150, 0, 0));
        purge.setForeground(Color.WHITE);
        purge.addActionListener(e -> {
            if (!SessionManager.getCurrentUser().equalsIgnoreCase("owner")) {
                JOptionPane.showMessageDialog(this, "Only the default system owner can purge logs.");
                return;
            }
            int opt = JOptionPane.showConfirmDialog(this, "Permanently delete all admin logs?", "Confirm Purge", JOptionPane.YES_NO_OPTION);
            if (opt == JOptionPane.YES_OPTION) {
                DatabaseManager.purgeLogs(SessionManager.getCurrentUser());
                refreshLogs();
            }
        });

        JButton back = new JButton("Back to Home");
        back.setBackground(Color.BLACK);
        back.setForeground(Color.WHITE);
        back.addActionListener(e -> frame.switchPanel(new HomePanel(frame)));

        bottom.add(purge);
        bottom.add(back);
        add(bottom, BorderLayout.SOUTH);
    }

    private void refreshLogs() {
        model.setRowCount(0);
        List<String[]> logs = DatabaseManager.getAdminLogs();
        for (String[] log : logs) {
            model.addRow(log);
        }
    }
}
