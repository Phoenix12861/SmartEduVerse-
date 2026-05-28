package modules.finance.electricitybill;

import core.SessionManager;
import core.UserRole;
import modules.finance.banking.BankAccountManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ElectricityBillPanel extends JPanel {

    private JPanel contentArea;
    private JPanel mainPanel;

    public ElectricityBillPanel() {
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

        JLabel title = new JLabel("Electricity Services");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        header.add(title, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.setBackground(Color.WHITE);

        UserRole role = SessionManager.getCurrentRole();
        if (role == UserRole.ADMIN || role == UserRole.OWNER) {
            JButton adminBtn = styleBtn("Admin: Manage Bills");
            adminBtn.addActionListener(e -> showAdminView());
            right.add(adminBtn);
        }

        header.add(right, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    private void initMainView() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBackground(Color.WHITE);

        List<ElectricityManager.Bill> bills = ElectricityManager.getBills(SessionManager.getCurrentUser());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        if (bills.isEmpty()) {
            JLabel label = new JLabel("No outstanding bills found.", SwingConstants.CENTER);
            label.setFont(new Font("SansSerif", Font.ITALIC, 18));
            mainPanel.add(label);
        } else {
            for (ElectricityManager.Bill b : bills) {
                mainPanel.add(createBillCard(b), gbc);
                gbc.gridy++;
            }
        }

        JScrollPane scroll = new JScrollPane(mainPanel);
        scroll.setBorder(null);
        contentArea.removeAll();
        contentArea.add(scroll, BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
    }

    private JPanel createBillCard(ElectricityManager.Bill b) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        card.setMaximumSize(new Dimension(800, 150));
        card.setPreferredSize(new Dimension(800, 150));

        JPanel left = new JPanel(new GridLayout(0, 1));
        left.setBackground(Color.WHITE);
        left.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        left.add(new JLabel("Bill ID: #" + b.id + " | Units: " + b.units));
        left.add(new JLabel("Due Date: " + b.dueDate));
        left.add(new JLabel("Amount: ₹" + b.amount + " | Fine: ₹" + b.fine));
        
        JLabel total = new JLabel("Total: ₹" + b.totalAmount);
        total.setFont(new Font("SansSerif", Font.BOLD, 18));
        left.add(total);

        card.add(left, BorderLayout.CENTER);

        if (b.status.equals("PENDING")) {
            JButton pay = styleBtn("Pay Now");
            pay.addActionListener(e -> processPayment(b));
            JPanel bp = new JPanel(new GridBagLayout());
            bp.setBackground(Color.WHITE);
            bp.add(pay);
            card.add(bp, BorderLayout.EAST);
        } else {
            JLabel paid = new JLabel("PAID", SwingConstants.CENTER);
            paid.setFont(new Font("SansSerif", Font.BOLD, 20));
            paid.setForeground(new Color(0, 150, 0));
            paid.setPreferredSize(new Dimension(100, 0));
            card.add(paid, BorderLayout.EAST);
        }

        return card;
    }

    private void processPayment(ElectricityManager.Bill b) {
        JPasswordField pass = new JPasswordField();
        if (JOptionPane.showConfirmDialog(this, new Object[]{"Enter Bank Password to pay ₹" + b.totalAmount, pass}, "Payment", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            String p = new String(pass.getPassword());
            if (BankAccountManager.transferMoney(SessionManager.getCurrentUser(), "SYSTEM_ELECTRICITY", b.totalAmount, p)) {
                ElectricityManager.payBill(b.id);
                JOptionPane.showMessageDialog(this, "Bill Paid Successfully.");
                initMainView();
            } else {
                JOptionPane.showMessageDialog(this, "Payment Failed.");
            }
        }
    }

    private void showAdminView() {
        JPanel adminPanel = new JPanel(new BorderLayout());
        adminPanel.setBackground(Color.WHITE);

        JButton createBtn = styleBtn("Create New Bill");
        createBtn.addActionListener(e -> showCreateBillForm());
        
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBackground(Color.WHITE);
        top.add(createBtn);
        adminPanel.add(top, BorderLayout.NORTH);

        String[] cols = {"ID", "User", "Units", "Total", "Due", "Status", "Action"};
        List<ElectricityManager.Bill> all = ElectricityManager.getAllBills();
        Object[][] data = new Object[all.size()][7];
        for (int i = 0; i < all.size(); i++) {
            ElectricityManager.Bill b = all.get(i);
            data[i] = new Object[]{b.id, b.username, b.units, b.totalAmount, b.dueDate, b.status, "Delete"};
        }

        JTable table = new JTable(data, cols);
        table.setRowHeight(30);
        adminPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton back = styleBtn("Back to My Bills");
        back.addActionListener(e -> initMainView());
        adminPanel.add(back, BorderLayout.SOUTH);

        contentArea.removeAll();
        contentArea.add(adminPanel, BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
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

        form.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        JTextField userF = new JTextField(15);
        form.add(userF, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Units Consumed:"), gbc);
        gbc.gridx = 1;
        JTextField unitsF = new JTextField(15);
        form.add(unitsF, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        form.add(new JLabel("Due Date:"), gbc);
        gbc.gridx = 1;
        JTextField dueF = new JTextField(java.time.LocalDate.now().plusDays(15).toString(), 15);
        form.add(dueF, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 5, 5, 5);
        JButton submit = styleBtn("Generate Bill");
        submit.addActionListener(e -> {
            try {
                double units = Double.parseDouble(unitsF.getText());
                if (ElectricityManager.createBill(userF.getText(), units, dueF.getText(), SessionManager.getCurrentUser())) {
                    JOptionPane.showMessageDialog(this, "Bill Generated Successfully.");
                    showAdminView();
                }
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid input. Please check fields."); }
        });
        form.add(submit, gbc);

        gbc.gridy = 4;
        JButton back = styleBtn("Cancel");
        back.addActionListener(e -> showAdminView());
        form.add(back, gbc);

        wrapper.add(form);
        contentArea.removeAll();
        contentArea.add(wrapper, BorderLayout.CENTER);
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
