package core;

import database.DatabaseManager;
import gui.LoginFrame;

public class AppLauncher {

    public void launch() {
        DatabaseManager.initialize();
        loadSettings();

        javax.swing.SwingUtilities.invokeLater(() ->
                new LoginFrame().setVisible(true)
        );
    }

    private void loadSettings() {
        String themeToSet = null;
        try (java.sql.Connection conn = DatabaseManager.connect();
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery("SELECT * FROM settings WHERE id = 1")) {
            if (rs.next()) {
                DashboardSettings.ICON_SIZE = rs.getInt("icon_size");
                DashboardSettings.CARD_WIDTH = rs.getInt("card_size");
                try {
                    DashboardSettings.GRID_GAP = rs.getInt("grid_gap");
                } catch (Exception e) {}
                
                themeToSet = rs.getString("theme");
            }
        } catch (Exception e) {
            System.err.println("Load Settings Error: " + e.getMessage());
        }

        if (themeToSet != null) {
            try {
                ThemeManager.setMode(ThemeManager.ThemeMode.valueOf(themeToSet), false);
            } catch (Exception e) {
                ThemeManager.setMode(ThemeManager.ThemeMode.DEFAULT, false);
            }
        }
    }
}
