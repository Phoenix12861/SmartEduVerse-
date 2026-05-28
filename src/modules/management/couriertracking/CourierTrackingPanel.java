package modules.management.couriertracking;

import core.SessionManager;
import core.UserRole;
import modules.finance.banking.BankAccountManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class CourierTrackingPanel extends JPanel {

    private JPanel contentArea;
    private JPanel rightPanel;
    private JButton backBtn;

    public CourierTrackingPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        contentArea = new JPanel(new BorderLayout());
        contentArea.setBackground(Color.WHITE);

        initHeader();
        initMainView();
        
        add(contentArea, BorderLayout.CENTER);
    }

    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        left.setBackground(Color.WHITE);

        backBtn = styleBtn("← Back to My Couriers");
        backBtn.setVisible(false);
        backBtn.addActionListener(e -> initMainView());
        left.add(backBtn);

        JLabel title = new JLabel("Courier & Package Tracking");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        left.add(title);
        header.add(left, BorderLayout.WEST);

        rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setBackground(Color.WHITE);

        JButton newBtn = styleBtn("Send New Courier");
        newBtn.addActionListener(e -> showCreateForm());
        rightPanel.add(newBtn);

        UserRole role = SessionManager.getCurrentRole();
        if (role == UserRole.ADMIN || role == UserRole.OWNER) {
            JButton adminBtn = styleBtn("Admin: Manage Shipments");
            adminBtn.addActionListener(e -> showAdminView());
            rightPanel.add(adminBtn);
        }

        header.add(rightPanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    public boolean goBack() {
        if (backBtn.isVisible()) {
            initMainView();
            return true;
        }
        return false;
    }

    private void initMainView() {
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);

        List<CourierManager.Courier> my = CourierManager.getMyCouriers(SessionManager.getCurrentUser());
        
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);

        if (my.isEmpty()) {
            JLabel empty = new JLabel("No active shipments found.", SwingConstants.CENTER);
            empty.setFont(new Font("SansSerif", Font.ITALIC, 16));
            wrapper.add(empty, BorderLayout.CENTER);
        } else {
            for (CourierManager.Courier c : my) {
                listPanel.add(createCourierCard(c));
                listPanel.add(Box.createVerticalStrut(15));
            }
            wrapper.add(listPanel, BorderLayout.NORTH);
        }

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        contentArea.removeAll();
        contentArea.add(scroll, BorderLayout.CENTER);
        
        backBtn.setVisible(false);
        rightPanel.setVisible(true);
        
        contentArea.revalidate();
        contentArea.repaint();
    }

    private JPanel createCourierCard(CourierManager.Courier c) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        card.setMaximumSize(new Dimension(1000, 180));
        card.setPreferredSize(new Dimension(800, 160));

        JPanel info = new JPanel(new GridLayout(0, 1));
        info.setBackground(Color.WHITE);
        info.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        String statusText = "Status: " + c.status;
        if ("REFUNDED".equals(c.paymentStatus)) statusText += " (REFUNDED - Returning to Sender)";

        info.add(new JLabel("Courier ID: #" + c.id + " | " + statusText));
        info.add(new JLabel("From: " + c.fromAddr + " | To: " + c.toAddr));
        info.add(new JLabel("Distance: " + c.distance + " km | Amount: ₹" + c.amount));
        info.add(new JLabel("Current Location: " + c.location));
        info.add(new JLabel("Estimated Time: " + c.estimatedTime + " mins | Payment: " + c.paymentStatus));

        card.add(info, BorderLayout.CENTER);

        JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bp.setBackground(Color.WHITE);

        if (c.status.equals("DELIVERED") && c.paymentStatus.equals("PENDING")) {
            JButton pay = styleBtn("Pay Now");
            pay.setBackground(new Color(0, 120, 0));
            pay.addActionListener(e -> processPayment(c));
            bp.add(pay);
        } else if (c.status.equals("PENDING") && c.paymentStatus.equals("PENDING")) {
            JButton pay = styleBtn("Pay Now");
            pay.setBackground(new Color(0, 120, 0));
            pay.addActionListener(e -> processPayment(c));
            bp.add(pay);

            JButton cancel = styleBtn("Cancel & Refund");
            cancel.setBackground(new Color(150, 0, 0));
            cancel.addActionListener(e -> {
                if (JOptionPane.showConfirmDialog(this, "Are you sure you want to cancel?", "Cancel Courier", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    if (CourierManager.updateStatus(c.id, "CANCELLED") && CourierManager.refundCourier(c.id)) {
                        JOptionPane.showMessageDialog(this, "Courier cancelled. Amount refunded to bank.");
                        initMainView();
                    }
                }
            });
            bp.add(cancel);
        }

        JPanel bpWrapper = new JPanel(new GridBagLayout());
        bpWrapper.setBackground(Color.WHITE);
        bpWrapper.add(bp);
        card.add(bpWrapper, BorderLayout.EAST);

        return card;
    }

    private void processPayment(CourierManager.Courier c) {
        if (c.paymentStatus.equals("PAID")) {
            JOptionPane.showMessageDialog(this, "Already paid.");
            return;
        }
        JPasswordField pass = new JPasswordField();
        if (JOptionPane.showConfirmDialog(this, new Object[]{"Amount: ₹" + c.amount + "\nBank Password:", pass}, "Payment", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            String password = new String(pass.getPassword());
            if (BankAccountManager.transferMoney(SessionManager.getCurrentUser(), "SYSTEM_COURIER", c.amount, password)) {
                CourierManager.payCourier(c.id);
                JOptionPane.showMessageDialog(this, "Paid Successfully!");
                initMainView();
            } else {
                JOptionPane.showMessageDialog(this, "Payment Failed.");
            }
        }
    }

    private void showCreateForm() {
        if (CourierManager.hasUnpaidDeliveredCourier(SessionManager.getCurrentUser())) {
            JOptionPane.showMessageDialog(this, "Please pay for your delivered couriers before sending new ones.", "Payment Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Color.WHITE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createTitledBorder("Send New Courier"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.gridx = 0; gbc.gridy = 0;

        form.add(new JLabel("Receiver Username:"), gbc);
        gbc.gridx = 1;
        JTextField receiverF = new JTextField(15);
        form.add(receiverF, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("From City:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> fromCity = new JComboBox<>(CourierManager.CITY_COORDS.keySet().toArray(new String[0]));
        form.add(fromCity, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        form.add(new JLabel("To City:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> toCity = new JComboBox<>(CourierManager.CITY_COORDS.keySet().toArray(new String[0]));
        form.add(toCity, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        form.add(new JLabel("Full Address:"), gbc);
        gbc.gridx = 1;
        JTextField addrF = new JTextField(20);
        form.add(addrF, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 10, 5, 10);
        JButton submit = styleBtn("Calculate & Book");
        submit.addActionListener(e -> {
            String f = (String)fromCity.getSelectedItem();
            String t = (String)toCity.getSelectedItem();
            if (f.equals(t)) { JOptionPane.showMessageDialog(this, "Select different cities."); return; }
            double d = CourierManager.calculateDistance(f, t);
            String fFull = f + " (HQ)";
            String tFull = addrF.getText() + ", " + t;

            if (CourierManager.createCourier(SessionManager.getCurrentUser(), receiverF.getText(), fFull, tFull, d)) {
                JOptionPane.showMessageDialog(this, "Courier request created at ₹" + (d * 10));
                initMainView();
            }
        });
        form.add(submit, gbc);

        wrapper.add(form);
        showInContent(wrapper, "New Courier");
    }

    private void showAdminView() {
        JPanel admin = new JPanel(new BorderLayout());
        admin.setBackground(Color.WHITE);

        List<CourierManager.Courier> all = CourierManager.getAllCouriers();
        String[] cols = {"ID", "Sender", "Receiver", "Location", "Status", "Action"};
        Object[][] data = new Object[all.size()][6];
        for (int i = 0; i < all.size(); i++) {
            CourierManager.Courier c = all.get(i);
            data[i] = new Object[]{c.id, c.sender, c.receiver, c.location, c.status, "Update"};
        }

        JTable table = new JTable(data, cols);
        table.setRowHeight(30);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = table.getSelectedRow();
                if (row >= 0) showAdminUpdateForm(all.get(row));
            }
        });

        admin.add(new JScrollPane(table), BorderLayout.CENTER);
        
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bp.setBackground(Color.WHITE);

        if (SessionManager.getCurrentRole() == UserRole.OWNER) {
            JButton purge = styleBtn("Purge Logs");
            purge.setBackground(new Color(180, 0, 0));
            purge.addActionListener(e -> {
                if (CourierManager.purgeLogs()) {
                    JOptionPane.showMessageDialog(this, "Logs Purged.");
                    showAdminView();
                }
            });
            bp.add(purge);
        }

        admin.add(bp, BorderLayout.SOUTH);
        showInContent(admin, "Admin View");
    }

    private void showAdminUpdateForm(CourierManager.Courier c) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField locF = new JTextField(c.location, 15);
        String[] statuses = {"PENDING", "IN TRANSIT", "OUT FOR DELIVERY", "DELIVERED", "CANCELLED"};
        JComboBox<String> statusCombo = new JComboBox<>(statuses);
        statusCombo.setSelectedItem(c.status);
        
        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Update Location:"), gbc);
        gbc.gridx = 1;
        form.add(locF, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Update Status:"), gbc);
        gbc.gridx = 1;
        form.add(statusCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 5, 5, 5);
        JButton save = styleBtn("Save Changes");
        save.addActionListener(e -> {
            String newStatus = (String) statusCombo.getSelectedItem();
            CourierManager.updateLocation(c.id, locF.getText());
            CourierManager.updateStatus(c.id, newStatus);
            
            if (newStatus.equals("CANCELLED") && !c.paymentStatus.equals("REFUNDED")) {
                CourierManager.refundCourier(c.id);
            }
            
            JOptionPane.showMessageDialog(this, "Updated.");
            showAdminView();
        });
        form.add(save, gbc);

        showInContent(form, "Update Courier #" + c.id);
    }

    private void showInContent(JPanel panel, String title) {
        contentArea.removeAll();
        contentArea.add(panel, BorderLayout.CENTER);
        
        backBtn.setText("← Back to My Couriers" + (title != null ? " (" + title + ")" : ""));
        backBtn.setVisible(true);
        rightPanel.setVisible(false);
        
        contentArea.revalidate();
        contentArea.repaint();
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
