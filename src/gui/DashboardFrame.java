package gui;

import javax.swing.*;
import java.awt.*;

public class DashboardFrame extends JFrame {

    private JPanel contentPanel;

    public DashboardFrame() {
        setTitle("Smart EduVerse AI Suite");
        setSize(1400, 850);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        HeaderPanel header = new HeaderPanel();
        SidebarPanel sidebar = new SidebarPanel(this);


        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(core.ThemeManager.getHomeBackground());

        HomePanel home = new HomePanel(this);
        JScrollPane scrollPane = new JScrollPane(home);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(core.ThemeManager.getHomeBackground());
        scrollPane.getViewport().setBackground(core.ThemeManager.getHomeBackground());

        contentPanel.add(scrollPane, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        JScrollPane sidebarScroll =
        new JScrollPane(sidebar);

        sidebarScroll.setBorder(null);

        sidebarScroll.setHorizontalScrollBarPolicy(
                 JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
       );

        sidebarScroll.getVerticalScrollBar()
               .setUnitIncrement(16);

        sidebarScroll.setPreferredSize(
                new Dimension(260,0)
        );

        add(sidebarScroll, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
    }

    public void switchPanel(JPanel panel) {
        contentPanel.removeAll();
        contentPanel.setBackground(core.ThemeManager.getHomeBackground());

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(core.ThemeManager.getHomeBackground());
        scrollPane.getViewport().setBackground(core.ThemeManager.getHomeBackground());

        contentPanel.add(scrollPane, BorderLayout.CENTER);

        core.ThemeManager.applyTheme(contentPanel);
        contentPanel.revalidate();
        contentPanel.repaint();
    }
}
