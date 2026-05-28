package gui;

import core.SessionManager;
import core.UserRole;

import javax.swing.*;
import java.awt.*;

public class SidebarPanel extends JPanel {

    public SidebarPanel(DashboardFrame frame) {
        setPreferredSize(new Dimension(260, 0));
        updateColors();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));

        addProfileSection();
        add(Box.createVerticalStrut(25));

        addSidebarBtn("Settings", e -> frame.switchPanel(new SettingsPanel(frame)));
        addNotificationButton(frame);
        addSidebarBtn("Theme Toggle", e -> {
            core.ThemeManager.toggleTheme(frame);
            updateColors();
        });
        addLogoutButton(frame);

        if (SessionManager.getCurrentRole() == UserRole.OWNER ||
            SessionManager.getCurrentRole() == UserRole.ADMIN) {
            add(Box.createVerticalStrut(30));
            addOwnerSection(frame);
        }
    }

    private void updateColors() {
        setBackground(core.ThemeManager.getSidebarBackground());
        for (Component c : getComponents()) {
            if (c instanceof JLabel) {
                c.setForeground(core.ThemeManager.getSidebarForeground());
            } else if (c instanceof JButton) {
                c.setBackground(core.ThemeManager.getSidebarButtonColor());
                c.setForeground(core.ThemeManager.getSidebarButtonText());
            }
        }
    }

    private void addProfileSection() {
        JLabel title = new JLabel("SMART EDUVERSE");
        title.setForeground(core.ThemeManager.getSidebarForeground());
        title.setFont(new Font("SansSerif", Font.BOLD, 18));

        JLabel user = new JLabel(SessionManager.getCurrentUser());
        user.setForeground(core.ThemeManager.getSidebarForeground());
        user.setFont(new Font("SansSerif", Font.BOLD, 20));

        JLabel role = new JLabel(SessionManager.getCurrentRole().name());
        role.setForeground(Color.GRAY);
        role.setFont(new Font("SansSerif", Font.PLAIN, 14));

        add(title);
        add(Box.createVerticalStrut(20));
        add(user);
        add(role);
    }

    private void addNotificationButton(DashboardFrame frame) {
        int unread = core.NotificationManager.getUnreadCount(SessionManager.getCurrentUser());
        String text = "Notifications" + (unread > 0 ? " (" + unread + ")" : "");
        JButton btn = styleButton(text);
        if (unread > 0) btn.setForeground(new Color(200, 200, 255));

        btn.addActionListener(e -> {
            frame.switchPanel(new NotificationsPanel(frame));
        });


        for (int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            if (c instanceof JButton && ((JButton) c).getText().startsWith("Notifications")) {
                int pos = i;
                remove(i);
                add(btn, pos);
                revalidate(); repaint();
                return;
            }
        }
        add(btn);
        add(Box.createVerticalStrut(10));
    }

    private void addSidebarBtn(String text, java.awt.event.ActionListener l) {
        JButton b = styleButton(text);
        b.addActionListener(l);
        add(b);
        add(Box.createVerticalStrut(10));
    }

    private void addOwnerSection(DashboardFrame frame) {
        UserRole role = SessionManager.getCurrentRole();
        JLabel label = new JLabel(role == UserRole.OWNER ? "OWNER TOOLS" : "ADMIN TOOLS");
        label.setForeground(Color.GRAY);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));

        add(label);
        add(Box.createVerticalStrut(10));

        addSidebarBtn("Debug Dashboard", e -> new DebugDashboardDialog(frame).setVisible(true));
        addSidebarBtn("Role Manager", e -> new RoleManagerDialog(frame).setVisible(true));

        if (role == UserRole.OWNER) {
            addSidebarBtn("Module Manager", e -> frame.switchPanel(new ModuleManagerPanel(frame)));
            addSidebarBtn("System Logs", e -> frame.switchPanel(new LogsViewerPanel(frame)));
        }
    }

    private void addLogoutButton(DashboardFrame frame) {
        JButton btn = styleButton("Logout");
        btn.addActionListener(e -> {
            frame.dispose();
            new LoginFrame().setVisible(true);
        });
        add(btn);
        add(Box.createVerticalStrut(10));
    }

    private JButton styleButton(String text) {
        JButton button = new JButton(text);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        button.setBackground(core.ThemeManager.getSidebarButtonColor());
        button.setForeground(core.ThemeManager.getSidebarButtonText());
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { 
                if (core.ThemeManager.getSidebarColor().equals("DARK")) {
                    button.setBackground(new Color(60, 60, 60));
                } else {
                    button.setBackground(new Color(230, 230, 230)); 
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) { 
                button.setBackground(core.ThemeManager.getSidebarButtonColor()); 
            }
        });

        return button;
    }
}
