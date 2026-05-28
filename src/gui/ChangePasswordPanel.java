package gui;

import core.SessionManager;
import core.UserManager;

import javax.swing.*;
import java.awt.*;

public class ChangePasswordPanel extends JPanel {

    public ChangePasswordPanel(DashboardFrame frame) {
        setLayout(new GridBagLayout());
        setBackground(Color.WHITE);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 10, 10, 10);
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel oldL = new JLabel("Current Password:");
        JPasswordField oldP = new JPasswordField(15);
        JLabel newL = new JLabel("New Password:");
        JPasswordField newP = new JPasswordField(15);
        JLabel conL = new JLabel("Confirm Password:");
        JPasswordField conP = new JPasswordField(15);

        JButton updateBtn = new JButton("UPDATE PASSWORD");
        updateBtn.setBackground(Color.BLACK);
        updateBtn.setForeground(Color.WHITE);
        updateBtn.setFocusPainted(false);

        g.gridx = 0; g.gridy = 0; add(oldL, g);
        g.gridx = 1; add(oldP, g);
        g.gridx = 0; g.gridy = 1; add(newL, g);
        g.gridx = 1; add(newP, g);
        g.gridx = 0; g.gridy = 2; add(conL, g);
        g.gridx = 1; add(conP, g);
        g.gridx = 0; g.gridy = 3; g.gridwidth = 2; add(updateBtn, g);

        updateBtn.addActionListener(e -> {
            String op = new String(oldP.getPassword());
            String np = new String(newP.getPassword());
            String cp = new String(conP.getPassword());

            if (op.isEmpty() || np.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Fields cannot be empty.");
                return;
            }
            if (!np.equals(cp)) {
                JOptionPane.showMessageDialog(this, "New passwords do not match.");
                return;
            }

            if (UserManager.authenticate(SessionManager.getCurrentUser(), op)) {
                UserManager.resetPassword(SessionManager.getCurrentUser(), np);
                JOptionPane.showMessageDialog(this, "Password updated successfully!");
                oldP.setText(""); newP.setText(""); conP.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "Incorrect current password.");
            }
        });
    }
}
