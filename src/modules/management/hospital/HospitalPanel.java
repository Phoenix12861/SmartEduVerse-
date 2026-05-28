package modules.management.hospital;

import core.SessionManager;
import core.UserRole;
import modules.finance.banking.BankAccountManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class HospitalPanel extends JPanel {

    private int selectedHospitalId = -1;
    private String selectedHospital;
    private JPanel contentArea;
    private JPanel gridPanel;
    private JComboBox<String> hospitalCombo;
    private JPanel rightPanel;
    private JButton backBtn;

    public HospitalPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        contentArea = new JPanel(new BorderLayout());
        contentArea.setBackground(Color.WHITE);
        
        gridPanel = new JPanel(new GridLayout(0, 5, 10, 10));
        gridPanel.setBackground(Color.WHITE);
        
        initHeader();
        showMainGrid();
        
        add(contentArea, BorderLayout.CENTER);
    }

    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setBackground(Color.WHITE);

        backBtn = styleBtn("← Back to Rooms");
        backBtn.setVisible(false);
        backBtn.addActionListener(e -> showMainGrid());
        leftPanel.add(backBtn);

        JLabel title = new JLabel("Hospital Management");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        leftPanel.add(title);
        header.add(leftPanel, BorderLayout.WEST);

        hospitalCombo = new JComboBox<>(HospitalManager.getHospitals().toArray(new String[0]));
        hospitalCombo.setBackground(Color.WHITE);
        hospitalCombo.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        hospitalCombo.addActionListener(e -> {
            selectedHospital = (String) hospitalCombo.getSelectedItem();
            selectedHospitalId = HospitalManager.getHospitalId(selectedHospital);
            refreshRooms();
        });

        if (hospitalCombo.getItemCount() > 0) {
            hospitalCombo.setSelectedIndex(0);
            selectedHospital = (String) hospitalCombo.getSelectedItem();
            selectedHospitalId = HospitalManager.getHospitalId(selectedHospital);
        }

        rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(Color.WHITE);
        rightPanel.add(new JLabel("Hospital: "));
        rightPanel.add(hospitalCombo);

        JButton myBills = styleBtn("My Bills");
        myBills.addActionListener(e -> showMyBills());
        rightPanel.add(myBills);

        UserRole role = SessionManager.getCurrentRole();
        if (role == UserRole.ADMIN || role == UserRole.OWNER) {
            JButton adminBtn = styleBtn("Admin Actions");
            adminBtn.addActionListener(e -> showAdminMenu());
            rightPanel.add(adminBtn);
        }

        header.add(rightPanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    public boolean goBack() {
        if (backBtn.isVisible()) {
            showMainGrid();
            return true;
        }
        return false;
    }

    private void refreshRooms() {
        gridPanel.removeAll();
        if (selectedHospitalId == -1) return;

        List<HospitalManager.RoomInfo> rooms = HospitalManager.getRooms(selectedHospitalId);
        for (HospitalManager.RoomInfo room : rooms) {
            if (room.number > 150) continue;

            JButton btn = new JButton();
            btn.setLayout(new BorderLayout());
            btn.setPreferredSize(new Dimension(110, 110));
            btn.setFocusPainted(false);
            
            Color bgColor = Color.WHITE;
            Color fgColor = Color.BLACK;
            String status = "[FREE]";

            if (room.isOccupied) {
                bgColor = Color.BLACK;
                fgColor = Color.WHITE;
                status = "[OCCUPIED]";
            }

            btn.setBackground(bgColor);
            btn.setBorder(BorderFactory.createLineBorder(Color.BLACK, room.type.equals("VIP") ? 3 : 1));
            
            JLabel icon = new JLabel(status, SwingConstants.CENTER);
            icon.setForeground(fgColor);
            icon.setFont(new Font("SansSerif", Font.BOLD, 11));
            btn.add(icon, BorderLayout.CENTER);

            JLabel label = new JLabel((room.type.equals("VIP") ? "VIP " : "") + "Room " + room.number, SwingConstants.CENTER);
            label.setForeground(fgColor);
            label.setFont(new Font("SansSerif", Font.BOLD, 12));
            btn.add(label, BorderLayout.SOUTH);

            if (!room.isOccupied) {
                btn.addActionListener(e -> showBookingForm(room));
            } else {
                btn.setEnabled(false);
            }
            
            gridPanel.add(btn);
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private void showBookingForm(HospitalManager.RoomInfo room) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Color.WHITE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createTitledBorder("Book Room " + room.number));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.gridx = 0; gbc.gridy = 0;

        form.add(new JLabel("Start Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        JTextField dateF = new JTextField(java.time.LocalDate.now().toString(), 10);
        form.add(dateF, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Days:"), gbc);
        gbc.gridx = 1;
        JTextField daysF = new JTextField("1", 10);
        form.add(daysF, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 10, 5, 10);
        
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bp.setBackground(Color.WHITE);
        JButton submit = styleBtn("Confirm Booking");
        
        submit.addActionListener(e -> {
            try {
                int days = Integer.parseInt(daysF.getText());
                if (HospitalManager.bookRoom(room.id, SessionManager.getCurrentUser(), dateF.getText(), days)) {
                    JOptionPane.showMessageDialog(this, "Room Booked Successfully!");
                    showMainGrid();
                }
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid input."); }
        });

        bp.add(submit);
        form.add(bp, gbc);

        wrapper.add(form);
        showInContent(wrapper, "Booking Room " + room.number);
    }

    private void showMyBills() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("My Pending Bills", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        panel.add(title, BorderLayout.NORTH);

        List<HospitalManager.BillInfo> bills = HospitalManager.getPendingBills(SessionManager.getCurrentUser());
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Color.WHITE);

        for (HospitalManager.BillInfo bill : bills) {
            JPanel row = new JPanel(new BorderLayout());
            row.setBackground(Color.WHITE);
            row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
            
            JLabel info = new JLabel(bill.hospitalName + " - ₹" + bill.amount);
            info.setFont(new Font("SansSerif", Font.PLAIN, 16));
            row.add(info, BorderLayout.WEST);
            
            JButton pay = styleBtn("Pay Now");
            pay.setPreferredSize(new Dimension(100, 35));
            pay.addActionListener(e -> showPaymentScreen(bill));
            row.add(pay, BorderLayout.EAST);
            list.add(row);
        }

        if (bills.isEmpty()) {
            JLabel empty = new JLabel("No pending bills.", SwingConstants.CENTER);
            empty.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
            list.add(empty);
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.add(list, BorderLayout.NORTH);

        panel.add(new JScrollPane(wrapper), BorderLayout.CENTER);
        showInContent(panel, "My Bills");
    }

    private void showPaymentScreen(HospitalManager.BillInfo bill) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;

        JLabel title = new JLabel("Payment for " + bill.hospitalName, SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        gbc.gridwidth = 2;
        panel.add(title, gbc);

        HospitalManager.BillDetails details = HospitalManager.getBillDetails(bill.id);

        gbc.gridy++; gbc.gridwidth = 1;
        panel.add(new JLabel("Room Charge:"), gbc);
        gbc.gridx = 1;
        panel.add(new JLabel("₹" + details.roomCharge), gbc);

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        JPanel itemsPanel = new JPanel();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        itemsPanel.setBackground(new Color(245, 245, 245));
        itemsPanel.setBorder(BorderFactory.createTitledBorder("Medical Supplies Breakdown"));
        
        java.util.List<HospitalManager.BillItem> items = HospitalManager.getBillItems(bill.id);
        for (HospitalManager.BillItem item : items) {
            JLabel il = new JLabel(item.name + " - ₹" + item.price);
            il.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
            itemsPanel.add(il);
        }
        panel.add(itemsPanel, gbc);

        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 1;
        panel.add(new JLabel("Total Amount:"), gbc);
        gbc.gridx = 1;
        JLabel amountLabel = new JLabel("₹" + details.totalAmount);
        amountLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(amountLabel, gbc);

        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Bank Password:"), gbc);
        gbc.gridx = 1;
        JPasswordField pass = new JPasswordField(15);
        panel.add(pass, gbc);

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        JButton payBtn = styleBtn("Confirm & Pay");
        payBtn.setPreferredSize(new Dimension(200, 45));
        payBtn.addActionListener(e -> {
            String password = new String(pass.getPassword());
            if (BankAccountManager.transferMoney(SessionManager.getCurrentUser(), "SYSTEM_HOSPITAL", bill.amount, password)) {
                HospitalManager.payBill(bill.id, SessionManager.getCurrentUser());
                JOptionPane.showMessageDialog(this, "Paid Successfully!");
                showMyBills();
            } else {
                JOptionPane.showMessageDialog(this, "Payment Failed.");
            }
        });
        panel.add(payBtn, gbc);

        showInContent(panel, "Pay Bill");
    }

    private void showAdminMenu() {
        JPanel menu = new JPanel(new GridBagLayout());
        menu.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0; gbc.gridy = 0;

        JLabel title = new JLabel("Hospital Admin", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        menu.add(title, gbc);

        gbc.gridy++;
        JButton createBill = styleBtn("Create Bill by Room");
        createBill.addActionListener(e -> showCreateBillForm());
        menu.add(createBill, gbc);

        gbc.gridy++;
        JButton viewLogs = styleBtn("View Booking Logs");
        viewLogs.addActionListener(e -> showLogs());
        menu.add(viewLogs, gbc);

        showInContent(menu, "Admin Menu");
    }

    private void showCreateBillForm() {
        showCreateBillForm(selectedHospitalId, -1);
    }

    private void showCreateBillForm(int hospitalId, int prefilledRoom) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Color.WHITE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK, 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0; gbc.gridy = 0;

        form.add(new JLabel("Room Number:"), gbc);
        gbc.gridx = 1;
        JTextField roomF = new JTextField(prefilledRoom == -1 ? "" : String.valueOf(prefilledRoom), 15);
        form.add(roomF, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Room Charge (₹):"), gbc);
        gbc.gridx = 1;
        JTextField roomChargeF = new JTextField(15);
        form.add(roomChargeF, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JPanel itemsPanel = new JPanel();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        itemsPanel.setBackground(Color.WHITE);
        java.util.List<HospitalManager.BillItem> items = new java.util.ArrayList<>();
        JScrollPane itemScroll = new JScrollPane(itemsPanel);
        itemScroll.setPreferredSize(new Dimension(300, 150));
        form.add(new JLabel("Medical Supplies:"), gbc);
        gbc.gridy = 3;
        form.add(itemScroll, gbc);

        gbc.gridy = 4;
        JButton addItem = styleBtn("Add Supply Item");
        addItem.addActionListener(e -> {
            JTextField nameF = new JTextField();
            JTextField priceF = new JTextField();
            Object[] msg = {"Supply Name:", nameF, "Price:", priceF};
            if (JOptionPane.showConfirmDialog(this, msg, "Add Item", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                try {
                    double p = Double.parseDouble(priceF.getText());
                    items.add(new HospitalManager.BillItem(nameF.getText(), p));
                    itemsPanel.add(new JLabel(nameF.getText() + " - ₹" + p));
                    itemsPanel.revalidate();
                } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid price."); }
            }
        });
        form.add(addItem, gbc);

        gbc.gridy = 5;
        gbc.insets = new Insets(15, 5, 5, 5);
        JButton submit = styleBtn("Generate Bill");
        submit.addActionListener(e -> {
            try {
                int rn = Integer.parseInt(roomF.getText());
                double rc = Double.parseDouble(roomChargeF.getText());
                if (HospitalManager.createBillByRoom(hospitalId, rn, rc, items)) {
                    JOptionPane.showMessageDialog(this, "Bill Created Successfully.");
                    showMainGrid();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed. Is there an active booking for this room?");
                }
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid input. Please check numbers."); }
        });
        form.add(submit, gbc);

        wrapper.add(form);
        showInContent(wrapper, "Create Bill");
    }

    private void showLogs() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        List<HospitalManager.BookingLog> logs = HospitalManager.getLogs();
        String[] cols = {"User", "Hospital", "Room", "Start Date", "Days", "Status", "Booked At"};
        
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        for (HospitalManager.BookingLog l : logs) {
            model.addRow(new Object[]{l.username, l.hospitalName, l.roomNumber, l.startDate, l.days, l.status, l.createdAt});
        }

        JTable table = new JTable(model);
        table.setRowHeight(30);
        
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                
                String startDateStr = (String) t.getValueAt(r, 3);
                int bookedDays = (int) t.getValueAt(r, 4);
                String status = (String) t.getValueAt(r, 5);
                
                try {
                    LocalDate start = LocalDate.parse(startDateStr);
                    long elapsed = ChronoUnit.DAYS.between(start, LocalDate.now());
                    
                    if (status.equals("ACTIVE") && elapsed >= bookedDays) {
                        comp.setForeground(Color.RED);
                    } else {
                        comp.setForeground(s ? t.getSelectionForeground() : t.getForeground());
                    }
                } catch (Exception e) {
                    comp.setForeground(s ? t.getSelectionForeground() : t.getForeground());
                }
                return comp;
            }
        });

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bp.setBackground(Color.WHITE);

        if (SessionManager.getCurrentRole() == UserRole.OWNER) {
            JButton purge = styleBtn("Purge Logs");
            purge.setBackground(new Color(180, 0, 0));
            purge.addActionListener(e -> {
                if (HospitalManager.purgeLogs()) {
                    JOptionPane.showMessageDialog(this, "Logs Purged.");
                    showLogs();
                }
            });
            bp.add(purge);
        }

        JButton createBillBtn = styleBtn("Create Bill for Selected");
        createBillBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                HospitalManager.BookingLog log = logs.get(row);
                if (log.status.equals("ACTIVE")) {
                    showCreateBillForm(log.hospitalId, log.roomNumber);
                } else {
                    JOptionPane.showMessageDialog(this, "Can only create bill for ACTIVE bookings.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a log entry.");
            }
        });

        bp.add(createBillBtn);
        panel.add(bp, BorderLayout.SOUTH);

        showInContent(panel, "Booking Logs");
    }

    private void showInContent(JPanel panel, String title) {
        contentArea.removeAll();
        contentArea.add(panel, BorderLayout.CENTER);
        
        backBtn.setText("← Back to Rooms" + (title != null ? " (" + title + ")" : ""));
        backBtn.setVisible(true);
        rightPanel.setVisible(false);
        
        contentArea.revalidate();
        contentArea.repaint();
    }

    private void showMainGrid() {
        contentArea.removeAll();
        JScrollPane scroll = new JScrollPane(gridPanel);
        scroll.setBorder(null);
        contentArea.add(scroll, BorderLayout.CENTER);
        
        backBtn.setVisible(false);
        rightPanel.setVisible(true);
        
        refreshRooms();
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
