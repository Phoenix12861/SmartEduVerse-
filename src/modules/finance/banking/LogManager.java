package modules.finance.banking;

import database.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LogManager {





    public static void addLog(
            String actor,
            String action,
            String target,
            String details
    ) {

        try (
                Connection conn =
                        DatabaseManager.connect()
        ) {

            addLog(
                    conn,
                    actor,
                    action,
                    target,
                    details
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }





    public static void addLog(
            Connection conn,
            String actor,
            String action,
            String target,
            String details
    ) throws SQLException {

        try (
                PreparedStatement ps =
                        conn.prepareStatement(
                                """
                                INSERT INTO bank_logs
                                (
                                    actor,
                                    action,
                                    target,
                                    details
                                )
                                VALUES(?,?,?,?)
                                """
                        )
        ) {

            ps.setString(1, actor);
            ps.setString(2, action);
            ps.setString(3, target);
            ps.setString(4, details);

            ps.executeUpdate();
        }
    }





    public static boolean deleteLogs() {

        try (
                Connection conn =
                        DatabaseManager.connect();

                Statement stmt =
                        conn.createStatement()
        ) {

            stmt.executeUpdate(
                    "DELETE FROM bank_logs"
            );

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }





    public static List<String[]> getLogs() {

        List<String[]> logs =
                new ArrayList<>();

        try (
                Connection conn =
                        DatabaseManager.connect();

                Statement stmt =
                        conn.createStatement();

                ResultSet rs =
                        stmt.executeQuery(
                                """
                                SELECT *
                                FROM bank_logs
                                ORDER BY timestamp DESC
                                """
                        )
        ) {

            while (rs.next()) {

                logs.add(
                        new String[] {

                                rs.getString("actor"),

                                rs.getString("action"),

                                rs.getString("target"),

                                rs.getString("details"),

                                rs.getString("timestamp")
                        }
                );
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return logs;
    }





    public static int getLogCount() {

        try (
                Connection conn =
                        DatabaseManager.connect();

                Statement stmt =
                        conn.createStatement();

                ResultSet rs =
                        stmt.executeQuery(
                                """
                                SELECT COUNT(*)
                                FROM bank_logs
                                """
                        )
        ) {

            if (rs.next()) {

                return rs.getInt(1);
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return 0;
    }
}
