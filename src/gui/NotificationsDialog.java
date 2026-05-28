package gui;

import core.NotificationManager;
import core.SessionManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class NotificationsDialog extends JDialog {

    public NotificationsDialog(JFrame parent) {
        super(parent, "My Notifications", true);
        setSize(500, 600);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.BLACK);
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        JLabel title = new JLabel("Notifications");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        header.add(title, BorderLayout.WEST);

        JButton clear = new JButton("Mark All as Read");
        clear.setBackground(Color.DARK_GRAY);
        clear.setForeground(Color.WHITE);
        clear.addActionListener(e -> {
            NotificationManager.markAllAsRead(SessionManager.getCurrentUser());
            loadNotifications();
        });
        header.add(clear, BorderLayout.EAST);
        
        add(header, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Color.WHITE);
        
        loadNotifications(list);
        
        add(new JScrollPane(list), BorderLayout.CENTER);
    }

    private void loadNotifications(JPanel list) {
        list.removeAll();
        List<NotificationManager.Notification> notes = NotificationManager.getNotifications(SessionManager.getCurrentUser());
        
        if (notes.isEmpty()) {
            JLabel empty = new JLabel("No notifications yet.", SwingConstants.CENTER);
            empty.setBorder(BorderFactory.createEmptyBorder(50, 0, 0, 0));
            list.add(empty);
        } else {
            for (NotificationManager.Notification n : notes) {
                JPanel card = new JPanel(new BorderLayout(10, 5));
                card.setBackground(n.isRead ? Color.WHITE : new Color(245, 245, 255));
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(15, 20, 15, 20)
                ));
                
                JLabel type = new JLabel(n.type);
                type.setFont(new Font("SansSerif", Font.BOLD, 12));
                type.setForeground(Color.GRAY);
                
                JLabel msg = new JLabel("<html><body style='width: 300px;'>" + n.message + "</body></html>");
                msg.setFont(new Font("SansSerif", n.isRead ? Font.PLAIN : Font.BOLD, 14));
                
                JLabel time = new JLabel(n.time);
                time.setFont(new Font("SansSerif", Font.ITALIC, 11));
                time.setForeground(Color.LIGHT_GRAY);
                
                card.add(type, BorderLayout.NORTH);
                card.add(msg, BorderLayout.CENTER);
                card.add(time, BorderLayout.SOUTH);
                
                list.add(card);
            }
        }
        list.revalidate(); list.repaint();
    }

    private void loadNotifications() {
        // Find the list panel and refresh
        Container content = getContentPane();
        for (Component c : content.getComponents()) {
            if (c instanceof JScrollPane) {
                JPanel list = (JPanel) ((JScrollPane) c).getViewport().getView();
                loadNotifications(list);
                break;
            }
        }
    }
}
