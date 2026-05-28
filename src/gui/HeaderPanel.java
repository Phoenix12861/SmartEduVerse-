package gui;

import javax.swing.*;
import java.awt.*;

public class HeaderPanel extends JPanel {

    public HeaderPanel() {
        setPreferredSize(new Dimension(0, 75));
        setBackground(Color.BLACK);
        setLayout(new BorderLayout());

        JLabel title = new JLabel("  Smart EduVerse AI Suite");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));

        add(title, BorderLayout.WEST);
    }
}
