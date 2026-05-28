package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:" + System.getProperty("user.dir") + "/data/smarteduverse.db";

    public static Connection connect() throws SQLException {
    try {
        Class.forName("org.sqlite.JDBC");
    } catch (ClassNotFoundException e) {
        throw new SQLException("SQLite JDBC Driver not found.", e);
    }

    Connection conn = DriverManager.getConnection(DB_URL);
    try (Statement stmt = conn.createStatement()) {
        stmt.execute("PRAGMA journal_mode=WAL;");
        stmt.execute("PRAGMA busy_timeout=5000;");
    } catch (SQLException e) { e.printStackTrace(); }
    return conn;
}
    public static void initialize() {
        System.out.println("Initializing Database...");
        try (
                Connection conn = connect();
                Statement stmt = conn.createStatement()
        ) {
            // ... (Tables creation remains the same)
            
            // USERS
            stmt.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE NOT NULL, password TEXT NOT NULL, role TEXT NOT NULL, is_library_banned INTEGER DEFAULT 0)");
            try { stmt.execute("ALTER TABLE users ADD COLUMN is_library_banned INTEGER DEFAULT 0"); } catch (Exception e) {}
            
            // SETTINGS
            stmt.execute("CREATE TABLE IF NOT EXISTS settings (id INTEGER PRIMARY KEY AUTOINCREMENT, icon_size INTEGER DEFAULT 30, card_size INTEGER DEFAULT 220, com_port TEXT DEFAULT 'COM3', theme TEXT DEFAULT 'DEFAULT', sidebar_style TEXT DEFAULT 'DARK', home_style TEXT DEFAULT 'LIGHT', grid_gap INTEGER DEFAULT 18, ai_provider TEXT DEFAULT 'OLLAMA', groq_key TEXT, last_train_reset TEXT)");
            
            try { stmt.execute("ALTER TABLE settings ADD COLUMN theme TEXT DEFAULT 'DEFAULT'"); } catch (Exception e) {}
            try { stmt.execute("ALTER TABLE settings ADD COLUMN sidebar_style TEXT DEFAULT 'DARK'"); } catch (Exception e) {}
            try { stmt.execute("ALTER TABLE settings ADD COLUMN home_style TEXT DEFAULT 'LIGHT'"); } catch (Exception e) {}
            try { stmt.execute("ALTER TABLE settings ADD COLUMN grid_gap INTEGER DEFAULT 18"); } catch (Exception e) {}
            try { stmt.execute("ALTER TABLE settings ADD COLUMN ai_provider TEXT DEFAULT 'OLLAMA'"); } catch (Exception e) {}
            try { stmt.execute("ALTER TABLE settings ADD COLUMN groq_key TEXT"); } catch (Exception e) {}
            try { stmt.execute("ALTER TABLE settings ADD COLUMN last_train_reset TEXT"); } catch (Exception e) {}

            stmt.execute("CREATE TABLE IF NOT EXISTS modules (id INTEGER PRIMARY KEY AUTOINCREMENT, module_name TEXT UNIQUE, enabled INTEGER DEFAULT 1, modified_by TEXT)");
            try { stmt.execute("ALTER TABLE modules ADD COLUMN modified_by TEXT"); } catch (Exception e) {}
            stmt.execute("CREATE TABLE IF NOT EXISTS admin_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, actor TEXT, action TEXT, details TEXT, timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // LIBRARY
            stmt.execute("CREATE TABLE IF NOT EXISTS books (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, author TEXT, category TEXT, quantity INTEGER DEFAULT 1, available_count INTEGER DEFAULT 1, price REAL DEFAULT 500)");
            try { stmt.execute("ALTER TABLE books ADD COLUMN price REAL DEFAULT 500"); } catch (Exception e) {}
            stmt.execute("CREATE TABLE IF NOT EXISTS library_records (id INTEGER PRIMARY KEY AUTOINCREMENT, book_id INTEGER, username TEXT, borrow_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, due_date TIMESTAMP, return_date TIMESTAMP, status TEXT DEFAULT 'BORROWED', fine_amount REAL DEFAULT 0, FOREIGN KEY(book_id) REFERENCES books(id), FOREIGN KEY(username) REFERENCES users(username))");
            try { stmt.execute("ALTER TABLE library_records ADD COLUMN due_date TIMESTAMP"); } catch (Exception e) {}
            try { stmt.execute("ALTER TABLE library_records ADD COLUMN fine_amount REAL DEFAULT 0"); } catch (Exception e) {}
            
            stmt.execute("CREATE TABLE IF NOT EXISTS library_bills (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, description TEXT, amount REAL, status TEXT DEFAULT 'UNPAID', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(username) REFERENCES users(username))");

            // ... (rest of tables)
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM settings")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute("INSERT INTO settings (icon_size) VALUES (30)");
                }
            }
            // BANK ACCOUNTS
            stmt.execute("CREATE TABLE IF NOT EXISTS bank_accounts (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE NOT NULL, bank_password TEXT NOT NULL, balance REAL DEFAULT 0, status TEXT DEFAULT 'PENDING', approved_by TEXT, terminated_by TEXT, terminated_at TIMESTAMP, rfid_uid TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(username) REFERENCES users(username))");

            // TRANSACTIONS
            stmt.execute("CREATE TABLE IF NOT EXISTS transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL, type TEXT NOT NULL, amount REAL NOT NULL, description TEXT, time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(username) REFERENCES users(username))");

            // DEPOSIT REQUESTS
            stmt.execute("CREATE TABLE IF NOT EXISTS deposit_requests (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL, amount REAL NOT NULL, status TEXT DEFAULT 'PENDING', requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, approved_by TEXT, FOREIGN KEY(username) REFERENCES users(username))");

            // RFID LINKS
            stmt.execute("CREATE TABLE IF NOT EXISTS rfid_cards (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE NOT NULL, uid TEXT UNIQUE NOT NULL, linked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(username) REFERENCES users(username))");

            // ATM LOGS
            stmt.execute("CREATE TABLE IF NOT EXISTS atm_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL, action TEXT NOT NULL, amount REAL, time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(username) REFERENCES users(username))");

            // BANK LOGS
            stmt.execute("CREATE TABLE IF NOT EXISTS bank_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, actor TEXT NOT NULL, action TEXT NOT NULL, target TEXT, details TEXT, timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // PASSWORD VAULT
            stmt.execute("CREATE TABLE IF NOT EXISTS vault (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL, app_name TEXT NOT NULL, app_password TEXT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(username) REFERENCES users(username))");

            // VAULT LOGS
            stmt.execute("CREATE TABLE IF NOT EXISTS vault_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, actor TEXT NOT NULL, action TEXT NOT NULL, target TEXT, details TEXT, timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // DIARY MODULE
            stmt.execute("CREATE TABLE IF NOT EXISTS diary (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL, title TEXT NOT NULL, content TEXT NOT NULL, is_locked INTEGER DEFAULT 0, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(username) REFERENCES users(username))");
            stmt.execute("CREATE TABLE IF NOT EXISTS diary_passwords (username TEXT PRIMARY KEY, password TEXT NOT NULL, FOREIGN KEY(username) REFERENCES users(username))");

            // RESTAURANT MODULE
            stmt.execute("CREATE TABLE IF NOT EXISTS restaurants (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS restaurant_tables (id INTEGER PRIMARY KEY AUTOINCREMENT, restaurant_id INTEGER, table_number INTEGER, is_occupied INTEGER DEFAULT 0, FOREIGN KEY(restaurant_id) REFERENCES restaurants(id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS restaurant_reservations (id INTEGER PRIMARY KEY AUTOINCREMENT, table_id INTEGER, username TEXT, date TEXT, time TEXT, status TEXT DEFAULT 'ACTIVE', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(table_id) REFERENCES restaurant_tables(id), FOREIGN KEY(username) REFERENCES users(username))");
            stmt.execute("CREATE TABLE IF NOT EXISTS restaurant_bills (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, restaurant_id INTEGER, table_number INTEGER, total_amount REAL DEFAULT 0, status TEXT DEFAULT 'PENDING', FOREIGN KEY(username) REFERENCES users(username), FOREIGN KEY(restaurant_id) REFERENCES restaurants(id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS restaurant_bill_items (id INTEGER PRIMARY KEY AUTOINCREMENT, bill_id INTEGER, item_name TEXT, price REAL, FOREIGN KEY(bill_id) REFERENCES restaurant_bills(id))");

            // HOSPITAL MODULE
            stmt.execute("CREATE TABLE IF NOT EXISTS hospitals (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS hospital_rooms (id INTEGER PRIMARY KEY AUTOINCREMENT, hospital_id INTEGER, room_number INTEGER, type TEXT, is_occupied INTEGER DEFAULT 0, FOREIGN KEY(hospital_id) REFERENCES hospitals(id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS hospital_bookings (id INTEGER PRIMARY KEY AUTOINCREMENT, room_id INTEGER, username TEXT, start_date TEXT, days INTEGER, status TEXT DEFAULT 'ACTIVE', FOREIGN KEY(room_id) REFERENCES hospital_rooms(id), FOREIGN KEY(username) REFERENCES users(username))");
            stmt.execute("CREATE TABLE IF NOT EXISTS hospital_bills (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, hospital_id INTEGER, room_number INTEGER, room_charge REAL DEFAULT 0, medical_charge REAL DEFAULT 0, total_amount REAL DEFAULT 0, status TEXT DEFAULT 'PENDING', FOREIGN KEY(username) REFERENCES users(username), FOREIGN KEY(hospital_id) REFERENCES hospitals(id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS hospital_bill_items (id INTEGER PRIMARY KEY AUTOINCREMENT, bill_id INTEGER, item_name TEXT, price REAL, FOREIGN KEY(bill_id) REFERENCES hospital_bills(id))");

            // PARKING MODULE
            stmt.execute("CREATE TABLE IF NOT EXISTS parking_lots (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE NOT NULL, type TEXT, reference_id INTEGER)");
            stmt.execute("CREATE TABLE IF NOT EXISTS parking_spots (id INTEGER PRIMARY KEY AUTOINCREMENT, lot_id INTEGER, spot_number INTEGER, is_occupied INTEGER DEFAULT 0, is_available INTEGER DEFAULT 1, FOREIGN KEY(lot_id) REFERENCES parking_lots(id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS parking_occupancy (id INTEGER PRIMARY KEY AUTOINCREMENT, spot_id INTEGER, username TEXT, occupied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, vacated_at TIMESTAMP, FOREIGN KEY(spot_id) REFERENCES parking_spots(id), FOREIGN KEY(username) REFERENCES users(username))");
            stmt.execute("CREATE TABLE IF NOT EXISTS parking_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, action TEXT, details TEXT, time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // TRAIN MODULE
            stmt.execute("CREATE TABLE IF NOT EXISTS stations (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS trains (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, from_station_id INTEGER, to_station_id INTEGER, departure_time TEXT, max_capacity INTEGER, current_occupancy INTEGER DEFAULT 0, price REAL, status TEXT DEFAULT 'SCHEDULED', FOREIGN KEY(from_station_id) REFERENCES stations(id), FOREIGN KEY(to_station_id) REFERENCES stations(id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS train_tickets (id INTEGER PRIMARY KEY AUTOINCREMENT, train_id INTEGER, username TEXT, seat_number INTEGER, status TEXT DEFAULT 'PAID', FOREIGN KEY(train_id) REFERENCES trains(id), FOREIGN KEY(username) REFERENCES users(username))");

            // ELECTRICITY MODULE
            stmt.execute("CREATE TABLE IF NOT EXISTS electricity_bills (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, units REAL, amount REAL, fine REAL DEFAULT 0, total_amount REAL, due_date TEXT, status TEXT DEFAULT 'PENDING', created_by TEXT, FOREIGN KEY(username) REFERENCES users(username))");

            // COURIER MODULE
            stmt.execute("CREATE TABLE IF NOT EXISTS couriers (id INTEGER PRIMARY KEY AUTOINCREMENT, sender_username TEXT, receiver_username TEXT, from_address TEXT, to_address TEXT, current_location TEXT, distance_km REAL, amount REAL, status TEXT DEFAULT 'PENDING', payment_status TEXT DEFAULT 'PENDING', route TEXT, estimated_time INTEGER DEFAULT 0, FOREIGN KEY(sender_username) REFERENCES users(username), FOREIGN KEY(receiver_username) REFERENCES users(username))");

            // SCHOOL MANAGEMENT MODULE
            stmt.execute("CREATE TABLE IF NOT EXISTS schools (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS school_staff (id INTEGER PRIMARY KEY AUTOINCREMENT, school_id INTEGER, username TEXT UNIQUE, role TEXT, salary REAL DEFAULT 0, last_salary_paid TEXT, FOREIGN KEY(school_id) REFERENCES schools(id), FOREIGN KEY(username) REFERENCES users(username))");
            stmt.execute("CREATE TABLE IF NOT EXISTS school_students (id INTEGER PRIMARY KEY AUTOINCREMENT, school_id INTEGER, username TEXT UNIQUE, class_number INTEGER, section TEXT, status TEXT DEFAULT 'ACTIVE', total_fees REAL DEFAULT 0, fees_paid REAL DEFAULT 0, repeat_class INTEGER DEFAULT 0, FOREIGN KEY(school_id) REFERENCES schools(id), FOREIGN KEY(username) REFERENCES users(username))");
            stmt.execute("CREATE TABLE IF NOT EXISTS school_timetables (id INTEGER PRIMARY KEY AUTOINCREMENT, school_id INTEGER, class_number INTEGER, section TEXT, day_of_week TEXT, period_number INTEGER, subject TEXT, teacher_username TEXT, FOREIGN KEY(school_id) REFERENCES schools(id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS school_exam_timetables (id INTEGER PRIMARY KEY AUTOINCREMENT, school_id INTEGER, class_number INTEGER, section TEXT, subject TEXT, exam_date TEXT, exam_time TEXT, FOREIGN KEY(school_id) REFERENCES schools(id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS school_homework (id INTEGER PRIMARY KEY AUTOINCREMENT, school_id INTEGER, class_number INTEGER, section TEXT, subject TEXT, content TEXT, assigned_date TEXT, FOREIGN KEY(school_id) REFERENCES schools(id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS school_detentions (id INTEGER PRIMARY KEY AUTOINCREMENT, student_username TEXT, teacher_username TEXT, reason TEXT, date TEXT, FOREIGN KEY(student_username) REFERENCES users(username))");
            stmt.execute("CREATE TABLE IF NOT EXISTS school_attendance (id INTEGER PRIMARY KEY AUTOINCREMENT, student_username TEXT, date TEXT, status TEXT, FOREIGN KEY(student_username) REFERENCES users(username))");
            stmt.execute("CREATE TABLE IF NOT EXISTS school_results (id INTEGER PRIMARY KEY AUTOINCREMENT, student_username TEXT, subject TEXT, marks REAL, total_marks REAL, FOREIGN KEY(student_username) REFERENCES users(username))");
            stmt.execute("CREATE TABLE IF NOT EXISTS school_transfer_requests (id INTEGER PRIMARY KEY AUTOINCREMENT, student_username TEXT, from_school_id INTEGER, to_school_id INTEGER, status TEXT DEFAULT 'PENDING', FOREIGN KEY(student_username) REFERENCES users(username))");
            
            // NEW TABLES FOR NOTIFICATIONS AND APPLICATIONS
            stmt.execute("CREATE TABLE IF NOT EXISTS notifications (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, message TEXT, type TEXT, is_read INTEGER DEFAULT 0, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(username) REFERENCES users(username))");
            stmt.execute("CREATE TABLE IF NOT EXISTS school_applications (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, school_id INTEGER, status TEXT DEFAULT 'PENDING', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(username) REFERENCES users(username), FOREIGN KEY(school_id) REFERENCES schools(id))");

            // Migration: Ensure status columns exist for older DBs
            try { stmt.execute("ALTER TABLE restaurant_reservations ADD COLUMN status TEXT DEFAULT 'ACTIVE'"); } catch (Exception e) {}
            try { stmt.execute("ALTER TABLE restaurant_bills ADD COLUMN table_number INTEGER"); } catch (Exception e) {}
            try { stmt.execute("ALTER TABLE hospital_bills ADD COLUMN room_number INTEGER"); } catch (Exception e) {}
            try { stmt.execute("ALTER TABLE couriers ADD COLUMN route TEXT"); } catch (Exception e) {}
            try { stmt.execute("ALTER TABLE couriers ADD COLUMN estimated_time INTEGER DEFAULT 0"); } catch (Exception e) {}

            ensureDefaultOwner(conn);
            initializeDefaultData(conn);
            System.out.println("Database Initialized Successfully.");

        } catch (SQLException e) {
            System.err.println("Database Initialization Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String[] getAISettings() {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT ai_provider, groq_key FROM settings WHERE id = 1")) {
            if (rs.next()) return new String[]{rs.getString("ai_provider"), rs.getString("groq_key")};
        } catch (SQLException e) { e.printStackTrace(); }
        return new String[]{"OLLAMA", null};
    }

    public static void setAISettings(String provider, String groqKey) {
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement("UPDATE settings SET ai_provider = ?, groq_key = ? WHERE id = 1")) {
            ps.setString(1, provider);
            ps.setString(2, groqKey);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static String getThemeSetting(String column) {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT " + column + " FROM settings WHERE id = 1")) {
            if (rs.next()) return rs.getString(column);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public static void setThemeSetting(String column, String value) {
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement("UPDATE settings SET " + column + " = ? WHERE id = 1")) {
            ps.setString(1, value);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void logAdminAction(String actor, String action, String details) {
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO admin_logs (actor, action, details) VALUES (?, ?, ?)")) {
            ps.setString(1, actor);
            ps.setString(2, action);
            ps.setString(3, details);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static boolean isModuleEnabled(String name) {
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement("SELECT enabled FROM modules WHERE module_name = ?")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) == 1;
        } catch (SQLException e) { e.printStackTrace(); }
        return true; // Default to enabled
    }

    public static void setModuleEnabled(String name, boolean enabled, String actor) {
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO modules (module_name, enabled, modified_by) VALUES (?, ?, ?)")) {
            ps.setString(1, name);
            ps.setInt(2, enabled ? 1 : 0);
            ps.setString(3, actor);
            ps.executeUpdate();
            logAdminAction(actor, "MODULE_TOGGLE", (enabled ? "Enabled " : "Disabled ") + name);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static List<String[]> getAdminLogs() {
        List<String[]> logs = new ArrayList<>();
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM admin_logs ORDER BY timestamp DESC")) {
            while (rs.next()) {
                logs.add(new String[]{rs.getString("actor"), rs.getString("action"), rs.getString("details"), rs.getString("timestamp")});
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return logs;
    }

    public static void purgeLogs(String actor) {
        if (!actor.equalsIgnoreCase("owner")) return; // Only default owner
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM admin_logs");
            logAdminAction(actor, "PURGE_LOGS", "All administrative logs cleared.");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private static void initializeDefaultData(Connection conn) throws SQLException {
        boolean originalAutoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);

            // 1. Restaurants
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM restaurants")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    System.out.println("Populating Restaurants...");
                    String[] defaultRestaurants = {"The Grand Buffet", "Spice Route", "Ocean Delights"};
                    for (String name : defaultRestaurants) {
                        try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO restaurants(name) VALUES(?)", Statement.RETURN_GENERATED_KEYS)) {
                            ps.setString(1, name);
                            ps.executeUpdate();
                            ResultSet rsg = ps.getGeneratedKeys();
                            if (rsg.next()) {
                                int restaurantId = rsg.getInt(1);
                                for (int i = 1; i <= 10; i++) {
                                    try (PreparedStatement psTable = conn.prepareStatement("INSERT INTO restaurant_tables(restaurant_id, table_number) VALUES(?, ?)")) {
                                        psTable.setInt(1, restaurantId);
                                        psTable.setInt(2, i);
                                        psTable.executeUpdate();
                                    }
                                }
                                try (PreparedStatement psLot = conn.prepareStatement("INSERT OR IGNORE INTO parking_lots(name, type, reference_id) VALUES(?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                                    psLot.setString(1, name + " Parking");
                                    psLot.setString(2, "RESTAURANT");
                                    psLot.setInt(3, restaurantId);
                                    psLot.executeUpdate();
                                    ResultSet rsLot = psLot.getGeneratedKeys();
                                    if (rsLot.next()) {
                                        int lotId = rsLot.getInt(1);
                                        for (int i = 1; i <= 20; i++) {
                                            try (PreparedStatement psSpot = conn.prepareStatement("INSERT INTO parking_spots(lot_id, spot_number) VALUES(?, ?)")) {
                                                psSpot.setInt(1, lotId);
                                                psSpot.setInt(2, i);
                                                psSpot.executeUpdate();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Hospitals
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM hospital_rooms")) {
                boolean repopulate = false;
                if (!rs.next() || rs.getInt(1) > 500) { // If it has the old 900 rooms (300*3)
                    repopulate = true;
                }
                
                if (repopulate) {
                    System.out.println("Repopulating Hospitals with 150 rooms each...");
                    try (Statement sDelete = conn.createStatement()) {
                        sDelete.execute("DELETE FROM hospital_bills");
                        sDelete.execute("DELETE FROM hospital_bookings");
                        sDelete.execute("DELETE FROM hospital_rooms");
                        sDelete.execute("DELETE FROM hospitals");
                    }
                    String[] defaultHospitals = {"City Central Hospital", "St. Mary Medical", "Metro Health Clinic"};
                    for (String name : defaultHospitals) {
                        try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO hospitals(name) VALUES(?)", Statement.RETURN_GENERATED_KEYS)) {
                            ps.setString(1, name);
                            ps.executeUpdate();
                            ResultSet rsg = ps.getGeneratedKeys();
                            if (rsg.next()) {
                                int hospitalId = rsg.getInt(1);
                                for (int i = 1; i <= 150; i++) {
                                    String type = (i <= 50) ? "VIP" : "NORMAL";
                                    try (PreparedStatement psRoom = conn.prepareStatement("INSERT INTO hospital_rooms(hospital_id, room_number, type) VALUES(?, ?, ?)")) {
                                        psRoom.setInt(1, hospitalId);
                                        psRoom.setInt(2, i);
                                        psRoom.setString(3, type);
                                        psRoom.executeUpdate();
                                    }
                                }
                                try (PreparedStatement psLot = conn.prepareStatement("INSERT OR IGNORE INTO parking_lots(name, type, reference_id) VALUES(?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                                    psLot.setString(1, name + " Parking");
                                    psLot.setString(2, "HOSPITAL");
                                    psLot.setInt(3, hospitalId);
                                    psLot.executeUpdate();
                                    ResultSet rsLot = psLot.getGeneratedKeys();
                                    if (rsLot.next()) {
                                        int lotId = rsLot.getInt(1);
                                        for (int i = 1; i <= 20; i++) {
                                            try (PreparedStatement psSpot = conn.prepareStatement("INSERT INTO parking_spots(lot_id, spot_number) VALUES(?, ?)")) {
                                                psSpot.setInt(1, lotId);
                                                psSpot.setInt(2, i);
                                                psSpot.executeUpdate();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Stations
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM trains")) {
                boolean repopulate = false;
                if (!rs.next() || rs.getInt(1) != 900) {
                    repopulate = true;
                }
                
                if (repopulate) {
                    System.out.println("Repopulating Stations and Trains (Target: 900 trains)...");
                    try (Statement sDelete = conn.createStatement()) {
                        sDelete.execute("DELETE FROM train_tickets");
                        sDelete.execute("DELETE FROM trains");
                        sDelete.execute("DELETE FROM stations");
                    }
                    String[] cities = {"Mumbai", "Delhi", "Bangalore", "Hyderabad", "Ahmedabad", "Chennai", "Kolkata", "Surat", "Pune", "Jaipur"};
                    List<Integer> stationIds = new ArrayList<>();
                    for (String city : cities) {
                        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO stations(name) VALUES(?)", Statement.RETURN_GENERATED_KEYS)) {
                            ps.setString(1, city);
                            ps.executeUpdate();
                            ResultSet rsG = ps.getGeneratedKeys();
                            if (rsG.next()) stationIds.add(rsG.getInt(1));
                        }
                    }
                    
                    for (int i = 0; i < stationIds.size(); i++) {
                        for (int j = 0; j < stationIds.size(); j++) {
                            if (i == j) continue;
                            int fromId = stationIds.get(i);
                            int toId = stationIds.get(j);
                            for (int k = 1; k <= 10; k++) {
                                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO trains(name, from_station_id, to_station_id, departure_time, max_capacity, price, status) VALUES(?, ?, ?, ?, ?, ?, 'SCHEDULED')")) {
                                    ps.setString(1, cities[i].substring(0, 3) + "-" + cities[j].substring(0, 3) + " Exp " + k);
                                    ps.setInt(2, fromId);
                                    ps.setInt(3, toId);
                                    ps.setString(4, String.format("%02d:00 %s", (6 + k) % 12 == 0 ? 12 : (6 + k) % 12, (6 + k) < 12 || (6 + k) >= 24 ? "AM" : "PM"));
                                    ps.setInt(5, 100);
                                    ps.setDouble(6, 450.0 + (k * 25));
                                    ps.executeUpdate();
                                }
                            }
                        }
                    }
                }
            }

            // 4. System Accounts
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM users WHERE username LIKE 'SYSTEM_%'")) {
                if (rs.next() && rs.getInt(1) < 6) {
                    System.out.println("Creating System Accounts...");
                    String[] systemAccounts = {"SYSTEM_RESTAURANT", "SYSTEM_HOSPITAL", "SYSTEM_TRAIN", "SYSTEM_ELECTRICITY", "SYSTEM_COURIER", "SYSTEM_SCHOOL"};                    for (String account : systemAccounts) {
                        try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO users(username, password, role) VALUES(?, ?, ?)")) {
                            ps.setString(1, account);
                            ps.setString(2, "system_pass");
                            ps.setString(3, "ADMIN");
                            ps.executeUpdate();
                        }
                        try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO bank_accounts(username, bank_password, balance, status, approved_by) VALUES(?, ?, ?, ?, ?)")) {
                            ps.setString(1, account);
                            ps.setString(2, "1234");
                            ps.setDouble(3, 1000000.0); // Give systems some money to allow refunds
                            ps.setString(4, "APPROVED");
                            ps.setString(5, "SYSTEM");
                            ps.executeUpdate();
                        }
                    }
                }
            }

            // 5. Schools
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM schools")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    System.out.println("Populating Schools...");
                    String[] defaultSchools = {"Saint Xavier's School", "Heritage International", "Oakridge Academy", "Global Vision High"};
                    for (String name : defaultSchools) {
                        try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO schools(name) VALUES(?)")) {
                            ps.setString(1, name);
                            ps.executeUpdate();
                        }
                    }
                }
            }

            // 6. Library Books
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM books")) {
                if (rs.next() && rs.getInt(1) < 10) { // If it's the old 8 books or empty
                    System.out.println("Populating Library with 100 books...");
                    s.execute("DELETE FROM books"); // Clear old
                    String[] categories = {"Computer Science", "Fiction", "Science", "Literature", "AI", "History", "Biography", "Art", "Business", "Health"};
                    for (int i = 0; i < categories.length; i++) {
                        String cat = categories[i];
                        for (int j = 1; j <= 10; j++) {
                            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO books (title, author, category, quantity, available_count, price) VALUES (?, ?, ?, ?, ?, ?)")) {
                                ps.setString(1, cat + " Vol " + j);
                                ps.setString(2, "Author " + ((i * 10) + j));
                                ps.setString(3, cat);
                                ps.setInt(4, 5);
                                ps.setInt(5, 5);
                                ps.setDouble(6, 400.0 + (j * 20)); // Varying prices
                                ps.executeUpdate();
                            }
                        }
                    }
                }
            }

            conn.commit();
            System.out.println("Default data check complete.");
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    private static void ensureDefaultOwner(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM users WHERE username='owner'"
             )) {

            ResultSet rs = ps.executeQuery();

            if (rs.next() && rs.getInt(1) == 0) {
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO users(username,password,role) VALUES(?,?,?)"
                )) {
                    insert.setString(1, "owner");
                    insert.setString(2, "owner123");
                    insert.setString(3, "OWNER");
                    insert.executeUpdate();
                }
            }
        }
    }
}
