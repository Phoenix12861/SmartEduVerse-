package gui;

import javax.swing.*;
import java.awt.*;

import gui.DashboardFrame;

public class OwnerControlPanel extends JPanel {

    private JPanel contentPanel;

    public OwnerControlPanel() {

        setLayout(new BorderLayout());
        setBackground(new Color(235,235,235));


        JPanel menuPanel = new JPanel();
        menuPanel.setPreferredSize(new Dimension(240,0));
        menuPanel.setBackground(Color.BLACK);
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("OWNER PANEL");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setBorder(BorderFactory.createEmptyBorder(30,20,30,20));

        menuPanel.add(title);

        JButton dashboardBtn = createMenuButton("Dashboard Settings");
        JButton usersBtn = createMenuButton("User Management");
        JButton hardwareBtn = createMenuButton("Hardware Controls");
        JButton moduleBtn = createMenuButton("Module Controls");
        JButton logsBtn = createMenuButton("System Logs");

        menuPanel.add(dashboardBtn);
        menuPanel.add(usersBtn);
        menuPanel.add(hardwareBtn);
        menuPanel.add(moduleBtn);
        menuPanel.add(logsBtn);


        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(new Color(240,240,240));

        showDashboardSettings();

        add(menuPanel, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);



        dashboardBtn.addActionListener(e ->
                showDashboardSettings());

        usersBtn.addActionListener(e ->
                showUserManagement());

        hardwareBtn.addActionListener(e ->
                showHardwareControls());

        moduleBtn.addActionListener(e ->
                showModuleControls());

        logsBtn.addActionListener(e ->
                showLogsPanel());
    }



    private void showDashboardSettings() {

        JPanel panel = createContentPanel("Dashboard Settings");

        panel.add(createSliderCard("Icon Size", 10, 40, 22));
        panel.add(createSliderCard("Card Width", 160, 350, 210));
        panel.add(createSliderCard("Card Height", 120, 300, 170));
        panel.add(createSliderCard("Grid Gap", 5, 40, 18));

        JButton save = new JButton("Save Dashboard Settings");
        styleActionButton(save);

        panel.add(save);

        switchContent(panel);
    }



    private void showUserManagement() {

        JPanel panel = createContentPanel("User Management");

        JButton openRoleManager = new JButton("Open Role Manager");
        styleActionButton(openRoleManager);

        openRoleManager.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            new RoleManagerDialog(frame).setVisible(true);
        });

        panel.add(openRoleManager);

        switchContent(panel);
    }



    private void showHardwareControls() {

        JPanel panel = createContentPanel("Hardware Controls");

        JComboBox<String> ports =
                new JComboBox<>(new String[]{
                        "COM1",
                        "COM2",
                        "COM3",
                        "/dev/ttyUSB0",
                        "/dev/ttyACM0"
                });

        JButton testBtn = new JButton("Test Arduino");
        JButton rfidBtn = new JButton("Test RFID");

        styleActionButton(testBtn);
        styleActionButton(rfidBtn);

        panel.add(new JLabel("COM Port"));
        panel.add(ports);

        panel.add(testBtn);
        panel.add(rfidBtn);

        switchContent(panel);
    }



    private void showModuleControls() {

        JPanel panel = createContentPanel("Module Controls");

        String[] modules = {
                "Library",
                "Banking",
                "Hospital",
                "ATM",
                "Restaurant",
                "Courier",
                "Parking",
                "Train",
                "Typing",
                "Math Quiz",
                "AI Quiz"
        };

        for(String module : modules) {

            JCheckBox box = new JCheckBox(module, true);

            box.setBackground(Color.WHITE);
            box.setFont(new Font("SansSerif", Font.PLAIN, 16));

            panel.add(box);
        }

        switchContent(panel);
    }



    private void showLogsPanel() {

        JPanel panel = createContentPanel("System Logs");

        JTextArea logs = new JTextArea();

        logs.setText("""
System Started...
Database Connected...
Owner Logged In...
Modules Loaded...
Arduino Not Connected...
""");

        logs.setEditable(false);

        panel.setLayout(new BorderLayout());

        panel.add(
                new JScrollPane(logs),
                BorderLayout.CENTER
        );

        switchContent(panel);
    }



    private JPanel createContentPanel(String title) {

        JPanel panel = new JPanel();

        panel.setBackground(new Color(245,245,245));
        panel.setBorder(
                BorderFactory.createEmptyBorder(25,25,25,25)
        );

        panel.setLayout(new GridLayout(0,1,15,15));

        JLabel heading = new JLabel(title);

        heading.setFont(
                new Font("SansSerif", Font.BOLD, 26)
        );

        panel.add(heading);

        return panel;
    }



    private JPanel createSliderCard(
            String title,
            int min,
            int max,
            int value
    ) {

        JPanel card = new JPanel(new BorderLayout());

        card.setBackground(Color.WHITE);
        card.setBorder(
                BorderFactory.createEmptyBorder(15,15,15,15)
        );

        JLabel label = new JLabel(title);

        label.setFont(
                new Font("SansSerif", Font.BOLD, 15)
        );

        JSlider slider = new JSlider(min,max,value);

        card.add(label, BorderLayout.NORTH);
        card.add(slider, BorderLayout.CENTER);

        return card;
    }

    private JButton createMenuButton(String text) {

        JButton btn = new JButton(text);

        btn.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 50)
        );

        btn.setBackground(new Color(20,20,20));
        btn.setForeground(Color.WHITE);

        btn.setFocusPainted(false);

        btn.setHorizontalAlignment(SwingConstants.LEFT);

        btn.setBorder(
                BorderFactory.createEmptyBorder(10,20,10,20)
        );

        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return btn;
    }

    private void styleActionButton(JButton btn) {

        btn.setBackground(Color.BLACK);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void switchContent(JPanel panel) {

        contentPanel.removeAll();
        contentPanel.add(panel, BorderLayout.CENTER);

        contentPanel.revalidate();
        contentPanel.repaint();
    }
}
