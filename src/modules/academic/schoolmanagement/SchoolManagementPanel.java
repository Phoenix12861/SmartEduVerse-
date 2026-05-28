package modules.academic.schoolmanagement;

import core.SessionManager;
import core.UserRole;
import modules.finance.banking.BankAccountManager;
import modules.academic.attendance.AttendanceManager;
import modules.academic.resultanalyzer.ResultAnalyzer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class SchoolManagementPanel extends JPanel {

    private JPanel contentArea;
    private JPanel sidebar;
    private SchoolManager.StaffInfo currentStaff;
    private SchoolManager.StudentInfo currentStudent;

    public SchoolManagementPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(240, 240, 240));
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));

        contentArea = new JPanel(new BorderLayout());
        contentArea.setBackground(Color.WHITE);

        add(sidebar, BorderLayout.WEST);
        add(contentArea, BorderLayout.CENTER);

        initPanel();
    }

    private void initPanel() {
        sidebar.removeAll();
        String user = SessionManager.getCurrentUser();
        UserRole role = SessionManager.getCurrentRole();

        if (role == UserRole.OWNER) {
            initOwnerUI();
        } else {
            currentStaff = SchoolManager.getStaffInfo(user);
            if (currentStaff != null) {
                if (currentStaff.role.equals("PRINCIPAL")) initPrincipalUI();
                else initTeacherUI();
            } else {
                currentStudent = SchoolManager.getStudentInfo(user);
                if (currentStudent != null) {
                    initStudentUI();
                } else {
                    showApplyToSchool();
                }
            }
        }
    }

    private void showEmptyState(String msg) {
        contentArea.removeAll();
        JLabel l = new JLabel(msg, SwingConstants.CENTER);
        l.setFont(new Font("SansSerif", Font.ITALIC, 16));
        contentArea.add(l);
        contentArea.revalidate(); contentArea.repaint();
    }


    private void initOwnerUI() {
        addSidebarButton("Manage Schools", e -> showOwnerSchools());
        addSidebarButton("Appoint Staff", e -> showOwnerAppoint());
        addSidebarButton("Set Timetables", e -> showOwnerTimetable());
        addSidebarButton("Pay Salaries", e -> {
            if (SchoolManager.autoPaySalaries()) JOptionPane.showMessageDialog(this, "Salaries paid to all staff.");
        });
        showOwnerSchools();
    }

    private void showOwnerSchools() {
        JPanel p = createContentPanel("School Directory");
        List<String> schools = SchoolManager.getSchools();
        
        JPanel list = new JPanel();
        list.setLayout(new GridBagLayout());
        list.setBackground(Color.WHITE);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0; g.insets = new Insets(10, 0, 10, 0);

        for (String s : schools) {
            JButton b = styleBtn(s);
            b.setPreferredSize(new Dimension(500, 50));
            list.add(b, g);
            g.gridy++;
        }
        
        p.add(new JScrollPane(list), BorderLayout.CENTER);
        showInContent(p);
    }

    private void showOwnerAppoint() {
        JPanel p = createContentPanel("Appoint Principal/Teacher");
        JPanel f = new JPanel(new GridBagLayout()); f.setBackground(Color.WHITE);
        GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(10,10,10,10); g.fill = GridBagConstraints.HORIZONTAL;
        
        JComboBox<String> schoolC = styleCombo(SchoolManager.getSchools().toArray(new String[0]));
        JTextField userF = styleField(15);
        JComboBox<String> roleC = styleCombo(new String[]{"PRINCIPAL", "TEACHER"});
        JTextField salF = styleField("50000", 15);
        
        g.gridx=0; g.gridy=0; f.add(new JLabel("School:"), g);
        g.gridx=1; f.add(schoolC, g);
        g.gridx=0; g.gridy=1; f.add(new JLabel("Username:"), g);
        g.gridx=1; f.add(userF, g);
        g.gridx=0; g.gridy=2; f.add(new JLabel("Role:"), g);
        g.gridx=1; f.add(roleC, g);
        g.gridx=0; g.gridy=3; f.add(new JLabel("Salary (₹):"), g);
        g.gridx=1; f.add(salF, g);
        
        JButton sub = styleBtn("Appoint Staff");
        sub.addActionListener(e -> {
            int sid = SchoolManager.getSchoolId((String)schoolC.getSelectedItem());
            if (SchoolManager.appointStaff(sid, userF.getText(), (String)roleC.getSelectedItem(), Double.parseDouble(salF.getText()))) {
                JOptionPane.showMessageDialog(this, "Staff Appointed Successfully.");
            }
        });
        g.gridy=4; g.gridwidth=2; f.add(sub, g);
        p.add(f, BorderLayout.NORTH);
        showInContent(p);
    }

    private void showOwnerTimetable() {
        JPanel p = createContentPanel("Master Timetable Management");
        showInContent(p);
    }


    private void initPrincipalUI() {
        addSidebarButton("Admission Requests", e -> showPrincipalApplications());
        addSidebarButton("Add Student", e -> showPrincipalAddStudent());
        addSidebarButton("Student Management", e -> showPrincipalStudents());
        addSidebarButton("Transfer Requests", e -> showPrincipalTransfers());
        addSidebarButton("Staff Roster", e -> showPrincipalStaff());
        addSidebarButton("Appoint Teacher", e -> showPrincipalAppoint());
        showPrincipalStudents();
    }

    private void showPrincipalAppoint() {
        JPanel p = createContentPanel("Appoint New Teacher");
        JPanel f = new JPanel(new GridBagLayout()); f.setBackground(Color.WHITE);
        GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(10,10,10,10); g.fill = GridBagConstraints.HORIZONTAL;
        
        JTextField userF = styleField(15);
        JTextField salF = styleField("40000", 15);
        
        g.gridx=0; g.gridy=0; f.add(new JLabel("Teacher Username:"), g);
        g.gridx=1; f.add(userF, g);
        g.gridx=0; g.gridy=1; f.add(new JLabel("Monthly Salary (₹):"), g);
        g.gridx=1; f.add(salF, g);
        
        JButton sub = styleBtn("Appoint Teacher");
        sub.addActionListener(e -> {
            if (SchoolManager.appointStaff(currentStaff.schoolId, userF.getText(), "TEACHER", Double.parseDouble(salF.getText()))) {
                JOptionPane.showMessageDialog(this, "Teacher Appointed Successfully.");
            }
        });
        g.gridy=2; g.gridwidth=2; f.add(sub, g);
        p.add(f, BorderLayout.NORTH);
        showInContent(p);
    }

    private void showPrincipalApplications() {
        JPanel p = createContentPanel("Incoming Admission Requests");
        List<SchoolManager.StudentInfo> apps = SchoolManager.getPendingApplications(currentStaff.schoolId);
        
        JPanel list = new JPanel(); list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS)); list.setBackground(Color.WHITE);
        for (SchoolManager.StudentInfo s : apps) {
            JPanel row = new JPanel(new BorderLayout()); row.setBackground(Color.WHITE);
            row.setBorder(BorderFactory.createMatteBorder(0,0,1,0, Color.LIGHT_GRAY));
            row.add(new JLabel(s.username), BorderLayout.WEST);
            
            JButton appBtn = styleBtn("Review Application");
            appBtn.addActionListener(e -> {
                JTextField classF = styleField("1", 5);
                JTextField secF = styleField("A", 5);
                JTextField feeF = styleField("100000", 10);
                Object[] msg = {"Assign Class:", classF, "Assign Section:", secF, "Annual Fees:", feeF};
                int opt = JOptionPane.showConfirmDialog(this, msg, "Approve Admission", JOptionPane.OK_CANCEL_OPTION);
                if (opt == JOptionPane.OK_OPTION) {
                    if (SchoolManager.approveApplication(s.id, currentStaff.schoolId, s.username, Integer.parseInt(classF.getText()), secF.getText(), Double.parseDouble(feeF.getText()))) {
                        JOptionPane.showMessageDialog(this, "Student Admitted!");
                        showPrincipalApplications();
                    }
                }
            });
            row.add(appBtn, BorderLayout.EAST);
            list.add(row);
        }
        p.add(new JScrollPane(list), BorderLayout.CENTER);
        showInContent(p);
    }

    private void showPrincipalAddStudent() {
        JPanel p = createContentPanel("Enroll New Student");
        JPanel f = new JPanel(new GridBagLayout()); f.setBackground(Color.WHITE);
        GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(8,8,8,8); g.fill = GridBagConstraints.HORIZONTAL;

        JTextField userF = styleField(15);
        JComboBox<Integer> classC = styleCombo(new Integer[]{1,2,3,4,5,6,7,8,9,10});
        JComboBox<String> secC = styleCombo(new String[]{"A", "B", "C", "D"});
        JTextField feeF = styleField("100000", 15);

        g.gridx=0; g.gridy=0; f.add(new JLabel("Student Username:"), g);
        g.gridx=1; f.add(userF, g);
        g.gridx=0; g.gridy=1; f.add(new JLabel("Class:"), g);
        g.gridx=1; f.add(classC, g);
        g.gridx=0; g.gridy=2; f.add(new JLabel("Section:"), g);
        g.gridx=1; f.add(secC, g);
        g.gridx=0; g.gridy=3; f.add(new JLabel("Total Annual Fees (₹):"), g);
        g.gridx=1; f.add(feeF, g);

        JButton sub = styleBtn("Enroll in School");
        sub.addActionListener(e -> {
            String targetUser = userF.getText();
            if (SchoolManager.enrollStudent(currentStaff.schoolId, targetUser, (int)classC.getSelectedItem(), (String)secC.getSelectedItem(), Double.parseDouble(feeF.getText()))) {
                JOptionPane.showMessageDialog(this, targetUser + " enrolled successfully!");
                showPrincipalStudents();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to enroll. Ensure student exists and isn't already enrolled elsewhere.");
            }
        });

        g.gridy=4; g.gridwidth=2; f.add(sub, g);
        p.add(f, BorderLayout.NORTH);
        showInContent(p);
    }

    private void showPrincipalStudents() {
        JPanel p = createContentPanel("Manage Students");
        
        JPanel filters = new JPanel(); filters.setBackground(Color.WHITE);
        JComboBox<Integer> classC = styleCombo(new Integer[]{1,2,3,4,5,6,7,8,9,10});
        JComboBox<String> secC = styleCombo(new String[]{"A", "B", "C", "D"});
        JButton load = styleBtn("Load Students");
        filters.add(new JLabel("Class:")); filters.add(classC);
        filters.add(new JLabel("Section:")); filters.add(secC);
        filters.add(load);
        
        JPanel tablePanel = new JPanel(new BorderLayout()); tablePanel.setBackground(Color.WHITE);
        load.addActionListener(e -> {
            List<SchoolManager.StudentInfo> students = SchoolManager.getStudentsByClass(currentStaff.schoolId, (int)classC.getSelectedItem(), (String)secC.getSelectedItem());
            String[] cols = {"ID", "Username", "Status", "Fees Paid", "Action"};
            DefaultTableModel m = new DefaultTableModel(cols, 0);
            for (SchoolManager.StudentInfo s : students) {
                m.addRow(new Object[]{s.id, s.username, s.status, s.feesPaid, "Manage"});
            }
            JTable t = new JTable(m);
            t.setRowHeight(30);
            t.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent me) {
                    int row = t.getSelectedRow();
                    if (row >= 0) showStudentAdminActions(students.get(row));
                }
            });
            tablePanel.removeAll();
            tablePanel.add(new JScrollPane(t), BorderLayout.CENTER);
            tablePanel.revalidate(); tablePanel.repaint();
        });

        p.add(filters, BorderLayout.NORTH);
        p.add(tablePanel, BorderLayout.CENTER);
        showInContent(p);
    }

    private void showStudentAdminActions(SchoolManager.StudentInfo s) {
        JPanel p = createContentPanel("Admin Action: " + s.username);
        JPanel bBox = new JPanel(new GridLayout(0, 1, 10, 10)); bBox.setBackground(Color.WHITE);
        
        JButton susp = styleBtn("Suspend Student");
        susp.addActionListener(e -> {
            SchoolManager.updateStudentStatus(s.username, "SUSPENDED");
            JOptionPane.showMessageDialog(this, "Student Suspended.");
            showPrincipalStudents();
        });
        
        JButton unsusp = styleBtn("Re-activate Student");
        unsusp.addActionListener(e -> {
            SchoolManager.updateStudentStatus(s.username, "ACTIVE");
            JOptionPane.showMessageDialog(this, "Student Re-activated.");
            showPrincipalStudents();
        });

        JButton tc = styleBtn("Issue Transfer Certificate (TC)");
        tc.addActionListener(e -> {
            SchoolManager.updateStudentStatus(s.username, "TC_ISSUED");
            JOptionPane.showMessageDialog(this, "TC Issued. Student removed from active roster.");
            showPrincipalStudents();
        });

        JButton demote = styleBtn("Demote / Repeat Class");
        demote.addActionListener(e -> {
            SchoolManager.setStudentRepeat(s.username, true);
            JOptionPane.showMessageDialog(this, "Student marked for repeating current class.");
            showPrincipalStudents();
        });

        bBox.add(susp); bBox.add(unsusp); bBox.add(tc); bBox.add(demote);
        p.add(bBox, BorderLayout.NORTH);
        showInContent(p);
    }

    private void showPrincipalTransfers() {
        JPanel p = createContentPanel("Incoming Transfer Requests");
        showInContent(p);
    }

    private void showPrincipalStaff() {
        JPanel p = createContentPanel("Staff of this School");
        showInContent(p);
    }


    private void initTeacherUI() {
        addSidebarButton("My Timetable", e -> showTeacherTimetable());
        addSidebarButton("Take Attendance", e -> showTeacherAttendance());
        addSidebarButton("Homework & Detention", e -> showTeacherHomework());
        showTeacherTimetable();
    }

    private void showTeacherTimetable() {
        JPanel p = createContentPanel("My Daily Schedule");
        showInContent(p);
    }

    private void showTeacherAttendance() {
        JPanel p = createContentPanel("Mark Student Attendance");
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
            List<SchoolManager.StudentInfo> students = SchoolManager.getStudentsByClass(currentStaff.schoolId, (int)classC.getSelectedItem(), (String)secC.getSelectedItem());
            for (SchoolManager.StudentInfo s : students) {
                JPanel row = new JPanel(new BorderLayout()); row.setBackground(Color.WHITE);
                row.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
                row.add(new JLabel(s.username), BorderLayout.WEST);
                
                JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT)); actions.setBackground(Color.WHITE);
                JButton pres = styleBtn("Present"); pres.setBackground(new Color(0,100,0));
                pres.addActionListener(ae -> {
                    AttendanceManager.takeAttendance(s.username, new java.util.Date().toString(), "PRESENT");
                    pres.setEnabled(false);
                });
                JButton abs = styleBtn("Absent"); abs.setBackground(new Color(100,0,0));
                abs.addActionListener(ae -> {
                    AttendanceManager.takeAttendance(s.username, new java.util.Date().toString(), "ABSENT");
                    abs.setEnabled(false);
                });
                actions.add(pres); actions.add(abs);
                row.add(actions, BorderLayout.EAST);
                list.add(row);
            }
            list.revalidate(); list.repaint();
        });

        p.add(filters, BorderLayout.NORTH);
        p.add(new JScrollPane(list), BorderLayout.CENTER);
        showInContent(p);
    }

    private void showTeacherHomework() {
        JPanel p = createContentPanel("Homework & Discipline");
        JTabbedPane tabs = new JTabbedPane();
        
        JPanel hPanel = new JPanel(new GridBagLayout()); hPanel.setBackground(Color.WHITE);
        GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(5,5,5,5); g.fill = GridBagConstraints.HORIZONTAL;
        JComboBox<Integer> classC = styleCombo(new Integer[]{1,2,3,4,5,6,7,8,9,10});
        JComboBox<String> secC = styleCombo(new String[]{"A", "B", "C", "D"});
        JTextField subF = styleField(15);
        JTextArea contA = new JTextArea(5, 20);
        contA.setBackground(Color.BLACK); contA.setForeground(Color.WHITE);
        g.gridx=0; g.gridy=0; hPanel.add(new JLabel("Class:"), g);
        g.gridx=1; hPanel.add(classC, g);
        g.gridx=0; g.gridy=1; hPanel.add(new JLabel("Section:"), g);
        g.gridx=1; hPanel.add(secC, g);
        g.gridx=0; g.gridy=2; hPanel.add(new JLabel("Subject:"), g);
        g.gridx=1; hPanel.add(subF, g);
        g.gridx=0; g.gridy=3; hPanel.add(new JLabel("Content:"), g);
        g.gridx=1; hPanel.add(new JScrollPane(contA), g);
        JButton hSub = styleBtn("Assign Homework");
        hSub.addActionListener(e -> {
            if (SchoolManager.assignHomework(currentStaff.schoolId, (int)classC.getSelectedItem(), (String)secC.getSelectedItem(), subF.getText(), contA.getText()))
                JOptionPane.showMessageDialog(this, "Homework Assigned.");
        });
        g.gridy=4; g.gridwidth=2; hPanel.add(hSub, g);

        JPanel dPanel = new JPanel(new GridBagLayout()); dPanel.setBackground(Color.WHITE);
        JTextField stuF = styleField(15);
        JTextArea reasA = new JTextArea(3, 20);
        reasA.setBackground(Color.BLACK); reasA.setForeground(Color.WHITE);
        g.gridx=0; g.gridy=0; g.gridwidth=1; dPanel.add(new JLabel("Student Username:"), g);
        g.gridx=1; dPanel.add(stuF, g);
        g.gridx=0; g.gridy=1; dPanel.add(new JLabel("Reason:"), g);
        g.gridx=1; dPanel.add(new JScrollPane(reasA), g);
        JButton dSub = styleBtn("Issue Detention");
        dSub.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Detention issued to " + stuF.getText());
        });
        g.gridy=2; g.gridwidth=2; dPanel.add(dSub, g);

        tabs.addTab("Homework", hPanel);
        tabs.addTab("Detention", dPanel);
        p.add(tabs, BorderLayout.CENTER);
        showInContent(p);
    }


    private void initStudentUI() {
        if (currentStudent.status.equals("TC_ISSUED")) {
            showTCAlert();
            return;
        }
        addSidebarButton("My Timetable", e -> showStudentTimetable());
        addSidebarButton("Exam Schedule", e -> showStudentExams());
        addSidebarButton("Pay Fees", e -> showStudentFees());
        addSidebarButton("Academic Record", e -> showStudentRecord());
        addSidebarButton("Transfer School", e -> showStudentTransfer());
        showStudentTimetable();
    }

    private void showTCAlert() {
        contentArea.removeAll();
        JPanel p = new JPanel(new GridBagLayout()); p.setBackground(Color.WHITE);
        JLabel l = new JLabel("<html><div style='text-align:center;'><b>TC ISSUED</b><br>You have been issued a Transfer Certificate by the Principal.<br>You must switch to another school.</div></html>");
        l.setFont(new Font("SansSerif", Font.PLAIN, 18));
        JButton b = styleBtn("Apply for School Switch");
        b.addActionListener(e -> showStudentTransfer());
        GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(20,20,20,20);
        p.add(l, g); g.gridy=1; p.add(b, g);
        contentArea.add(p);
        contentArea.revalidate(); contentArea.repaint();
    }

    private void showStudentTimetable() {
        JPanel p = createContentPanel("Class Timetable - Class " + currentStudent.classNumber + currentStudent.section);
        showInContent(p);
    }

    private void showStudentExams() {
        JPanel p = createContentPanel("Upcoming Examinations");
        showInContent(p);
    }

    private void showStudentFees() {
        JPanel p = createContentPanel("Fee Management");
        JPanel info = new JPanel(new GridLayout(0, 1, 5, 5)); info.setBackground(Color.WHITE);
        info.add(new JLabel("Total Fees: ₹" + currentStudent.totalFees));
        info.add(new JLabel("Paid: ₹" + currentStudent.feesPaid));
        info.add(new JLabel("Balance: ₹" + (currentStudent.totalFees - currentStudent.feesPaid)));
        
        JTextField amtF = styleField(15);
        JButton pay = styleBtn("Pay Installment");
        pay.addActionListener(e -> {
            String pass = JOptionPane.showInputDialog(this, "Enter Bank Password:");
            double amt = Double.parseDouble(amtF.getText());
            if (BankAccountManager.transferMoney(currentStudent.username, "SYSTEM_SCHOOL", amt, pass)) {
                SchoolManager.payFees(currentStudent.username, amt);
                JOptionPane.showMessageDialog(this, "Payment Successful.");
                initPanel();
            }
        });
        p.add(info, BorderLayout.NORTH);
        JPanel f = new JPanel(); f.setBackground(Color.WHITE); f.add(new JLabel("Amount:")); f.add(amtF); f.add(pay);
        p.add(f, BorderLayout.CENTER);
        showInContent(p);
    }

    private void showStudentRecord() {
        JPanel p = createContentPanel("Academic Report Card");
        double att = AttendanceManager.getAttendancePercentage(currentStudent.username);
        p.add(new JLabel("Attendance: " + String.format("%.1f", att) + "%"), BorderLayout.NORTH);
        
        List<ResultAnalyzer.MarkEntry> marks = ResultAnalyzer.getResults(currentStudent.username);
        String[] cols = {"Subject", "Marks", "Total", "Percentage"};
        DefaultTableModel m = new DefaultTableModel(cols, 0);
        for (ResultAnalyzer.MarkEntry e : marks) {
            m.addRow(new Object[]{e.subject, e.marks, e.totalMarks, String.format("%.1f%%", (e.marks/e.totalMarks)*100)});
        }
        p.add(new JScrollPane(new JTable(m)), BorderLayout.CENTER);
        showInContent(p);
    }

    private void showStudentTransfer() {
        JPanel p = createContentPanel("Transfer Request");
        JComboBox<String> sC = styleCombo(SchoolManager.getSchools().toArray(new String[0]));
        JButton sub = styleBtn("Submit Request");
        sub.addActionListener(e -> {
            int toSid = SchoolManager.getSchoolId((String)sC.getSelectedItem());
            if (SchoolManager.requestTransfer(currentStudent.username, currentStudent.schoolId, toSid)) {
                JOptionPane.showMessageDialog(this, "Request submitted. Waiting for Principal's approval.");
            }
        });
        JPanel f = new JPanel(); f.setBackground(Color.WHITE); f.add(new JLabel("Target School:")); f.add(sC); f.add(sub);
        p.add(f, BorderLayout.NORTH);
        showInContent(p);
    }

    private void showApplyToSchool() {
        JPanel p = createContentPanel("Apply for Admission");
        List<String> schools = SchoolManager.getSchools();
        
        JPanel list = new JPanel(); list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS)); list.setBackground(Color.WHITE);
        for (String s : schools) {
            JPanel row = new JPanel(new BorderLayout()); row.setBackground(Color.WHITE);
            row.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
            row.add(new JLabel(s), BorderLayout.WEST);
            
            JButton app = styleBtn("Apply to Join");
            app.addActionListener(e -> {
                int sid = SchoolManager.getSchoolId(s);
                if (SchoolManager.applyToSchool(SessionManager.getCurrentUser(), sid)) {
                    JOptionPane.showMessageDialog(this, "Application submitted to " + s);
                    String principal = SchoolManager.getPrincipalUsername(sid);
                    if (principal != null) {
                        core.NotificationManager.sendNotification(principal, "New Admission Request from " + SessionManager.getCurrentUser(), "SCHOOL");
                    }
                }
            });
            row.add(app, BorderLayout.EAST);
            list.add(row);
        }
        p.add(new JScrollPane(list), BorderLayout.CENTER);
        showInContent(p);
    }


    private JPanel createContentPanel(String title) {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, 22));
        p.add(t, BorderLayout.NORTH);
        return p;
    }

    private void addSidebarButton(String text, java.awt.event.ActionListener l) {
        JButton b = new JButton(text);
        b.setMaximumSize(new Dimension(200, 40));
        b.setPreferredSize(new Dimension(200, 40));
        b.setFocusPainted(false);
        b.setBackground(new Color(240, 240, 240));
        b.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.addActionListener(l);
        sidebar.add(b);
    }

    private void showInContent(JPanel p) {
        contentArea.removeAll();
        p.setBackground(Color.WHITE);
        contentArea.add(p, BorderLayout.CENTER);
        contentArea.revalidate(); contentArea.repaint();
    }

    private JButton styleBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(Color.BLACK);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        return b;
    }

    private JTextField styleField(int cols) { return styleField("", cols); }
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
