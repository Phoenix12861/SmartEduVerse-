package gui.components;

import javax.swing.*;
import java.awt.*;
import core.DashboardSettings;

public class ModuleCard extends JPanel {

    private boolean hovered = false;
    private boolean pressed = false;
    private final Runnable onClick;

    public ModuleCard(String title, String imagePath, Runnable onClick) {

        this.onClick = onClick;

        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setLayout(new BorderLayout());

        // Smaller cards
        setPreferredSize(
                new Dimension(
                        DashboardSettings.CARD_WIDTH,
                        DashboardSettings.CARD_HEIGHT
                )
        );

        JPanel innerPanel = new JPanel();
        innerPanel.setOpaque(false);

        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        innerPanel.setBorder(
                BorderFactory.createEmptyBorder(18,18,18,18)
        );

        JLabel iconLabel = new JLabel();
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        try {

            ImageIcon icon = new ImageIcon(imagePath);

            // Balanced icon size
        Image scaled = icon.getImage().getScaledInstance(
                DashboardSettings.ICON_SIZE,
                DashboardSettings.ICON_SIZE,
                Image.SCALE_SMOOTH
       );

            iconLabel.setIcon(new ImageIcon(scaled));

        } catch (Exception e) {
            iconLabel.setText("[No Icon]");
        }

        JLabel titleLabel = new JLabel(title);

        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(
                new Font("SansSerif", Font.BOLD, 15)
        );
        titleLabel.setForeground(core.ThemeManager.getTextColor("home"));

        innerPanel.add(Box.createVerticalGlue());
        innerPanel.add(iconLabel);
        innerPanel.add(Box.createVerticalStrut(14));
        innerPanel.add(titleLabel);
        innerPanel.add(Box.createVerticalGlue());

        add(innerPanel, BorderLayout.CENTER);

        addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseEntered(java.awt.event.MouseEvent evt) {
                hovered = true;
                repaint();
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                hovered = false;
                pressed = false;
                repaint();
            }

            public void mousePressed(java.awt.event.MouseEvent evt) {
                pressed = true;
                repaint();
            }

            public void mouseReleased(java.awt.event.MouseEvent evt) {

                pressed = false;
                repaint();

                if (contains(evt.getPoint()) && onClick != null) {
                    onClick.run();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        int offset = pressed ? 4 : (hovered ? 2 : 0);

        // Glow (Selective based on theme)
        if (hovered) {
            Color glowColor = (core.ThemeManager.getHomeColor().equals("DARK")) ? new Color(255, 255, 255, 30) : new Color(0, 0, 0, 20);
            g2.setColor(glowColor);
            g2.fillRoundRect(-2, -2 + offset, getWidth()-6, getHeight()-6, 28, 28);
        }

        // Main card box
        Color cardBg = core.ThemeManager.getCardBackground();
        g2.setColor(cardBg);

        g2.fillRoundRect(
                0,
                offset,
                getWidth()-12,
                getHeight()-12,
                26,
                26
        );

        // Border
        g2.setColor(core.ThemeManager.getHomeColor().equals("DARK") ? Color.DARK_GRAY : Color.BLACK);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(0, offset, getWidth()-12, getHeight()-12, 26, 26);

        g2.dispose();

        super.paintComponent(g);
    }
}
