package gui;

import database.DatabaseManager;
import core.SessionManager;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ModuleManagerPanel extends JPanel {

    private final String[] allModules = {
        "Library", "Banking", "Hospital", "ATM", "Restaurant", "Courier", "Parking", "Train",
        "Typing", "Math Quiz", "Guess Game", "Password", "Electricity", "Number System", "AI Quiz", "Diary",
        "School Mgmt", "Attendance", "Results", "Study Planner"
    };

    public ModuleManagerPanel(DashboardFrame frame) {
        setLayout(new BorderLayout(15, 15));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("System Module Manager");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        add(title, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setLayout(new GridLayout(0, 3, 20, 20));
        list.setBackground(Color.WHITE);

        for (String mod : allModules) {
            list.add(createModuleToggle(mod));
        }

        add(new JScrollPane(list), BorderLayout.CENTER);
        
        JButton back = new JButton("Back to Home");
        back.setBackground(Color.BLACK);
        back.setForeground(Color.WHITE);
        back.addActionListener(e -> frame.switchPanel(new HomePanel(frame)));
        add(back, BorderLayout.SOUTH);
    }

    private JPanel createModuleToggle(String name) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(245, 245, 245));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        JLabel lbl = new JLabel(name);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        p.add(lbl, BorderLayout.WEST);

        JToggleButton toggle = new JToggleButton(DatabaseManager.isModuleEnabled(name) ? "ENABLED" : "DISABLED");
        toggle.setSelected(DatabaseManager.isModuleEnabled(name));
        toggle.setBackground(toggle.isSelected() ? new Color(0, 120, 0) : new Color(150, 0, 0));
        toggle.setForeground(Color.WHITE);
        
        toggle.addActionListener(e -> {
            boolean enabled = toggle.isSelected();
            DatabaseManager.setModuleEnabled(name, enabled, SessionManager.getCurrentUser());
            toggle.setText(enabled ? "ENABLED" : "DISABLED");
            toggle.setBackground(enabled ? new Color(0, 120, 0) : new Color(150, 0, 0));
        });

        p.add(toggle, BorderLayout.EAST);
        return p;
    }
}
