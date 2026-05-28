package modules.management.parking;

import core.SessionManager;
import core.UserRole;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ParkingPanel extends JPanel {

    private int selectedLotId = -1;
    private JPanel spotsPanel;
    private JComboBox<String> lotCombo;
    private List<ParkingManager.LotInfo> lots;
    private JPanel rightPanel;
    private JButton backBtn;
    private JPanel header;

    public ParkingPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initContent();
        initHeader();
    }

    private void initHeader() {
        header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setBackground(Color.WHITE);

        backBtn = styleBtn("← Back to Parking");
        backBtn.setVisible(false);
        backBtn.addActionListener(e -> restoreMainView());
        leftPanel.add(backBtn);

        JLabel title = new JLabel("Smart Parking");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(Color.BLACK);
        leftPanel.add(title);
        header.add(leftPanel, BorderLayout.WEST);

        lots = ParkingManager.getLots();
        String[] lotNames = lots.stream().map(l -> l.name + " (" + l.type + ")").toArray(String[]::new);
        lotCombo = new JComboBox<>(lotNames);
        lotCombo.setBackground(Color.WHITE);
        lotCombo.addActionListener(e -> {
            int idx = lotCombo.getSelectedIndex();
            if (idx >= 0) {
                selectedLotId = lots.get(idx).id;
                refreshSpots();
            }
        });

        if (!lots.isEmpty()) {
            lotCombo.setSelectedIndex(0);
            selectedLotId = lots.get(0).id;
        }

        rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setBackground(Color.WHITE);
        
        rightPanel.add(new JLabel("Lot: "));
        rightPanel.add(lotCombo);

        UserRole role = SessionManager.getCurrentRole();
        if (role == UserRole.ADMIN || role == UserRole.OWNER) {
            JButton logsBtn = styleBtn("View Logs");
            logsBtn.addActionListener(e -> showLogs());
            rightPanel.add(logsBtn);

            JButton vacateBtn = styleBtn("Vacate Spot");
            vacateBtn.addActionListener(e -> {
                String spotNum = JOptionPane.showInputDialog(this, "Enter Spot Number to Vacate:");
                if (spotNum != null && !spotNum.isEmpty()) {
                    try {
                        int num = Integer.parseInt(spotNum);
                        List<ParkingManager.SpotInfo> spots = ParkingManager.getSpots(selectedLotId);
                        for (ParkingManager.SpotInfo s : spots) {
                            if (s.number == num) {
                                if (s.isOccupied) {
                                    if (ParkingManager.adminVacate(s.id)) {
                                        JOptionPane.showMessageDialog(this, "Spot " + num + " vacated.");
                                        refreshSpots();
                                    }
                                } else {
                                    JOptionPane.showMessageDialog(this, "Spot " + num + " is already free.");
                                }
                                return;
                            }
                        }
                        JOptionPane.showMessageDialog(this, "Spot " + num + " not found in this lot.");
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Invalid spot number.");
                    }
                }
            });
            rightPanel.add(vacateBtn);
        }

        header.add(rightPanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    public boolean goBack() {
        if (backBtn.isVisible()) {
            restoreMainView();
            return true;
        }
        return false;
    }

    private void restoreMainView() {

        Component[] comps = getComponents();
        for (Component c : comps) {
            if (c != header) remove(c);
        }
        backBtn.setVisible(false);
        rightPanel.setVisible(true);
        initContent();
        revalidate();
        repaint();
    }

    private void initContent() {
        spotsPanel = new JPanel(new GridLayout(0, 5, 15, 15));
        spotsPanel.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(spotsPanel);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        refreshSpots();
    }

    private void refreshSpots() {
        if (spotsPanel == null) return;
        spotsPanel.removeAll();
        if (selectedLotId == -1) return;

        if (ParkingManager.checkTowed(SessionManager.getCurrentUser())) {
            JOptionPane.showMessageDialog(this, "Your car has been towed!", "Alert", JOptionPane.WARNING_MESSAGE);
        }

        List<ParkingManager.SpotInfo> spots = ParkingManager.getSpots(selectedLotId);
        boolean full = true;
        
        int count = 0;
        for (ParkingManager.SpotInfo spot : spots) {
            if (count >= 20) break;
            count++;

            JButton btn = new JButton();
            btn.setLayout(new BorderLayout());
            btn.setPreferredSize(new Dimension(100, 100));
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

            String status = "[FREE]";
            Color bgColor = Color.WHITE;
            Color fgColor = Color.BLACK;

            if (!spot.isAvailable) {
                status = "[BLOCKED]";
                bgColor = Color.GRAY;
                fgColor = Color.WHITE;
            } else if (spot.isOccupied) {
                status = "[BUSY]";
                bgColor = Color.BLACK;
                fgColor = Color.WHITE;
                if (SessionManager.getCurrentUser().equals(spot.occupant)) {
                    status = "[YOURS]";
                }
            } else {
                full = false;
            }

            btn.setBackground(bgColor);
            
            JLabel icon = new JLabel(status, SwingConstants.CENTER);
            icon.setForeground(fgColor);
            icon.setFont(new Font("SansSerif", Font.BOLD, 12));
            btn.add(icon, BorderLayout.CENTER);

            JLabel label = new JLabel("Spot " + spot.number, SwingConstants.CENTER);
            label.setForeground(fgColor);
            label.setFont(new Font("SansSerif", Font.BOLD, 14));
            btn.add(label, BorderLayout.SOUTH);

            btn.addActionListener(e -> handleSpotAction(spot));
            spotsPanel.add(btn);
        }

        if (full && !spots.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All 20 spots are FULL. Please park elsewhere.");
        }

        spotsPanel.revalidate();
        spotsPanel.repaint();
    }

    private void handleSpotAction(ParkingManager.SpotInfo spot) {
        if (!spot.isAvailable) {
            JOptionPane.showMessageDialog(this, "This spot is currently unavailable.");
            return;
        }

        if (spot.isOccupied) {
            if (SessionManager.getCurrentUser().equals(spot.occupant)) {
                if (JOptionPane.showConfirmDialog(this, "Vacate this spot?", "Vacate", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    if (ParkingManager.unpark(spot.id, SessionManager.getCurrentUser())) {
                        refreshSpots();
                    }
                }
            } else if (SessionManager.getCurrentRole() == UserRole.ADMIN || SessionManager.getCurrentRole() == UserRole.OWNER) {
                showAdminAction(spot);
            } else {
                JOptionPane.showMessageDialog(this, "This spot is occupied.");
            }
        } else {
            if (ParkingManager.park(spot.id, SessionManager.getCurrentUser())) {
                JOptionPane.showMessageDialog(this, "Parked successfully!");
                refreshSpots();
            } else {
                JOptionPane.showMessageDialog(this, "You can only occupy one slot at a time.");
            }
        }
    }

    private void showAdminAction(ParkingManager.SpotInfo spot) {
        String[] options;
        if (SessionManager.getCurrentRole() == UserRole.OWNER) {
            options = new String[]{"Vacate Anyone", spot.isAvailable ? "Make Unavailable" : "Make Available", "Cancel"};
        } else {
            options = new String[]{"Vacate Anyone", "Cancel"};
        }

        int choice = JOptionPane.showOptionDialog(this, "Spot " + spot.number + " Actions (" + (spot.isOccupied ? "Occupied by " + spot.occupant : "Free") + ")",
                "Admin Actions", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        if (choice == 0) {
            if (spot.isOccupied) {
                if (ParkingManager.adminVacate(spot.id)) {
                    JOptionPane.showMessageDialog(this, "Spot vacated. User will be notified.");
                    refreshSpots();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Spot is already free.");
            }
        } else if (choice == 1 && SessionManager.getCurrentRole() == UserRole.OWNER) {
            if (ParkingManager.toggleAvailability(spot.id, !spot.isAvailable)) {
                refreshSpots();
            }
        }
    }

    private void showLogs() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Parking Logs"));

        List<ParkingManager.ParkingLog> logs = ParkingManager.getLogs();
        String[] cols = {"User", "Action", "Details", "Time"};
        Object[][] data = new Object[logs.size()][4];
        for (int i = 0; i < logs.size(); i++) {
            ParkingManager.ParkingLog l = logs.get(i);
            data[i] = new Object[]{l.username, l.action, l.details, l.time};
        }

        JTable table = new JTable(data, cols);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bp.setBackground(Color.WHITE);

        if (SessionManager.getCurrentRole() == UserRole.OWNER) {
            JButton purge = styleBtn("Purge Logs");
            purge.setBackground(new Color(180, 0, 0));
            purge.addActionListener(e -> {
                if (ParkingManager.purgeLogs()) {
                    JOptionPane.showMessageDialog(this, "Logs Purged.");
                    showLogs();
                }
            });
            bp.add(purge);
        }

        panel.add(bp, BorderLayout.SOUTH);


        Component[] comps = getComponents();
        for (Component c : comps) {
            if (c != header) remove(c);
        }
        add(panel, BorderLayout.CENTER);
        backBtn.setVisible(true);
        rightPanel.setVisible(false);
        revalidate();
        repaint();
    }

    private JButton styleBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(Color.BLACK);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
        return b;
    }
}
