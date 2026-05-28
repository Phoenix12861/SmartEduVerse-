package core;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ThemeManager {
    public enum ThemeMode { LIGHT, DARK, DEFAULT }
    
    private static ThemeMode currentMode = ThemeMode.DEFAULT;
    
    private static String sidebarStyle = "DARK"; // DARK or LIGHT
    private static String homeStyle = "LIGHT";   // DARK or LIGHT

    static {
        loadFromDB();
    }

    private static void loadFromDB() {
        String t = database.DatabaseManager.getThemeSetting("theme");
        String s = database.DatabaseManager.getThemeSetting("sidebar_style");
        String h = database.DatabaseManager.getThemeSetting("home_style");
        
        if (t != null) {
            try { currentMode = ThemeMode.valueOf(t); } catch (Exception e) {}
        }
        if (s != null) sidebarStyle = s;
        if (h != null) homeStyle = h;
    }
    
    public static void setMode(ThemeMode mode) {
        setMode(mode, true);
    }

    public static void setMode(ThemeMode mode, boolean save) {
        currentMode = mode;
        if (mode == ThemeMode.DARK) {
            sidebarStyle = "DARK";
            homeStyle = "DARK";
        } else if (mode == ThemeMode.LIGHT) {
            sidebarStyle = "LIGHT";
            homeStyle = "LIGHT";
        } else {
            sidebarStyle = "DARK";
            homeStyle = "LIGHT";
        }
        if (save) saveToDB();
    }

    public static void setSidebarColor(String color) { 
        sidebarStyle = color; 
        saveToDB();
    }
    public static void setHomeColor(String color) { 
        homeStyle = color; 
        saveToDB();
    }

    private static void saveToDB() {
        database.DatabaseManager.setThemeSetting("theme", currentMode.name());
        database.DatabaseManager.setThemeSetting("sidebar_style", sidebarStyle);
        database.DatabaseManager.setThemeSetting("home_style", homeStyle);
    }
    
    public static String getSidebarColor() { return sidebarStyle; }
    public static String getHomeColor() { return homeStyle; }
    public static ThemeMode getMode() { return currentMode; }

    public static Color getSidebarBackground() {
        return sidebarStyle.equals("DARK") ? Color.BLACK : Color.WHITE;
    }

    public static Color getSidebarForeground() {
        return sidebarStyle.equals("DARK") ? Color.WHITE : Color.BLACK;
    }

    public static Color getSidebarButtonColor() {
        if (sidebarStyle.equals("DARK")) return new Color(40, 40, 40);
        return new Color(245, 245, 245);
    }

    public static Color getSidebarButtonText() {
        return sidebarStyle.equals("DARK") ? Color.WHITE : Color.BLACK;
    }

    public static Color getHomeBackground() {
        return homeStyle.equals("DARK") ? new Color(60, 60, 60) : Color.WHITE;
    }

    public static Color getTextColor(String section) {
        if (section.equalsIgnoreCase("sidebar")) {
            return sidebarStyle.equals("DARK") ? Color.WHITE : Color.BLACK;
        } else {
            return homeStyle.equals("DARK") ? Color.WHITE : Color.BLACK;
        }
    }

    public static Color getCardBackground() {
        return homeStyle.equals("DARK") ? Color.BLACK : Color.WHITE;
    }

    public static void applyTheme(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof gui.SidebarPanel) {
                c.setBackground(getSidebarBackground());
                refreshTree(c, "sidebar");
            } else if (c instanceof gui.HomePanel) {
                c.setBackground(getHomeBackground());
                refreshTree(c, "home");
            } else if (c instanceof JScrollPane) {
                applyTheme((Container) c);
            } else if (c instanceof JPanel) {
                // Only color panels if they are specifically HomePanel or SidebarPanel
                // or containers inside them that don't have their own color logic
                applyTheme((Container) c);
            } else if (c instanceof Container) {
                applyTheme((Container) c);
            }
        }
        if (container instanceof JFrame) container.repaint();
    }

    private static void refreshTree(Component c, String section) {
        if (c instanceof JLabel) {
            c.setForeground(getTextColor(section));
        } else if (c instanceof JButton && section.equals("sidebar")) {
            c.setBackground(getSidebarButtonColor());
            c.setForeground(getSidebarButtonText());
        }
        
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                refreshTree(child, section);
            }
        }
    }
    
    public static void toggleTheme(JFrame frame) {
        if (currentMode == ThemeMode.DEFAULT) setMode(ThemeMode.DARK);
        else if (currentMode == ThemeMode.DARK) setMode(ThemeMode.LIGHT);
        else setMode(ThemeMode.DEFAULT);
        applyTheme(frame);
    }
}
