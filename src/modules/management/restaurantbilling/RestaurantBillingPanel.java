package modules.management.restaurantbilling;

import core.SessionManager;
import core.UserRole;
import modules.finance.banking.BankAccountManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalTime;

public class RestaurantBillingPanel extends JPanel {

    private String selectedRestaurant;
    private int selectedRestaurantId;
    private JPanel contentArea;
    private JPanel gridPanel;
    private JComboBox<String> restaurantCombo;
    private JPanel rightPanel;
    private JButton backBtn;

    public RestaurantBillingPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        contentArea = new JPanel(new BorderLayout());
        contentArea.setBackground(Color.WHITE);
        
        gridPanel = new JPanel(new GridLayout(0, 5, 15, 15));
        gridPanel.setBackground(Color.WHITE);
        
        initHeader();
        showMainGrid();
        
        add(contentArea, BorderLayout.CENTER);
    }

    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setBackground(Color.WHITE);
        
        backBtn = styleBtn("← Back to Tables");
        backBtn.setVisible(false);
        backBtn.addActionListener(e -> showMainGrid());
        leftPanel.add(backBtn);

        restaurantCombo = new JComboBox<>(RestaurantManager.getRestaurants().toArray(new String[0]));
        restaurantCombo.setPreferredSize(new Dimension(200, 30));
        restaurantCombo.setBackground(Color.WHITE);
        restaurantCombo.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        restaurantCombo.addActionListener(e -> {
            selectedRestaurant = (String) restaurantCombo.getSelectedItem();
            selectedRestaurantId = RestaurantManager.getRestaurantId(selectedRestaurant);
            refreshTables();
        });
        
        if (restaurantCombo.getItemCount() > 0) {
            restaurantCombo.setSelectedIndex(0);
            selectedRestaurant = (String) restaurantCombo.getSelectedItem();
            selectedRestaurantId = RestaurantManager.getRestaurantId(selectedRestaurant);
        }
        
        leftPanel.add(new JLabel(" Restaurant: "));
        leftPanel.add(restaurantCombo);
        header.add(leftPanel, BorderLayout.WEST);

        rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setBackground(Color.WHITE);

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

    private void refreshTables() {
        gridPanel.removeAll();
        if (selectedRestaurantId == -1) return;

        List<RestaurantManager.TableInfo> tables = RestaurantManager.getTables(selectedRestaurantId);
        for (RestaurantManager.TableInfo table : tables) {
            JButton btn = new JButton();
            btn.setLayout(new BorderLayout());
            btn.setPreferredSize(new Dimension(120, 120));
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));

            String statusText = "FREE";
            Color bgColor = Color.WHITE;
            Color fgColor = Color.BLACK;

            if (table.isOccupied) {
                statusText = "OCCUPIED";
                bgColor = new Color(50, 50, 50);
                fgColor = Color.WHITE;
                if (table.forcePay) {
                    statusText = "PAY NOW";
                    bgColor = new Color(180, 0, 0);
                }
            } else if (table.isReserved) {
                statusText = "Reserved";
                bgColor = new Color(245, 245, 245);
            }

            btn.setBackground(bgColor);
            
            JLabel tableLabel = new JLabel("Table " + table.number, SwingConstants.CENTER);
            tableLabel.setForeground(fgColor);
            tableLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            btn.add(tableLabel, BorderLayout.CENTER);

            JPanel subPanel = new JPanel(new GridLayout(2, 1));
            subPanel.setOpaque(false);
            
            JLabel statusLabel = new JLabel(statusText, SwingConstants.CENTER);
            statusLabel.setForeground(fgColor);
            statusLabel.setFont(new Font("SansSerif", table.isReserved ? Font.PLAIN : Font.BOLD, table.isReserved ? 10 : 12));
            subPanel.add(statusLabel);
            
            if (table.isReserved || (table.isOccupied && !table.forcePay)) {
                JLabel timeLabel = new JLabel(table.reservedUntil, SwingConstants.CENTER);
                timeLabel.setForeground(fgColor);
                timeLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
                subPanel.add(timeLabel);
            }
            
            btn.add(subPanel, BorderLayout.SOUTH);

            btn.addActionListener(e -> handleTableAction(table));
            gridPanel.add(btn);
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private void handleTableAction(RestaurantManager.TableInfo table) {
        if (table.forcePay) {
             showForcePayScreen(table);
             return;
        }

        if (table.isOccupied) {
            JOptionPane.showMessageDialog(this, "Table " + table.number + " is currently occupied.");
            return;
        }

        String[] options = {"Reserve", "Cancel My Reservation", "Close"};
        int choice = JOptionPane.showOptionDialog(this, "Table " + table.number, "Table Options",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        if (choice == 0) showReservationForm(table);
        else if (choice == 1) {
            if (table.reservationId != -1) {
                if (RestaurantManager.cancelReservation(table.reservationId, SessionManager.getCurrentUser())) {
                    JOptionPane.showMessageDialog(this, "Reservation Cancelled.");
                    refreshTables();
                } else {
                    JOptionPane.showMessageDialog(this, "You can only cancel your own reservations.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "No active reservation for this table.");
            }
        }
    }

    private void showForcePayScreen(RestaurantManager.TableInfo table) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.gridx = 0; gbc.gridy = 0;

        JLabel msg = new JLabel("<html><center><h2 style='color:red;'>Time Expired!</h2>" +
                "Please leave the table politely as the next reservation is waiting.<br>" +
                "Proceed to payment to vacate the table.</center></html>");
        msg.setFont(new Font("SansSerif", Font.PLAIN, 16));
        panel.add(msg, gbc);

        gbc.gridy++;
        JButton payBtn = styleBtn("PAY BILL");
        payBtn.setPreferredSize(new Dimension(200, 50));
        payBtn.addActionListener(e -> showMyBills());
        panel.add(payBtn, gbc);

        showInContent(panel, "Force Pay");
    }


    private void showReservationForm(RestaurantManager.TableInfo table) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Color.WHITE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
            BorderFactory.createEmptyBorder(30, 40, 30, 40)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0; gbc.gridy = 0;

        JLabel title = new JLabel("Reserve Table " + table.number, SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        gbc.gridwidth = 2;
        form.add(title, gbc);

        gbc.gridwidth = 1; gbc.gridy++;
        form.add(new JLabel("Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        JTextField dateF = new JTextField(LocalDate.now().toString(), 10);
        form.add(dateF, gbc);

        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Time (HH:MM):"), gbc);
        gbc.gridx = 1;
        JTextField timeF = new JTextField(LocalTime.now().plusMinutes(30).toString().substring(0, 5), 10);
        form.add(timeF, gbc);

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 5, 10);
        JButton submit = styleBtn("Confirm Reservation");
        submit.setPreferredSize(new Dimension(0, 45));
        submit.addActionListener(e -> {
            if (RestaurantManager.makeReservation(table.id, SessionManager.getCurrentUser(), dateF.getText(), timeF.getText())) {
                JOptionPane.showMessageDialog(this, "Reserved Successfully!");
                showMainGrid();
            } else {
                JOptionPane.showMessageDialog(this, "Failed. Table unavailable (120m buffer) or 2/day limit reached.");
            }
        });
        form.add(submit, gbc);

        wrapper.add(form);
        showInContent(wrapper, "Reservation");
    }

    private void showMyBills() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("My Pending Bills", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        panel.add(title, BorderLayout.NORTH);

        List<RestaurantManager.BillInfo> bills = RestaurantManager.getPendingBills(SessionManager.getCurrentUser());
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Color.WHITE);

        for (RestaurantManager.BillInfo bill : bills) {
            JPanel row = new JPanel(new BorderLayout());
            row.setBackground(Color.WHITE);
            row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
            
            JLabel billLabel = new JLabel(bill.restaurantName + " - ₹" + String.format("%.2f", bill.amount));
            billLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
            row.add(billLabel, BorderLayout.WEST);
            
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

    private void showPaymentScreen(RestaurantManager.BillInfo bill) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;

        JLabel title = new JLabel("Payment for " + bill.restaurantName, SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Total Amount:"), gbc);
        gbc.gridx = 1;
        JLabel amountLabel = new JLabel("₹" + String.format("%.2f", bill.amount));
        amountLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(amountLabel, gbc);

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        JPanel itemsPanel = new JPanel();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        itemsPanel.setBackground(new Color(245, 245, 245));
        itemsPanel.setBorder(BorderFactory.createTitledBorder("Bill Details"));
        
        List<RestaurantManager.BillItem> items = RestaurantManager.getBillItems(bill.id);
        for (RestaurantManager.BillItem item : items) {
            JLabel il = new JLabel(item.name + " - ₹" + String.format("%.2f", item.price));
            il.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
            itemsPanel.add(il);
        }
        panel.add(itemsPanel, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Bank Password:"), gbc);
        gbc.gridx = 1;
        JPasswordField pass = new JPasswordField(15);
        panel.add(pass, gbc);

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        JButton payBtn = styleBtn("Confirm & Pay");
        payBtn.setPreferredSize(new Dimension(200, 45));
        payBtn.addActionListener(e -> {
            String password = new String(pass.getPassword());
            if (BankAccountManager.transferMoney(SessionManager.getCurrentUser(), "SYSTEM_RESTAURANT", bill.amount, password)) {
                RestaurantManager.payBill(bill.id, SessionManager.getCurrentUser());
                JOptionPane.showMessageDialog(this, "Paid Successfully!");
                showMyBills();
            } else {
                JOptionPane.showMessageDialog(this, "Payment Failed. Check balance or password.");
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

        JLabel title = new JLabel("Admin Dashboard", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        menu.add(title, gbc);

        gbc.gridy++;
        JButton createBill = styleBtn("Create Bill by Table");
        createBill.setPreferredSize(new Dimension(250, 45));
        createBill.addActionListener(e -> showCreateBillForm());
        menu.add(createBill, gbc);

        gbc.gridy++;
        JButton viewLogs = styleBtn("View Reservation Logs");
        viewLogs.setPreferredSize(new Dimension(250, 45));
        viewLogs.addActionListener(e -> showLogs());
        menu.add(viewLogs, gbc);

        showInContent(menu, "Admin Menu");
    }

    private void showCreateBillForm() {
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

        form.add(new JLabel("Table Number:"), gbc);
        gbc.gridx = 1;
        JTextField tableF = new JTextField(10);
        form.add(tableF, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        JPanel itemsPanel = new JPanel();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        itemsPanel.setBackground(Color.WHITE);
        List<RestaurantManager.BillItem> items = new ArrayList<>();
        JScrollPane itemScroll = new JScrollPane(itemsPanel);
        itemScroll.setPreferredSize(new Dimension(300, 200));
        form.add(new JLabel("Bill Items:"), gbc);
        gbc.gridy = 2;
        form.add(itemScroll, gbc);

        gbc.gridy = 3;
        JButton addItem = styleBtn("Add Item");
        addItem.addActionListener(e -> {
            JTextField nameF = new JTextField();
            JTextField priceF = new JTextField();
            Object[] msg = {"Item Name:", nameF, "Price:", priceF};
            if (JOptionPane.showConfirmDialog(this, msg, "Add Item", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                try {
                    double p = Double.parseDouble(priceF.getText());
                    items.add(new RestaurantManager.BillItem(nameF.getText(), p));
                    itemsPanel.add(new JLabel(nameF.getText() + " - ₹" + p));
                    itemsPanel.revalidate();
                } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid price."); }
            }
        });
        form.add(addItem, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(15, 5, 5, 5);
        JButton submit = styleBtn("Confirm & Create Bill");
        submit.addActionListener(e -> {
            try {
                int tn = Integer.parseInt(tableF.getText());
                if (RestaurantManager.createBillByTable(selectedRestaurantId, tn, items)) {
                    JOptionPane.showMessageDialog(this, "Bill Created (including extra stay charges if any).");
                    showMainGrid();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed. Ensure table is occupied.");
                }
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid table number."); }
        });
        form.add(submit, gbc);

        wrapper.add(form);
        showInContent(wrapper, "Create Bill");
    }

    private void showLogs() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Reservation Logs"));

        List<RestaurantManager.ReservationLog> logs = RestaurantManager.getOwnerLogs();
        String[] cols = {"User", "Restaurant", "Table", "Date", "Time", "Status"};
        Object[][] data = new Object[logs.size()][6];
        for (int i = 0; i < logs.size(); i++) {
            data[i] = new Object[]{logs.get(i).username, logs.get(i).restaurantName, logs.get(i).tableNumber, logs.get(i).date, logs.get(i).time, logs.get(i).status};
        }

        JTable table = new JTable(data, cols);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bp.setBackground(Color.WHITE);

        if (SessionManager.getCurrentRole() == UserRole.OWNER) {
            JButton purge = styleBtn("Purge Logs");
            purge.setBackground(new Color(180, 0, 0));
            purge.addActionListener(e -> {
                if (RestaurantManager.purgeLogs()) {
                    JOptionPane.showMessageDialog(this, "Logs Purged.");
                    showLogs();
                }
            });
            bp.add(purge);
        }

        panel.add(bp, BorderLayout.SOUTH);
        showInContent(panel, "Logs");
    }


    private void showInContent(JPanel panel, String title) {
        contentArea.removeAll();
        contentArea.add(panel, BorderLayout.CENTER);
        
        backBtn.setText("← Back to Tables" + (title != null ? " (" + title + ")" : ""));
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
        
        refreshTables();
    }

    private JButton styleBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(Color.BLACK);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        return b;
    }
}
