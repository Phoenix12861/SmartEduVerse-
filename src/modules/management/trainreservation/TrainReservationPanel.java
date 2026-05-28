package modules.management.trainreservation;

import core.SessionManager;
import core.UserRole;
import modules.finance.banking.BankAccountManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class TrainReservationPanel extends JPanel {

    private JComboBox<TicketManager.StationInfo> fromCombo, toCombo;
    private JTable trainTable;
    private DefaultTableModel tableModel;
    private List<TicketManager.TrainInfo> currentTrains;
    private JPanel contentArea;

    public TrainReservationPanel() {
        TicketManager.refreshDailyCapacity();
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        contentArea = new JPanel(new BorderLayout());
        contentArea.setBackground(Color.WHITE);

        initHeader();
        initTableView();
        
        add(contentArea, BorderLayout.CENTER);
    }

    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setBackground(Color.WHITE);

        JLabel title = new JLabel("Train Reservation");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        leftPanel.add(title);
        header.add(leftPanel, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.setBackground(Color.WHITE);

        UserRole role = SessionManager.getCurrentRole();
        if (role == UserRole.ADMIN || role == UserRole.OWNER) {
            JButton adminBtn = styleBtn("Admin Actions");
            adminBtn.addActionListener(e -> showAdminMenu());
            right.add(adminBtn);
        }

        header.add(right, BorderLayout.EAST);
        
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        searchPanel.setBackground(Color.WHITE);

        List<TicketManager.StationInfo> stations = TicketManager.getStations();
        fromCombo = new JComboBox<>(stations.toArray(new TicketManager.StationInfo[0]));
        toCombo = new JComboBox<>(stations.toArray(new TicketManager.StationInfo[0]));
        
        fromCombo.setBackground(Color.WHITE);
        fromCombo.setPreferredSize(new Dimension(180, 30));
        fromCombo.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        fromCombo.addActionListener(e -> refreshTrains());
        
        toCombo.setBackground(Color.WHITE);
        toCombo.setPreferredSize(new Dimension(180, 30));
        toCombo.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        toCombo.addActionListener(e -> refreshTrains());

        searchPanel.add(new JLabel("From:"));
        searchPanel.add(fromCombo);
        searchPanel.add(new JLabel("To:"));
        searchPanel.add(toCombo);

        JButton refreshBtn = styleBtn("Refresh List");
        refreshBtn.addActionListener(e -> refreshTrains());
        searchPanel.add(refreshBtn);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);
        topPanel.add(header, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
    }

    private void initTableView() {
        String[] columns = {"Train Name", "Departure", "Capacity", "Status", "Price", "Action"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        trainTable = new JTable(tableModel);
        trainTable.setRowHeight(40);
        trainTable.setBackground(Color.WHITE);
        trainTable.setGridColor(Color.BLACK);
        trainTable.getTableHeader().setBackground(Color.BLACK);
        trainTable.getTableHeader().setForeground(Color.WHITE);
        
        trainTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = trainTable.getSelectedRow();
                if (row >= 0) {
                    TicketManager.TrainInfo train = currentTrains.get(row);
                    processBooking(train);
                }
            }
        });

        JScrollPane scroll = new JScrollPane(trainTable);
        scroll.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        contentArea.add(scroll, BorderLayout.CENTER);
    }

    private void refreshTrains() {
        TicketManager.StationInfo from = (TicketManager.StationInfo) fromCombo.getSelectedItem();
        TicketManager.StationInfo to = (TicketManager.StationInfo) toCombo.getSelectedItem();

        tableModel.setRowCount(0);

        if (from == null || to == null) return;
        
        if (from.id == to.id) {

            return;
        }

        currentTrains = TicketManager.getTrains(from.id, to.id);

        for (TicketManager.TrainInfo t : currentTrains) {
            tableModel.addRow(new Object[]{
                t.name, t.departureTime, t.currentOccupancy + "/" + t.maxCapacity, t.status, "₹" + t.price, "Book Now"
            });
        }
    }

    private void processBooking(TicketManager.TrainInfo train) {
        if (!train.status.equals("SCHEDULED")) {
            JOptionPane.showMessageDialog(this, "Train is not available for booking.");
            return;
        }
        
        JPasswordField pass = new JPasswordField();
        if (JOptionPane.showConfirmDialog(this, new Object[]{"Pay ₹" + train.price + " for " + train.name, pass}, "Booking", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            if (BankAccountManager.transferMoney(SessionManager.getCurrentUser(), "SYSTEM_TRAIN", train.price, new String(pass.getPassword()))) {
                TicketManager.bookTicket(train.id, SessionManager.getCurrentUser(), train.price);
                JOptionPane.showMessageDialog(this, "Ticket Booked Successfully!");
                refreshTrains();
            }
        }
    }

    private void showAdminMenu() {
        String[] options = {"Start Train", "Delay/Cancel Selected", "Back"};
        int choice = JOptionPane.showOptionDialog(this, "Admin Tools", "Admin",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        if (choice == 0) {
            int row = trainTable.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Select a train from the table first."); return; }
            TicketManager.TrainInfo train = currentTrains.get(row);
            if (TicketManager.updateTrainStatus(train.id, "STARTED")) {
                JOptionPane.showMessageDialog(this, "Train " + train.name + " has started.");
                refreshTrains();
            }
        } else if (choice == 1) {
            int row = trainTable.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Select a train from the table first."); return; }
            TicketManager.TrainInfo train = currentTrains.get(row);
            String status = JOptionPane.showInputDialog("New Status (DELAYED/CANCELLED):");
            if (status != null) {
                TicketManager.updateTrainStatus(train.id, status.toUpperCase());
                refreshTrains();
            }
        }
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
