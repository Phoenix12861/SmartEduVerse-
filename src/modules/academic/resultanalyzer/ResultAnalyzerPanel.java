package modules.academic.resultanalyzer;

import core.SessionManager;
import modules.academic.attendance.AttendanceManager;
import modules.academic.schoolmanagement.SchoolManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ResultAnalyzerPanel extends JPanel {

    public ResultAnalyzerPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        
        SchoolManager.StaffInfo staff = SchoolManager.getStaffInfo(SessionManager.getCurrentUser());
        if (staff != null) {
            showStaffManagement(staff);
        } else {
            showStudentReport();
        }
    }

    private void showStudentReport() {
        removeAll();
        String verifiedUser = SessionManager.getCurrentUser();
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Academic Performance Report: " + verifiedUser);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        p.add(title, BorderLayout.NORTH);

        String[] cols = {"Subject", "Marks Obtained", "Total Marks", "Percentage", "Grade"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        JTable table = new JTable(model);
        table.setRowHeight(30);
        
        List<ResultAnalyzer.MarkEntry> results = ResultAnalyzer.getResults(verifiedUser);
        double totalObtained = 0, totalMax = 0;
        
        for (ResultAnalyzer.MarkEntry me : results) {
            double perc = (me.marks / me.totalMarks) * 100;
            model.addRow(new Object[]{
                me.subject, me.marks, me.totalMarks, String.format("%.2f%%", perc), getGrade(perc)
            });
            totalObtained += me.marks;
            totalMax += me.totalMarks;
        }

        if (totalMax > 0) {
            double totalPerc = (totalObtained / totalMax) * 100;
            JLabel totalL = new JLabel("Aggregate: " + String.format("%.2f%%", totalPerc) + " (" + getGrade(totalPerc) + ")");
            totalL.setFont(new Font("SansSerif", Font.BOLD, 18));
            p.add(totalL, BorderLayout.SOUTH);
        }

        p.add(new JScrollPane(table), BorderLayout.CENTER);
        add(p, BorderLayout.CENTER);
        revalidate(); repaint();
    }

    private void showStaffManagement(SchoolManager.StaffInfo staff) {
        removeAll();
        JPanel p = new JPanel(new BorderLayout(10, 10)); p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        
        JLabel title = new JLabel("Result Management System");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(Color.BLACK);
        p.add(title, BorderLayout.NORTH);
        
        JPanel filters = new JPanel(); filters.setBackground(Color.WHITE);
        JComboBox<Integer> classC = styleCombo(new Integer[]{1,2,3,4,5,6,7,8,9,10});
        JComboBox<String> secC = styleCombo(new String[]{"A", "B", "C", "D"});
        JButton load = styleBtn("Load Students");
        filters.add(new JLabel("Class:")); filters.add(classC);
        filters.add(new JLabel("Section:")); filters.add(secC);
        filters.add(load);
        
        JPanel list = new JPanel(); list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS)); list.setBackground(Color.WHITE);
        load.addActionListener(e -> {
            list.removeAll();
            List<SchoolManager.StudentInfo> students = SchoolManager.getStudentsByClass(staff.schoolId, (int)classC.getSelectedItem(), (String)secC.getSelectedItem());
            for (SchoolManager.StudentInfo s : students) {
                JPanel row = new JPanel(new BorderLayout()); row.setBackground(Color.WHITE);
                row.setBorder(BorderFactory.createMatteBorder(0,0,1,0, Color.BLACK));
                JLabel nameLabel = new JLabel(s.username);
                nameLabel.setForeground(Color.BLACK);
                row.add(nameLabel, BorderLayout.WEST);
                
                JButton edit = styleBtn("Manage Marks");
                edit.addActionListener(ae -> showMarkEditor(s.username));
                row.add(edit, BorderLayout.EAST);
                list.add(row);
            }
            list.revalidate(); list.repaint();
        });
        
        p.add(filters, BorderLayout.NORTH);
        p.add(new JScrollPane(list), BorderLayout.CENTER);
        add(p, BorderLayout.CENTER);
        revalidate(); repaint();
    }

    private void showMarkEditor(String username) {
        removeAll();
        JPanel p = new JPanel(new BorderLayout(15, 15)); p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout()); header.setBackground(Color.WHITE);
        JButton back = styleBtn("← Back to Student List");
        back.addActionListener(e -> showStaffManagement(SchoolManager.getStaffInfo(SessionManager.getCurrentUser())));
        header.add(back, BorderLayout.WEST);
        
        JLabel title = new JLabel("Managing Marks for: " + username, SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(Color.BLACK);
        header.add(title, BorderLayout.CENTER);
        p.add(header, BorderLayout.NORTH);


        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10)); form.setBackground(new Color(250, 250, 250));
        form.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        JTextField subF = styleField("Subject Name", 15);
        JTextField marksF = styleField("Marks", 5);
        JTextField totalF = styleField("Total", 5);
        JButton addBtn = styleBtn("Save Marks");
        addBtn.addActionListener(e -> {
            try {
                if (ResultAnalyzer.setMarks(username, subF.getText(), Double.parseDouble(marksF.getText()), Double.parseDouble(totalF.getText()))) {
                    showMarkEditor(username);
                    core.NotificationManager.sendNotification(username, "Marks updated for " + subF.getText(), "ACADEMIC");
                }
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid input."); }
        });
        
        JLabel addLabel = new JLabel("Add New Subject:");
        addLabel.setForeground(Color.BLACK);
        form.add(addLabel); form.add(subF); form.add(marksF); form.add(totalF); form.add(addBtn);
        p.add(form, BorderLayout.SOUTH);


        String[] cols = {"Subject", "Marks Obtained", "Total Marks", "Percentage", "Actions"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(35);
        
        List<ResultAnalyzer.MarkEntry> results = ResultAnalyzer.getResults(username);
        double totalObtained = 0, totalMax = 0;
        for (ResultAnalyzer.MarkEntry me : results) {
            double perc = (me.marks / me.totalMarks) * 100;
            model.addRow(new Object[]{me.subject, me.marks, me.totalMarks, String.format("%.1f%%", perc), "Edit/Delete"});
            totalObtained += me.marks;
            totalMax += me.totalMarks;
        }

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    ResultAnalyzer.MarkEntry me = results.get(row);
                    int opt = JOptionPane.showOptionDialog(null, "Manage marks for " + me.subject, "Action",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[]{"Edit", "Delete", "Cancel"}, "Edit");
                    if (opt == 0) {
                        subF.setText(me.subject);
                        marksF.setText(String.valueOf(me.marks));
                        totalF.setText(String.valueOf(me.totalMarks));
                    } else if (opt == 1) {
                        if (ResultAnalyzer.deleteMarks(username, me.subject)) showMarkEditor(username);
                    }
                }
            }
        });

        JPanel centerPanel = new JPanel(new BorderLayout()); centerPanel.setBackground(Color.WHITE);
        centerPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        
        if (totalMax > 0) {
            double totalPerc = (totalObtained / totalMax) * 100;
            JLabel agg = new JLabel("Aggregate Score: " + String.format("%.1f%%", totalPerc) + " | Grade: " + getGrade(totalPerc));
            agg.setFont(new Font("SansSerif", Font.BOLD, 18));
            agg.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            centerPanel.add(agg, BorderLayout.SOUTH);
        }

        p.add(centerPanel, BorderLayout.CENTER);
        add(p, BorderLayout.CENTER);
        revalidate(); repaint();
    }

    private String getGrade(double p) {
        if (p >= 90) return "A+";
        if (p >= 80) return "A";
        if (p >= 70) return "B";
        if (p >= 60) return "C";
        if (p >= 50) return "D";
        return "F";
    }

    private JButton styleBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(Color.BLACK);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        return b;
    }

    private JTextField styleField(String text, int cols) {
        JTextField f = new JTextField(text, cols);
        f.setBackground(Color.WHITE);
        f.setForeground(Color.BLACK);
        f.setCaretColor(Color.BLACK);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        return f;
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
