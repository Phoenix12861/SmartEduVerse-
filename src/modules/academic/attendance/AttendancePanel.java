package modules.academic.attendance;

import core.SessionManager;
import modules.academic.schoolmanagement.SchoolManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class AttendancePanel extends JPanel {

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public AttendancePanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        
        SchoolManager.StaffInfo staff = SchoolManager.getStaffInfo(SessionManager.getCurrentUser());
        if (staff == null || staff.role.equals("STUDENT")) {
            add(new JLabel("Only teachers and principals can access this module.", SwingConstants.CENTER));
            return;
        }

        initUI(staff);
    }

    private void initUI(SchoolManager.StaffInfo staff) {
        JTabbedPane tabs = new JTabbedPane();
        
        JPanel daily = new JPanel(new BorderLayout(10, 10)); daily.setBackground(Color.WHITE);
        daily.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JPanel filters = new JPanel(); filters.setBackground(Color.WHITE);
        JComboBox<Integer> classC = styleCombo(new Integer[]{1,2,3,4,5,6,7,8,9,10});
        JComboBox<String> secC = styleCombo(new String[]{"A", "B", "C", "D"});
        JButton load = styleBtn("Load Class");
        filters.add(new JLabel("Class:")); filters.add(classC);
        filters.add(new JLabel("Section:")); filters.add(secC);
        filters.add(load);

        JPanel list = new JPanel(); list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS)); list.setBackground(Color.WHITE);
        load.addActionListener(e -> {
            Calendar today = Calendar.getInstance();
            if (today.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                JOptionPane.showMessageDialog(this, "Attendance cannot be marked on Sundays.");
                return;
            }
            
            list.removeAll();
            List<SchoolManager.StudentInfo> students = SchoolManager.getStudentsByClass(staff.schoolId, (int)classC.getSelectedItem(), (String)secC.getSelectedItem());
            for (SchoolManager.StudentInfo s : students) {
                JPanel row = new JPanel(new BorderLayout()); row.setBackground(Color.WHITE);
                row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
                row.setPreferredSize(new Dimension(0, 50));
                
                JLabel name = new JLabel("  " + s.username);
                name.setFont(new Font("SansSerif", Font.PLAIN, 16));
                name.setForeground(Color.BLACK);
                row.add(name, BorderLayout.WEST);
                
                JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT)); actions.setBackground(Color.WHITE);
                JButton pres = styleBtn("P"); 
                pres.setPreferredSize(new Dimension(45, 30));
                pres.addActionListener(ae -> {
                    AttendanceManager.takeAttendance(s.username, sdf.format(new Date()), "PRESENT");
                    pres.setEnabled(false);
                });
                JButton abs = styleBtn("A");
                abs.setPreferredSize(new Dimension(45, 30));
                abs.addActionListener(ae -> {
                    AttendanceManager.takeAttendance(s.username, sdf.format(new Date()), "ABSENT");
                    abs.setEnabled(false);
                });
                actions.add(pres); actions.add(abs);
                row.add(actions, BorderLayout.EAST);
                list.add(row);
            }
            list.revalidate(); list.repaint();
        });

        daily.add(filters, BorderLayout.NORTH);
        daily.add(new JScrollPane(list), BorderLayout.CENTER);
        

        JPanel calPanel = new JPanel(new BorderLayout()); calPanel.setBackground(Color.WHITE);
        JPanel grid = new JPanel(new GridLayout(0, 7)); grid.setBackground(Color.WHITE);
        
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);
        
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String d : days) {
            JLabel lbl = new JLabel(d, SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
            lbl.setForeground(Color.BLACK);
            grid.add(lbl);
        }
        
        int startDay = cal.get(Calendar.DAY_OF_WEEK);
        for (int i = 1; i < startDay; i++) grid.add(new JLabel(""));
        
        while (cal.get(Calendar.MONTH) == month) {
            int d = cal.get(Calendar.DAY_OF_MONTH);
            String dateStr = String.format("%d-%02d-%02d", year, month + 1, d);
            JButton dBtn = new JButton(String.valueOf(d));
            dBtn.setBackground(Color.WHITE); dBtn.setFocusPainted(false);
            dBtn.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) dBtn.setForeground(Color.RED);
            
            dBtn.addActionListener(e -> {
                showClassSelector(staff, dateStr);
            });
            
            grid.add(dBtn);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        JLabel monthLabel = new JLabel("  " + new SimpleDateFormat("MMMM yyyy").format(new Date()), SwingConstants.LEFT);
        monthLabel.setForeground(Color.BLACK);
        calPanel.add(monthLabel, BorderLayout.NORTH);
        calPanel.add(grid, BorderLayout.CENTER);

        tabs.addTab("Mark Daily", daily);
        tabs.addTab("Monthly View", calPanel);
        add(tabs, BorderLayout.CENTER);
    }

    private void showClassSelector(SchoolManager.StaffInfo staff, String dateStr) {
        JPanel p = new JPanel(new FlowLayout()); p.setBackground(Color.WHITE);
        JComboBox<Integer> classC = styleCombo(new Integer[]{1,2,3,4,5,6,7,8,9,10});
        JComboBox<String> secC = styleCombo(new String[]{"A", "B", "C", "D"});
        p.add(new JLabel("Select Class:")); p.add(classC);
        p.add(new JLabel("Section:")); p.add(secC);

        int opt = JOptionPane.showConfirmDialog(this, p, "View Attendance for " + dateStr, JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            showAttendanceEditor(staff, dateStr, (int)classC.getSelectedItem(), (String)secC.getSelectedItem());
        }
    }

    private void showAttendanceEditor(SchoolManager.StaffInfo staff, String dateStr, int classNum, String section) {
        JDialog dialog = new JDialog((JFrame)null, "Attendance: " + dateStr + " - Class " + classNum + section, true);
        dialog.setSize(500, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        
        JPanel list = new JPanel(); list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS)); list.setBackground(Color.WHITE);
        List<SchoolManager.StudentInfo> students = SchoolManager.getStudentsByClass(staff.schoolId, classNum, section);
        List<String[]> existing = AttendanceManager.getDailyAttendance(dateStr);
        
        for (SchoolManager.StudentInfo s : students) {
            JPanel row = new JPanel(new BorderLayout()); row.setBackground(Color.WHITE);
            row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
            row.add(new JLabel(" " + s.username), BorderLayout.WEST);
            
            String status = "ABSENT";
            for (String[] r : existing) {
                if (r[0].equals(s.username)) {
                    status = r[1];
                    break;
                }
            }
            
            JComboBox<String> statusCombo = styleCombo(new String[]{"PRESENT", "ABSENT"});
            statusCombo.setSelectedItem(status);
            statusCombo.addActionListener(e -> {
                AttendanceManager.takeAttendance(s.username, dateStr, (String)statusCombo.getSelectedItem());
            });
            row.add(statusCombo, BorderLayout.EAST);
            list.add(row);
        }
        
        dialog.add(new JScrollPane(list), BorderLayout.CENTER);
        JButton close = styleBtn("Close & Save");
        close.addActionListener(e -> dialog.dispose());
        dialog.add(close, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JButton styleBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(Color.BLACK);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        return b;
    }

    private <T> JComboBox<T> styleCombo(T[] items) {
        JComboBox<T> c = new JComboBox<>(items);
        c.setBackground(Color.WHITE);
        c.setForeground(Color.BLACK);
        c.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        c.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                comp.setBackground(isSelected ? Color.LIGHT_GRAY : Color.WHITE);
                comp.setForeground(Color.BLACK);
                return comp;
            }
        });
        return c;
    }
}
