package modules.management.library;

import core.SessionManager;
import core.UserRole;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;

public class LibraryPanel extends JPanel {

    private JTable bookTable;
    private DefaultTableModel bookModel;
    private JComboBox<String> categoryFilter;
    private JButton borrowBtn;
    private JPanel mainContent;
    private CardLayout cardLayout;

    public LibraryPanel() {
        LibraryManager.checkOverdueAndFine();
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        cardLayout = new CardLayout();
        mainContent = new JPanel(cardLayout);
        mainContent.setBackground(Color.WHITE);

        // UI Views
        mainContent.add(createBrowseView(), "BROWSE");
        mainContent.add(createBillsView(), "BILLS");
        if (SessionManager.getCurrentRole() != UserRole.STUDENT && SessionManager.getCurrentRole() != UserRole.USER) {
            mainContent.add(createAdminView(), "ADMIN");
        }

        // Header with top-right buttons
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel titleLabel = new JLabel("Smart Library System");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        header.add(titleLabel, BorderLayout.WEST);

        JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        topButtons.setBackground(Color.WHITE);

        JButton browseBtn = styleTabBtn("Browse");
        browseBtn.addActionListener(e -> cardLayout.show(mainContent, "BROWSE"));
        topButtons.add(browseBtn);

        JButton myBillsBtn = styleTabBtn("My Bills");
        myBillsBtn.addActionListener(e -> {
            cardLayout.show(mainContent, "BILLS");
            refreshUserBills();
        });
        topButtons.add(myBillsBtn);

        if (SessionManager.getCurrentRole() != UserRole.STUDENT && SessionManager.getCurrentRole() != UserRole.USER) {
            JButton adminBtn = styleTabBtn("Admin Panel");
            adminBtn.setBackground(new Color(200, 0, 0));
            adminBtn.addActionListener(e -> {
                cardLayout.show(mainContent, "ADMIN");
                refreshAdminData();
            });
            topButtons.add(adminBtn);
        }

        header.add(topButtons, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        add(mainContent, BorderLayout.CENTER);

        refreshBooks();
    }

    private JPanel createBrowseView() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setBackground(Color.WHITE);
        filterPanel.add(new JLabel("Category:"));
        String[] cats = {"All", "Computer Science", "Fiction", "Science", "Literature", "AI", "History", "Biography", "Art", "Business", "Health"};
        categoryFilter = new JComboBox<>(cats);
        categoryFilter.addActionListener(e -> refreshBooks());
        filterPanel.add(categoryFilter);
        panel.add(filterPanel, BorderLayout.NORTH);

        String[] cols = {"ID", "Title", "Author", "Category", "Available", "Price (₹)", "REAL_ID"};
        bookModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        bookTable = styleTable(new JTable(bookModel));
        bookTable.removeColumn(bookTable.getColumnModel().getColumn(6)); // Hide real ID
        bookTable.getSelectionModel().addListSelectionListener(e -> updateBorrowButton());

        JScrollPane scroll = new JScrollPane(bookTable);
        scroll.setPreferredSize(new Dimension(720, 400)); // 10% smaller than 800x450 approx
        panel.add(scroll, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionPanel.setBackground(Color.WHITE);

        borrowBtn = styleBtn("Borrow / Rent");
        borrowBtn.addActionListener(e -> handleBorrowOrReturn());
        
        JButton buyBtn = styleBtn("Buy This Book");
        buyBtn.addActionListener(e -> handlePurchase());

        actionPanel.add(borrowBtn);
        actionPanel.add(buyBtn);
        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createBillsView() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JLabel title = new JLabel("My Library Bills");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(title, BorderLayout.NORTH);

        String[] cols = {"Bill ID", "Description", "Amount", "Status", "Date"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        JTable table = styleTable(new JTable(model));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(720, 400));
        panel.add(scroll, BorderLayout.CENTER);

        JButton payBtn = styleBtn("Pay Selected Bill");
        payBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int id = Integer.parseInt(model.getValueAt(row, 0).toString());
                String status = (String) model.getValueAt(row, 3);
                if (status.equals("UNPAID")) {
                    String pass = JOptionPane.showInputDialog(this, "Enter Bank Password:");
                    if (pass != null && LibraryManager.payBill(id, SessionManager.getCurrentUser(), pass)) {
                        JOptionPane.showMessageDialog(this, "Bill Paid!");
                        refreshUserBills();
                    } else {
                        JOptionPane.showMessageDialog(this, "Payment Failed.");
                    }
                }
            }
        });
        panel.add(payBtn, BorderLayout.SOUTH);

        return panel;
    }

    private DefaultTableModel adminRecModel;
    private DefaultTableModel adminBookModel;

    private JPanel createAdminView() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JTabbedPane adminTabs = new JTabbedPane();

        // Admin Book Management
        JPanel bookMgmt = new JPanel(new BorderLayout(10, 10));
        bookMgmt.setBackground(Color.WHITE);
        String[] bookCols = {"ID", "Title", "Author", "Category", "Qty", "Price", "REAL_ID"};
        adminBookModel = new DefaultTableModel(bookCols, 0);
        JTable table = styleTable(new JTable(adminBookModel));
        table.removeColumn(table.getColumnModel().getColumn(6)); // Hide real ID
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(650, 400));
        bookMgmt.add(scroll, BorderLayout.CENTER);

        JPanel controls = new JPanel(new GridBagLayout());
        controls.setBackground(Color.WHITE);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5); g.fill = GridBagConstraints.HORIZONTAL;

        JTextField tF = styleField(15); JTextField aF = styleField(15);
        JTextField cF = styleField(15); JTextField qF = styleField("5", 5);
        JTextField pF = styleField("500", 5);

        g.gridx = 0; g.gridy = 0; controls.add(new JLabel("Title:"), g); g.gridx = 1; controls.add(tF, g);
        g.gridx = 0; g.gridy++; controls.add(new JLabel("Author:"), g); g.gridx = 1; controls.add(aF, g);
        g.gridx = 0; g.gridy++; controls.add(new JLabel("Category:"), g); g.gridx = 1; controls.add(cF, g);
        g.gridx = 0; g.gridy++; controls.add(new JLabel("Qty:"), g); g.gridx = 1; controls.add(qF, g);
        g.gridx = 0; g.gridy++; controls.add(new JLabel("Price:"), g); g.gridx = 1; controls.add(pF, g);

        JButton addBtn = styleBtn("Add Book");
        addBtn.addActionListener(e -> {
            try {
                if (LibraryManager.addBook(tF.getText(), aF.getText(), cF.getText(), Integer.parseInt(qF.getText()), Double.parseDouble(pF.getText()))) {
                    JOptionPane.showMessageDialog(this, "Book Added!");
                    refreshAdminData();
                }
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid input."); }
        });
        g.gridx = 0; g.gridy++; g.gridwidth = 2; controls.add(addBtn, g);

        JButton editBtn = styleBtn("Edit Selected Book");
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int modelRow = table.convertRowIndexToModel(row);
                int realId = (int) adminBookModel.getValueAt(modelRow, 6);
                if (LibraryManager.updateBook(realId, tF.getText(), aF.getText(), cF.getText(), Integer.parseInt(qF.getText()), Double.parseDouble(pF.getText()))) {
                    JOptionPane.showMessageDialog(this, "Book Updated!");
                    refreshAdminData();
                }
            } else { JOptionPane.showMessageDialog(this, "Select a book first."); }
        });
        g.gridy++; controls.add(editBtn, g);

        JButton fillBtn = styleBtn("Fill Fields from Selection");
        fillBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int modelRow = table.convertRowIndexToModel(row);
                tF.setText(adminBookModel.getValueAt(modelRow, 1).toString());
                aF.setText(adminBookModel.getValueAt(modelRow, 2).toString());
                cF.setText(adminBookModel.getValueAt(modelRow, 3).toString());
                String[] qtyParts = adminBookModel.getValueAt(modelRow, 4).toString().split("/");
                qF.setText(qtyParts[1]);
                pF.setText(adminBookModel.getValueAt(modelRow, 5).toString());
            }
        });
        g.gridy++; controls.add(fillBtn, g);

        JButton delBtn = styleBtn("Delete Selected");
        delBtn.setBackground(new Color(200, 0, 0));
        delBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int modelRow = table.convertRowIndexToModel(row);
                int realId = (int) adminBookModel.getValueAt(modelRow, 6);
                if (LibraryManager.deleteBook(realId)) refreshAdminData();
            }
        });
        g.gridy++; controls.add(delBtn, g);

        if (SessionManager.getCurrentRole() == UserRole.OWNER) {
            JButton priceBtn = styleBtn("Change Price (Owner Only)");
            priceBtn.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    String newP = JOptionPane.showInputDialog(this, "Enter New Price:");
                    if (newP != null) {
                        int modelRow = table.convertRowIndexToModel(row);
                        int realId = (int) adminBookModel.getValueAt(modelRow, 6);
                        LibraryManager.updateBookPrice(realId, Double.parseDouble(newP));
                        refreshAdminData();
                    }
                }
            });
            g.gridy++; controls.add(priceBtn, g);
        }

        bookMgmt.add(controls, BorderLayout.EAST);
        adminTabs.addTab("Manage Books", bookMgmt);

        // Admin Records & Billing
        JPanel recordMgmt = new JPanel(new BorderLayout(10, 10));
        recordMgmt.setBackground(Color.WHITE);
        String[] recCols = {"ID", "User", "Book", "Borrowed", "Due", "Status", "Fine"};
        adminRecModel = new DefaultTableModel(recCols, 0);
        JTable recTable = styleTable(new JTable(adminRecModel));
        recordMgmt.add(new JScrollPane(recTable), BorderLayout.CENTER);

        JButton billBtn = styleBtn("Generate Purchase Bill for Selected User");
        billBtn.addActionListener(e -> {
            int row = recTable.getSelectedRow();
            if (row >= 0) {
                String user = (String) adminRecModel.getValueAt(row, 1);
                String book = (String) adminRecModel.getValueAt(row, 2);
                // Need to find price. For simplicity, ask or get from selected book in management tab.
                // Or let's just use 500 as default or ask.
                String amt = JOptionPane.showInputDialog(this, "Enter Billing Amount for " + user + ":", "500");
                if (amt != null) {
                    LibraryManager.createBill(user, book, Double.parseDouble(amt));
                    JOptionPane.showMessageDialog(this, "Bill Created for " + user);
                }
            }
        });
        recordMgmt.add(billBtn, BorderLayout.SOUTH);
        adminTabs.addTab("Borrowed Records & Billing", recordMgmt);

        panel.add(adminTabs, BorderLayout.CENTER);
        return panel;
    }

    private void refreshBooks() {
        bookModel.setRowCount(0);
        String cat = (String) categoryFilter.getSelectedItem();
        List<Book> books = LibraryManager.getAllBooks(cat);
        int displayId = 1;
        for (Book b : books) {
            bookModel.addRow(new Object[]{displayId++, b.title, b.author, b.category, b.availableCount + "/" + b.quantity, b.price, b.id});
        }
    }

    private void refreshUserBills() {
        JPanel view = (JPanel) mainContent.getComponent(1);
        JScrollPane scroll = (JScrollPane) view.getComponent(1);
        JTable table = (JTable) scroll.getViewport().getView();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        for (String[] r : LibraryManager.getUserBills(SessionManager.getCurrentUser())) model.addRow(r);
    }

    private void refreshAdminData() {
        adminBookModel.setRowCount(0);
        int displayId = 1;
        for (Book b : LibraryManager.getAllBooks("All")) {
            adminBookModel.addRow(new Object[]{displayId++, b.title, b.author, b.category, b.availableCount + "/" + b.quantity, b.price, b.id});
        }
        adminRecModel.setRowCount(0);
        for (String[] r : LibraryManager.getAllRecords()) adminRecModel.addRow(r);
        refreshBooks();
    }

    private void updateBorrowButton() {
        int row = bookTable.getSelectedRow();
        if (row < 0) { borrowBtn.setText("Borrow / Rent"); return; }
        int modelRow = bookTable.convertRowIndexToModel(row);
        int realId = (int) bookModel.getValueAt(modelRow, 6);
        if (LibraryManager.isBookBorrowedByUser(realId, SessionManager.getCurrentUser())) {
            borrowBtn.setText("Return This Book");
            borrowBtn.setBackground(new Color(0, 150, 0));
        } else {
            borrowBtn.setText("Borrow / Rent");
            borrowBtn.setBackground(Color.BLACK);
        }
    }

    private void handleBorrowOrReturn() {
        int row = bookTable.getSelectedRow();
        if (row < 0) return;
        int modelRow = bookTable.convertRowIndexToModel(row);
        int realId = (int) bookModel.getValueAt(modelRow, 6);
        String user = SessionManager.getCurrentUser();
        if (LibraryManager.isBookBorrowedByUser(realId, user)) {
            int recId = LibraryManager.getBorrowedRecordId(realId, user);
            if (LibraryManager.payFineAndReturn(recId, user, "")) {
                JOptionPane.showMessageDialog(this, "Returned!");
                refreshBooks();
            }
        } else {
            if (LibraryManager.borrowBook(realId, user)) {
                JOptionPane.showMessageDialog(this, "Borrowed!");
                refreshBooks();
            }
        }
    }

    private void handlePurchase() {
        int row = bookTable.getSelectedRow();
        if (row >= 0) {
            String title = (String) bookModel.getValueAt(row, 1);
            double price = (double) bookModel.getValueAt(row, 5);
            int choice = JOptionPane.showConfirmDialog(this, "Buy '" + title + "' for ₹" + price + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                String user = SessionManager.getCurrentUser();
                // Rule: If not student, create bill. If student, pay now or fail (as per prompt "only if role != Student" for bill creation?)
                // Actually the prompt says: "create bills who have borrowed a book only if their role != Student"
                // And "to buy a book let the system keep defined prices"
                // Let's implement: Non-students get a bill, students must pay now.
                if (SessionManager.getCurrentRole() != UserRole.STUDENT) {
                    LibraryManager.createBill(user, title, price);
                    JOptionPane.showMessageDialog(this, "Bill generated! Pay it in 'My Bills'.");
                } else {
                    String pass = JOptionPane.showInputDialog(this, "Enter Bank Password:");
                    if (pass != null && modules.finance.banking.BankAccountManager.transferMoney(user, "SYSTEM_SCHOOL", price, pass)) {
                        JOptionPane.showMessageDialog(this, "Purchase successful!");
                    } else {
                        JOptionPane.showMessageDialog(this, "Payment failed.");
                    }
                }
                refreshBooks();
            }
        }
    }

    private JTable styleTable(JTable t) {
        t.setRowHeight(35);
        t.setFont(new Font("SansSerif", Font.PLAIN, 14));
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        t.getTableHeader().setBackground(Color.WHITE);
        t.getTableHeader().setBorder(BorderFactory.createLineBorder(Color.BLACK));
        t.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        t.setGridColor(Color.BLACK);
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < t.getColumnCount(); i++) t.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        
        return t;
    }

    private JButton styleBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(Color.BLACK);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        return b;
    }

    private JButton styleTabBtn(String text) {
        JButton b = styleBtn(text);
        b.setPreferredSize(new Dimension(120, 35));
        return b;
    }

    private JTextField styleField(int cols) { return styleField("", cols); }
    private JTextField styleField(String text, int cols) {
        JTextField f = new JTextField(text, cols);
        f.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        return f;
    }
}
