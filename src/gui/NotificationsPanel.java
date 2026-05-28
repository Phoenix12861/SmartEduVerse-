package gui;

import core.NotificationManager;
import core.SessionManager;
import core.ThemeManager;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class NotificationsPanel extends JPanel {

    public NotificationsPanel(DashboardFrame frame) {
        setLayout(new BorderLayout(15, 15));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JButton back = styleBtn("← Back");
        back.addActionListener(e -> frame.switchPanel(new HomePanel(frame)));
        header.add(back, BorderLayout.WEST);
        
        JLabel title = new JLabel("My Notifications", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        header.add(title, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Color.WHITE);
        
        loadNotifications(list);
        
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);
        container.add(list, BorderLayout.NORTH);
        
        add(new JScrollPane(container), BorderLayout.CENTER);

        JButton clear = styleBtn("Mark All as Read");
        clear.addActionListener(e -> {
            NotificationManager.markAllAsRead(SessionManager.getCurrentUser());
            loadNotifications(list);
        });
        add(clear, BorderLayout.SOUTH);
    }

    private void loadNotifications(JPanel list) {
        list.removeAll();
        List<NotificationManager.Notification> notes = NotificationManager.getNotifications(SessionManager.getCurrentUser());
        
        if (notes.isEmpty()) {
            JLabel empty = new JLabel("No notifications.", SwingConstants.CENTER);
            empty.setBorder(BorderFactory.createEmptyBorder(50, 0, 0, 0));
            list.add(empty);
        } else {
            for (NotificationManager.Notification n : notes) {
                JPanel card = new JPanel(new BorderLayout(15, 10));
                card.setBackground(n.isRead ? Color.WHITE : new Color(245, 245, 255));
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK),
                    BorderFactory.createEmptyBorder(15, 20, 15, 20)
                ));

                JLabel msg = new JLabel("<html><b>" + n.type + "</b>: " + n.message + "<br><small>" + n.time + "</small></html>");
                msg.setFont(new Font("SansSerif", Font.PLAIN, 14));
                card.add(msg, BorderLayout.CENTER);
                list.add(card);
            }
        }
        list.revalidate(); list.repaint();
    }

    private JButton styleBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(Color.BLACK);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        return b;
    }
}
