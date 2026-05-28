package gui;

import modules.learning.typingspeed.TypingSpeedPanel;
import modules.utility.numbersystem.NumberSystemPanel;
import modules.finance.banking.BankingPanel;
import modules.finance.atm.ATMPanel;
import modules.finance.passwordchecker.PasswordCheckerPanel;
import modules.academic.diary.DiaryPanel;
import modules.management.restaurantbilling.RestaurantBillingPanel;
import modules.management.hospital.HospitalPanel;
import modules.management.parking.ParkingPanel;
import modules.management.trainreservation.TrainReservationPanel;
import modules.finance.electricitybill.ElectricityBillPanel;
import modules.management.couriertracking.CourierTrackingPanel;
import modules.academic.schoolmanagement.SchoolManagementPanel;
import modules.academic.attendance.AttendancePanel;
import modules.academic.resultanalyzer.ResultAnalyzerPanel;

import modules.management.library.LibraryPanel;
import modules.learning.onlinequiz.OnlineQuizPanel;
import modules.learning.mathquiz.MathQuizPanel;
import modules.academic.studyplanner.StudyPlannerPanel;
import modules.learning.guessnumber.GuessNumberPanel;

import javax.swing.*;
import java.awt.*;

public class ModulePage extends JPanel {

    private JPanel centerPanel;

    public ModulePage(
            DashboardFrame frame,
            String title
    ) {

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // ================= TOP BAR =================

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton back = new JButton("← Back");
        back.setFocusPainted(false);
        back.setBackground(Color.BLACK);
        back.setForeground(Color.WHITE);
        back.setPreferredSize(new Dimension(120,40));

        back.addActionListener(e -> {
            Component module = centerPanel.getComponent(0);
            boolean handled = false;
            try {
                // Use reflection to check for goBack() method in the module panel
                java.lang.reflect.Method goBack = module.getClass().getMethod("goBack");
                handled = (boolean) goBack.invoke(module);
            } catch (Exception ex) {
                // Method not found or failed, ignore
            }

            if (!handled) {
                frame.switchPanel(new HomePanel(frame));
            }
        });

        JLabel label = new JLabel(title);
        label.setFont(new Font("SansSerif", Font.BOLD, 26));
        label.setHorizontalAlignment(SwingConstants.CENTER);

        topBar.add(back, BorderLayout.WEST);
        topBar.add(label, BorderLayout.CENTER);

        add(topBar, BorderLayout.NORTH);

        // ================= MODULE LOADER =================

        centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Color.WHITE);

        switch (title) {
            case "Typing":
                centerPanel.add(new TypingSpeedPanel(), BorderLayout.CENTER);
                break;
            case "Number System":
                centerPanel.add(new NumberSystemPanel(), BorderLayout.CENTER);
                break;
            case "ATM":
                centerPanel.add(new ATMPanel(), BorderLayout.CENTER);
                break;
            case "Guess Game":
                centerPanel.add(new GuessNumberPanel(), BorderLayout.CENTER);
                break;
           case "Banking":
                centerPanel.add(new BankingPanel(), BorderLayout.CENTER);
                break;
            case "Password":
                centerPanel.add(new PasswordCheckerPanel(), BorderLayout.CENTER);
                break;
            case "Diary":
                centerPanel.add(new DiaryPanel(), BorderLayout.CENTER);
                break;
            case "Restaurant":
                centerPanel.add(new RestaurantBillingPanel(), BorderLayout.CENTER);
                break;
            case "Hospital":
                centerPanel.add(new HospitalPanel(), BorderLayout.CENTER);
                break;
            case "Parking":
                centerPanel.add(new ParkingPanel(), BorderLayout.CENTER);
                break;
            case "Train":
                centerPanel.add(new TrainReservationPanel(), BorderLayout.CENTER);
                break;
            case "Library":
                centerPanel.add(new LibraryPanel(), BorderLayout.CENTER);
                break;
            case "Math Quiz":
                centerPanel.add(new MathQuizPanel(), BorderLayout.CENTER);
                break;
            case "AI Quiz":
                centerPanel.add(new OnlineQuizPanel(), BorderLayout.CENTER);
                break;
            case "Study Planner":
                centerPanel.add(new StudyPlannerPanel(), BorderLayout.CENTER);
                break;
            case "Electricity":
                centerPanel.add(new ElectricityBillPanel(), BorderLayout.CENTER);
                break;
            case "Courier":
                centerPanel.add(new CourierTrackingPanel(), BorderLayout.CENTER);
                break;
            case "School Mgmt":
                centerPanel.add(new SchoolManagementPanel(), BorderLayout.CENTER);
                break;
            case "Attendance":
                centerPanel.add(new AttendancePanel(), BorderLayout.CENTER);
                break;
            case "Results":
                centerPanel.add(new ResultAnalyzerPanel(), BorderLayout.CENTER);
                break;
            default:
                JPanel comingSoon = new JPanel(new GridBagLayout());
                comingSoon.setBackground(Color.WHITE);
                JLabel text = new JLabel(title + " Module Coming Soon");
                text.setFont(new Font("SansSerif", Font.BOLD, 30));
                text.setForeground(new Color(90,90,90));
                comingSoon.add(text);
                centerPanel.add(comingSoon, BorderLayout.CENTER);
        }

        add(centerPanel, BorderLayout.CENTER);
    }

    private void showError(JPanel panel, String module, Throwable t) {
        panel.removeAll();
        JPanel errorPanel = new JPanel(new BorderLayout());
        errorPanel.setBackground(Color.WHITE);
        
        JLabel msg = new JLabel("Failed to load " + module + ": " + t.getMessage(), SwingConstants.CENTER);
        msg.setForeground(Color.RED);
        msg.setFont(new Font("SansSerif", Font.BOLD, 16));
        
        errorPanel.add(msg, BorderLayout.CENTER);
        panel.add(errorPanel);
        panel.revalidate();
        panel.repaint();
    }
}
