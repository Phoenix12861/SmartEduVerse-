package gui;

import core.SessionManager;
import core.UserRole;
import gui.components.ModuleCard;

import javax.swing.*;
import java.awt.*;

public class HomePanel extends JPanel {

    private static final String ICON_PATH = System.getProperty("user.dir") + "/resources/icons/";

    public HomePanel(DashboardFrame frame) {

        boolean isUser = SessionManager.getCurrentRole() == UserRole.USER;

        setLayout(new GridLayout(
                0,
                isUser ? 4 : 5,
                18,
                18
        ));

        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        setBackground(core.ThemeManager.getHomeBackground());

        if (database.DatabaseManager.isModuleEnabled("Library")) addModule(frame,"Library","library.png");
        if (database.DatabaseManager.isModuleEnabled("Banking")) addModule(frame,"Banking","finance.png");
        if (database.DatabaseManager.isModuleEnabled("Hospital")) addModule(frame,"Hospital","hospital.png");
        if (database.DatabaseManager.isModuleEnabled("ATM")) addModule(frame,"ATM","atm.png");

        if (database.DatabaseManager.isModuleEnabled("Restaurant")) addModule(frame,"Restaurant","restaurant.png");
        if (database.DatabaseManager.isModuleEnabled("Courier")) addModule(frame,"Courier","courier.png");
        if (database.DatabaseManager.isModuleEnabled("Parking")) addModule(frame,"Parking","parking.png");
        if (database.DatabaseManager.isModuleEnabled("Train")) addModule(frame,"Train","train.png");

        if (database.DatabaseManager.isModuleEnabled("Typing")) addModule(frame,"Typing","typing.png");
        if (database.DatabaseManager.isModuleEnabled("Math Quiz")) addModule(frame,"Math Quiz","math.png");
        if (database.DatabaseManager.isModuleEnabled("Guess Game")) addModule(frame,"Guess Game","guess.png");
        if (database.DatabaseManager.isModuleEnabled("Password")) addModule(frame,"Password","password.png");

        if (database.DatabaseManager.isModuleEnabled("Electricity")) addModule(frame,"Electricity","electricity.png");
        if (database.DatabaseManager.isModuleEnabled("Number System")) addModule(frame,"Number System","number.png");
        if (database.DatabaseManager.isModuleEnabled("AI Quiz")) addModule(frame,"AI Quiz","quiz.png");
        if (database.DatabaseManager.isModuleEnabled("Diary")) addModule(frame,"Diary","diary.png");

        if (!isUser) {
            if (database.DatabaseManager.isModuleEnabled("School Mgmt")) addModule(frame,"School Mgmt","academic.png");
            if (database.DatabaseManager.isModuleEnabled("Attendance")) addModule(frame,"Attendance","attendance.png");
            if (database.DatabaseManager.isModuleEnabled("Results")) addModule(frame,"Results","results.png");
            if (database.DatabaseManager.isModuleEnabled("Study Planner")) addModule(frame,"Study Planner","ai.png");
        }

        JPanel spacer = new JPanel();

            spacer.setOpaque(false);

            spacer.setPreferredSize(
            new Dimension(100, 120)
       );

       add(spacer);

    }

    private void addModule(DashboardFrame frame, String title, String icon) {
        add(new ModuleCard(
                title,
                ICON_PATH + icon,
                () -> frame.switchPanel(new ModulePage(frame, title))
        ));
    }
}
