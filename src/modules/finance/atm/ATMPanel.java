package modules.finance.atm;

import core.SessionManager;
import modules.finance.banking.BankAccountManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class ATMPanel extends JPanel {

    private final String username;
    private JLabel balanceLabel;
    private JPasswordField pinField;
    private JTextField amountField;

    public ATMPanel() {
        this.username = SessionManager.getCurrentUser();
        setLayout(new GridBagLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(40, 40, 40, 40));

        initUI();
    }

    private void initUI() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Color.BLACK, 2),
                new EmptyBorder(40, 50, 40, 50)
        ));


        JLabel logo = new JLabel("SMART ATM");
        logo.setFont(new Font("SansSerif", Font.BOLD, 42));
        logo.setForeground(Color.BLACK);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subHeader = new JLabel("SECURE WITHDRAWAL SYSTEM");
        subHeader.setFont(new Font("SansSerif", Font.BOLD, 12));
        subHeader.setForeground(Color.GRAY);
        subHeader.setAlignmentX(Component.CENTER_ALIGNMENT);


        JPanel balanceBox = new JPanel();
        balanceBox.setBackground(Color.BLACK);
        balanceBox.setMaximumSize(new Dimension(400, 80));
        balanceBox.setLayout(new GridBagLayout());
        
        balanceLabel = new JLabel("BALANCE: ₹ " + BankAccountManager.formatAmount(BankAccountManager.getBalance(username)));
        balanceLabel.setForeground(Color.WHITE);
        balanceLabel.setFont(new Font("Monospaced", Font.BOLD, 24));
        balanceBox.add(balanceLabel);


        JPanel inputs = new JPanel(new GridLayout(2, 1, 0, 20));
        inputs.setOpaque(false);
        inputs.setMaximumSize(new Dimension(400, 150));

        JPanel pinSection = createInputSection("ENTER BANK PIN", pinField = new JPasswordField());
        pinField.setFont(new Font("SansSerif", Font.BOLD, 24));
        pinField.setHorizontalAlignment(JTextField.CENTER);
        
        JPanel amountSection = createInputSection("ENTER AMOUNT", amountField = new JTextField());
        amountField.setFont(new Font("SansSerif", Font.BOLD, 24));
        amountField.setHorizontalAlignment(JTextField.CENTER);

        inputs.add(pinSection);
        inputs.add(amountSection);


        JPanel quickOptions = new JPanel(new GridLayout(1, 3, 10, 0));
        quickOptions.setOpaque(false);
        quickOptions.setMaximumSize(new Dimension(400, 50));
        
        quickOptions.add(createQuickBtn("₹ 500", "500"));
        quickOptions.add(createQuickBtn("₹ 1000", "1000"));
        quickOptions.add(createQuickBtn("₹ 5000", "5000"));


        JButton withdrawBtn = new JButton("WITHDRAW CASH");
        withdrawBtn.setBackground(Color.BLACK);
        withdrawBtn.setForeground(Color.WHITE);
        withdrawBtn.setFont(new Font("SansSerif", Font.BOLD, 18));
        withdrawBtn.setFocusPainted(false);
        withdrawBtn.setMaximumSize(new Dimension(400, 60));
        withdrawBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        withdrawBtn.addActionListener(e -> handleWithdraw());


        card.add(logo);
        card.add(Box.createVerticalStrut(5));
        card.add(subHeader);
        card.add(Box.createVerticalStrut(40));
        card.add(balanceBox);
        card.add(Box.createVerticalStrut(40));
        card.add(inputs);
        card.add(Box.createVerticalStrut(25));
        card.add(quickOptions);
        card.add(Box.createVerticalStrut(40));
        card.add(withdrawBtn);
        card.add(Box.createVerticalStrut(65));

        add(card, gbc);
    }

    private JPanel createInputSection(String title, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        
        JLabel l = new JLabel(title);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(Color.GRAY);
        
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 220, 220), 1),
                new EmptyBorder(5, 10, 5, 10)
        ));
        
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JButton createQuickBtn(String text, String amount) {
        JButton btn = new JButton(text);
        btn.setBackground(Color.WHITE);
        btn.setForeground(Color.BLACK);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setBorder(new LineBorder(Color.BLACK, 1));
        btn.setFocusPainted(false);
        btn.addActionListener(e -> amountField.setText(amount));
        return btn;
    }

    private void handleWithdraw() {
        String pin = new String(pinField.getPassword());
        String amountStr = amountField.getText();

        if (pin.isEmpty() || amountStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (ATMManager.withdraw(username, pin, amount)) {
                JOptionPane.showMessageDialog(this, "Withdrawal Successful! Please collect your cash.");
                refreshUI();
            } else {
                JOptionPane.showMessageDialog(this, "Withdrawal Failed. Check balance, status, or PIN.");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid amount.");
        } finally {
            pinField.setText("");
            amountField.setText("");
        }
    }

    private void refreshUI() {
        balanceLabel.setText("BALANCE: ₹ " + BankAccountManager.formatAmount(BankAccountManager.getBalance(username)));
    }
}
