package gui;

import core.ThemeManager;
import core.DashboardSettings;
import database.DatabaseManager;
import javax.swing.*;
import java.awt.*;

public class SettingsPanel extends JPanel {

    private JComboBox<String> providerBox;
    private JTextField groqKeyField;

    public SettingsPanel(DashboardFrame frame) {
        setLayout(new BorderLayout());
        setBackground(ThemeManager.getHomeBackground());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JButton backBtn = styleBtn("← Back");
        backBtn.addActionListener(e -> frame.switchPanel(new HomePanel(frame)));
        header.add(backBtn, BorderLayout.WEST);
        
        JLabel title = new JLabel("System Settings", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(ThemeManager.getTextColor("home"));
        header.add(title, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 14));


        JPanel appearanceTab = new JPanel();
        appearanceTab.setLayout(new BoxLayout(appearanceTab, BoxLayout.Y_AXIS));
        appearanceTab.setBackground(Color.WHITE);
        appearanceTab.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        appearanceTab.add(createOptionRow("Global Theme", createThemeBox(frame)));
        appearanceTab.add(Box.createVerticalStrut(15));
        appearanceTab.add(createOptionRow("Sidebar Style", createSidebarBox(frame)));
        appearanceTab.add(Box.createVerticalStrut(15));
        appearanceTab.add(createOptionRow("Homepage Style", createHomeBox(frame)));
        tabs.addTab("Appearance", appearanceTab);


        JPanel aiTab = new JPanel();
        aiTab.setLayout(new BoxLayout(aiTab, BoxLayout.Y_AXIS));
        aiTab.setBackground(Color.WHITE);
        aiTab.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        String[] settings = DatabaseManager.getAISettings();

        providerBox = new JComboBox<>(new String[]{"OLLAMA", "GROQ"});
        providerBox.setSelectedItem(settings[0]);
        aiTab.add(createOptionRow("AI Provider", providerBox));
        aiTab.add(Box.createVerticalStrut(15));

        groqKeyField = new JTextField(settings[1], 20);
        groqKeyField.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        aiTab.add(createOptionRow("Groq API Key", groqKeyField));
        aiTab.add(Box.createVerticalStrut(20));

        JButton saveAI = styleBtn("Save AI Configuration");
        saveAI.addActionListener(e -> {
            DatabaseManager.setAISettings((String)providerBox.getSelectedItem(), groqKeyField.getText());
            JOptionPane.showMessageDialog(this, "AI Settings Saved.");
        });
        JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        savePanel.setBackground(Color.WHITE);
        savePanel.add(saveAI);
        aiTab.add(savePanel);

        tabs.addTab("AI Configuration", aiTab);


        JPanel securityTab = new JPanel();
        securityTab.setLayout(new BoxLayout(securityTab, BoxLayout.Y_AXIS));
        securityTab.setBackground(Color.WHITE);
        securityTab.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        securityTab.add(createOptionRow("Account Password", createPasswordBtn(frame)));
        tabs.addTab("Security", securityTab);

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel createOptionRow(String label, Component comp) {
        JPanel p = new JPanel(new BorderLayout(20, 0));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK, 1),
            BorderFactory.createEmptyBorder(12, 20, 12, 20)
        ));
        p.setPreferredSize(new Dimension(750, 65));
        p.setMaximumSize(new Dimension(750, 65));
        
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        lbl.setForeground(Color.BLACK);
        
        if (comp instanceof JComboBox) {
            JComboBox<?> cb = (JComboBox<?>) comp;
            cb.setBackground(Color.WHITE);
            cb.setForeground(Color.BLACK);
            cb.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        }

        p.add(lbl, BorderLayout.WEST);
        p.add(comp, BorderLayout.EAST);
        return p;
    }

    private JComboBox<ThemeManager.ThemeMode> createThemeBox(DashboardFrame frame) {
        JComboBox<ThemeManager.ThemeMode> cb = new JComboBox<>(ThemeManager.ThemeMode.values());
        cb.setSelectedItem(ThemeManager.getMode());
        cb.addActionListener(e -> {
            ThemeManager.setMode((ThemeManager.ThemeMode) cb.getSelectedItem());
            ThemeManager.applyTheme(frame);
        });
        return cb;
    }

    private JComboBox<String> createSidebarBox(DashboardFrame frame) {
        JComboBox<String> cb = new JComboBox<>(new String[]{"DARK", "LIGHT"});
        cb.setSelectedItem(ThemeManager.getSidebarColor());
        cb.addActionListener(e -> {
            ThemeManager.setSidebarColor((String) cb.getSelectedItem());
            ThemeManager.applyTheme(frame);
        });
        return cb;
    }

    private JComboBox<String> createHomeBox(DashboardFrame frame) {
        JComboBox<String> cb = new JComboBox<>(new String[]{"DARK", "LIGHT"});
        cb.setSelectedItem(ThemeManager.getHomeColor());
        cb.addActionListener(e -> {
            ThemeManager.setHomeColor((String) cb.getSelectedItem());
            ThemeManager.applyTheme(frame);
        });
        return cb;
    }

    private JButton createPasswordBtn(DashboardFrame frame) {
        JButton btn = styleBtn("Change My Password");
        btn.addActionListener(e -> frame.switchPanel(new ChangePasswordPanel(frame)));
        return btn;
    }

    private JButton styleBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(Color.BLACK);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        return b;
    }
}
