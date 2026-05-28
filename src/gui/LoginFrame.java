package gui;

import core.*;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    public LoginFrame() {
        setTitle("Smart EduVerse Login");
        setSize(450, 350);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        getContentPane().setBackground(Color.WHITE);

        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(300, 220));
        panel.setBackground(Color.BLACK);
        panel.setLayout(new GridLayout(5, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();

        JButton login = new JButton("Login");
        login.setBackground(Color.WHITE);
        login.setForeground(Color.BLACK);

        panel.add(label("Username"));
        panel.add(username);
        panel.add(label("Password"));
        panel.add(password);
        panel.add(login);

        add(panel);

        login.addActionListener(e -> {
            String user = username.getText();
            String pass = new String(password.getPassword());

            if (UserManager.authenticate(user, pass)) {
                SessionManager.setCurrentUser(user);
                SessionManager.setCurrentRole(UserManager.getRole(user));

                dispose();
                new DashboardFrame().setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Credentials");
            }
        });
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        return l;
    }
}
