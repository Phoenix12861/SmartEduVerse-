package gui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import core.DashboardSettings;

public class DebugDashboardDialog extends JDialog {

    public DebugDashboardDialog(JFrame parent) {
        super(parent, "Dashboard Debug Settings", true);
        setSize(700, 520);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(15,15));

        getContentPane().setBackground(new Color(245,245,245));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBackground(new Color(245,245,245));
        top.setBorder(BorderFactory.createEmptyBorder(20,20,0,20));

        JLabel title = new JLabel("Dashboard Debug Settings");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        JLabel subtitle = new JLabel("Modify dashboard appearance and layout");
        subtitle.setForeground(Color.GRAY);

        top.add(title);
        top.add(Box.createVerticalStrut(5));
        top.add(subtitle);
        add(top, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(new Color(245,245,245));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        JSlider iconSlider = createSlider("Icon Size", 10, 300, clamp(DashboardSettings.ICON_SIZE, 10, 300), mainPanel);
        JSlider widthSlider = createSlider("Card Width", 100, 600, clamp(DashboardSettings.CARD_WIDTH, 100, 600), mainPanel);
        JSlider heightSlider = createSlider("Card Height", 100, 500, clamp(DashboardSettings.CARD_HEIGHT, 100, 500), mainPanel);
        JSlider gapSlider = createSlider("Grid Gap", 0, 100, clamp(DashboardSettings.GRID_GAP, 0, 100), mainPanel);

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(new Color(245,245,245));
        JButton save = new JButton("Save Settings");
        save.setBackground(Color.BLACK);
        save.setForeground(Color.WHITE);
        save.setFocusPainted(false);
        save.setPreferredSize(new Dimension(180,45));
        bottom.add(save);
        add(bottom, BorderLayout.SOUTH);

        save.addActionListener(e -> {
           DashboardSettings.ICON_SIZE = iconSlider.getValue();
           DashboardSettings.CARD_WIDTH = widthSlider.getValue();
           DashboardSettings.CARD_HEIGHT = heightSlider.getValue();
           DashboardSettings.GRID_GAP = gapSlider.getValue();

           try (java.sql.Connection conn = database.DatabaseManager.connect();
                java.sql.PreparedStatement ps = conn.prepareStatement("UPDATE settings SET icon_size=?, card_size=?, grid_gap=? WHERE id=1")) {
               ps.setInt(1, DashboardSettings.ICON_SIZE);
               ps.setInt(2, DashboardSettings.CARD_WIDTH);
               ps.setInt(3, DashboardSettings.GRID_GAP);
               ps.executeUpdate();
           } catch (Exception ex) { ex.printStackTrace(); }

           JOptionPane.showMessageDialog(this, "Settings Saved and Persisted Globally.");
           dispose();
           if (parent instanceof DashboardFrame) {
               ((DashboardFrame) parent).switchPanel(new HomePanel((DashboardFrame) parent));
           }
        });
    }

    private JPanel createSliderCard(String title, int min, int max, int value) {
        JPanel card = new JPanel(new BorderLayout(10,10));
        card.setBackground(Color.WHITE);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230,230,230)),
            BorderFactory.createEmptyBorder(15,15,15,15)
        ));

        JLabel label = new JLabel(title);
        label.setFont(new Font("SansSerif", Font.BOLD, 16));
        JSlider slider = new JSlider(min,max,value);
        slider.setBackground(Color.WHITE);

        JLabel valueLabel = new JLabel(value + "");
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        slider.addChangeListener(e -> valueLabel.setText(slider.getValue()+""));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.add(label, BorderLayout.WEST);
        top.add(valueLabel, BorderLayout.EAST);

        card.add(top, BorderLayout.NORTH);
        card.add(slider, BorderLayout.CENTER);
        return card;
    }

    private JSlider createSlider(String title, int min, int max, int value, JPanel parent) {
       JPanel card = createSliderCard(title, min, max, value);
       JSlider slider = null;
        for(Component c : card.getComponents()) {
           if(c instanceof JSlider s) { slider = s; }
       }
       parent.add(card);
       parent.add(Box.createVerticalStrut(15));
       return slider;
    }

    private int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}
