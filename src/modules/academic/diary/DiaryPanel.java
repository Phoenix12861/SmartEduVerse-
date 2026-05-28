package modules.academic.diary;

import core.SessionManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class DiaryPanel extends JPanel {

    private CardLayout cardLayout;
    private JPanel cards;
    private DefaultTableModel entryModel;
    private JTable entryTable;
    private String username;

    public DiaryPanel() {
        this.username = SessionManager.getCurrentUser();
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);
        cards.setOpaque(false);

        cards.add(createListPanel(), "LIST");
        cards.add(createEditorPanel(null), "EDITOR");

        add(cards, BorderLayout.CENTER);
        refreshEntries();
    }

    private JPanel createListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(40, 60, 40, 60));


        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Personal Diary");
        title.setFont(new Font("SansSerif", Font.BOLD, 42));
        header.add(title, BorderLayout.WEST);
        panel.add(header, BorderLayout.NORTH);

        entryModel = new DefaultTableModel(new String[]{"ID", "Title", "Status", "Date"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        entryTable = new JTable(entryModel);
        entryTable.setRowHeight(32);
        entryTable.setFont(new Font("SansSerif", Font.PLAIN, 15));
        entryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        entryTable.setShowVerticalLines(false);
        entryTable.setGridColor(new Color(240, 240, 240));
        entryTable.setPreferredScrollableViewportSize(new Dimension(960, 330));
        entryTable.removeColumn(entryTable.getColumnModel().getColumn(0));

        JScrollPane scroll = new JScrollPane(entryTable);
        scroll.setBorder(new LineBorder(new Color(230, 230, 230), 1));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 25));
        actions.setOpaque(false);

        JButton addBtn = createStyledButton("NEW ENTRY", Color.BLACK, Color.WHITE);
        addBtn.addActionListener(e -> {
            cards.add(createEditorPanel(null), "EDITOR");
            cardLayout.show(cards, "EDITOR");
        });

        JButton viewBtn = createStyledButton("VIEW/UPDATE", Color.DARK_GRAY, Color.WHITE);
        viewBtn.addActionListener(e -> handleViewUpdate());

        JButton lockBtn = createStyledButton("LOCK/UNLOCK", new Color(0, 150, 136), Color.WHITE);
        lockBtn.addActionListener(e -> handleLock());

        JButton deleteBtn = createStyledButton("DELETE", new Color(211, 47, 47), Color.WHITE);
        deleteBtn.addActionListener(e -> handleDelete());

        actions.add(addBtn);
        actions.add(viewBtn);
        actions.add(lockBtn);
        actions.add(deleteBtn);

        JPanel mainContent = new JPanel(new GridBagLayout());
        mainContent.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0; 
        gbc.anchor = GridBagConstraints.NORTH;
        mainContent.add(scroll, gbc);
        
        gbc.gridy = 1;
        mainContent.add(actions, gbc);
        
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        mainContent.add(Box.createGlue(), gbc);

        panel.add(mainContent, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createEditorPanel(Integer entryId) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new EmptyBorder(20, 40, 40, 40));


        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.setOpaque(false);
        JButton back = new JButton("← Back");
        back.setFocusPainted(false);
        back.setBackground(Color.BLACK);
        back.setForeground(Color.WHITE);
        back.setPreferredSize(new Dimension(120, 40));
        back.addActionListener(e -> cardLayout.show(cards, "LIST"));
        topBar.add(back);
        wrapper.add(topBar, BorderLayout.NORTH);

        JPanel main = new JPanel(new GridBagLayout());
        main.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(220, 220, 220), 1), 
            new EmptyBorder(30, 40, 30, 40)
        ));
        card.setPreferredSize(new Dimension(650, 600));

        JLabel title = new JLabel(entryId == null ? "New Diary Entry" : "Edit Diary Entry");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField titleField = new JTextField();
        titleField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        titleField.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleField.setBorder(BorderFactory.createTitledBorder("ENTRY TITLE"));

        JTextArea bodyArea = new JTextArea();
        bodyArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);
        JScrollPane bodyScroll = new JScrollPane(bodyArea);
        bodyScroll.setBorder(BorderFactory.createTitledBorder("CONTENT"));
        bodyScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        bodyScroll.setAlignmentX(Component.CENTER_ALIGNMENT);

        if (entryId != null) {
            int row = entryTable.getSelectedRow();
            titleField.setText(entryModel.getValueAt(row, 1).toString());
            bodyArea.setText(DiaryManager.getEntryContent(entryId));
        }

        JButton saveBtn = createStyledButton("SAVE TO DIARY", new Color(0, 150, 136), Color.WHITE);
        saveBtn.setMaximumSize(new Dimension(300, 45));
        saveBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveBtn.addActionListener(e -> {
            String t = titleField.getText().trim();
            String b = bodyArea.getText().trim();
            if (t.isEmpty() || b.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields.");
                return;
            }
            if (entryId == null) DiaryManager.addEntry(username, t, b);
            else DiaryManager.updateEntry(entryId, t, b);
            refreshEntries();
            cardLayout.show(cards, "LIST");
        });

        card.add(title);
        card.add(Box.createVerticalStrut(30));
        card.add(titleField);
        card.add(Box.createVerticalStrut(20));
        card.add(bodyScroll);
        card.add(Box.createVerticalStrut(40));
        card.add(saveBtn);

        main.add(card, gbc);
        wrapper.add(main, BorderLayout.CENTER);
        return wrapper;
    }

    private void handleViewUpdate() {
        int row = entryTable.getSelectedRow();
        if (row == -1) return;
        int id = (int) entryModel.getValueAt(row, 0);
        String status = entryModel.getValueAt(row, 2).toString();

        if (status.equals("LOCKED")) {
            String pass = JOptionPane.showInputDialog(this, "Entry is Locked. Enter Diary Password:");
            if (!DiaryManager.verifyDiaryPassword(username, pass)) {
                JOptionPane.showMessageDialog(this, "Incorrect Password");
                return;
            }
        }

        cards.add(createEditorPanel(id), "EDITOR");
        cardLayout.show(cards, "EDITOR");
    }

    private void handleLock() {
        int row = entryTable.getSelectedRow();
        if (row == -1) return;
        int id = (int) entryModel.getValueAt(row, 0);
        String status = entryModel.getValueAt(row, 2).toString();

        if (status.equals("OPEN")) {
            if (!DiaryManager.hasDiaryPassword(username)) {
                showSetupPasswordDialog(() -> {
                    DiaryManager.setLock(id, true);
                    refreshEntries();
                });
            } else {
                DiaryManager.setLock(id, true);
                refreshEntries();
            }
        } else {
            String pass = JOptionPane.showInputDialog(this, "Enter Password to Unlock:");
            if (DiaryManager.verifyDiaryPassword(username, pass)) {
                DiaryManager.setLock(id, false);
                refreshEntries();
            } else {
                JOptionPane.showMessageDialog(this, "Incorrect Password");
            }
        }
    }

    private void handleDelete() {
        int row = entryTable.getSelectedRow();
        if (row == -1) return;
        int id = (int) entryModel.getValueAt(row, 0);
        if (JOptionPane.showConfirmDialog(this, "Delete this entry?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            DiaryManager.deleteEntry(id);
            refreshEntries();
        }
    }

    private void showSetupPasswordDialog(Runnable onSuccess) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Setup Diary Password", true);
        dialog.setSize(500, 600);
        dialog.setLocationRelativeTo(this);

        AuthAction action = (oldP, p1, p2) -> {
            if (p1.isBlank() || !p1.equals(p2)) {
                JOptionPane.showMessageDialog(dialog, "Passwords must match and not be empty");
                return;
            }
            if (DiaryManager.createDiaryPassword(username, p1)) {
                dialog.dispose();
                onSuccess.run();
            }
        };

        dialog.add(createAuthStylePanel("Create Diary Password", "Secure your entries", "CREATE PASSWORD", action, false));
        dialog.setVisible(true);
    }

    private JPanel createAuthStylePanel(String titleStr, String sub, String btnStr, AuthAction action, boolean showOld) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);

        JPanel card = new JPanel();
        card.setPreferredSize(new Dimension(450, 500));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(30, 40, 30, 40));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(titleStr);
        title.setFont(new Font("SansSerif", Font.BOLD, 32));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPasswordField oldF = createStyledPasswordField();
        JPasswordField p1 = createStyledPasswordField();
        JPasswordField p2 = createStyledPasswordField();

        JButton btn = new JButton(btnStr);
        btn.setBackground(Color.BLACK);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(Box.createVerticalGlue());
        card.add(title);
        card.add(Box.createVerticalStrut(30));

        if(showOld) {
            card.add(createLabel("CURRENT PASSWORD"));
            card.add(oldF);
            card.add(Box.createVerticalStrut(15));
        }

        card.add(createLabel("NEW PASSWORD"));
        card.add(p1);
        card.add(Box.createVerticalStrut(15));
        card.add(createLabel("CONFIRM PASSWORD"));
        card.add(p2);
        card.add(Box.createVerticalStrut(30));
        card.add(btn);
        card.add(Box.createVerticalGlue());

        btn.addActionListener(e -> action.run(new String(oldF.getPassword()), new String(p1.getPassword()), new String(p2.getPassword())));
        wrapper.add(card);
        return wrapper;
    }

    private JPasswordField createStyledPasswordField() {
        JPasswordField pf = new JPasswordField();
        pf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        pf.setAlignmentX(Component.CENTER_ALIGNMENT);
        pf.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(220, 220, 220), 1), new EmptyBorder(5, 10, 5, 10)));
        return pf;
    }

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(Color.GRAY);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private void refreshEntries() {
        entryModel.setRowCount(0);
        List<Object[]> entries = DiaryManager.getEntries(username);
        for (Object[] e : entries) entryModel.addRow(e);
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(150, 40));
        return btn;
    }

    @FunctionalInterface
    interface AuthAction { void run(String p1, String p2, String p3); }
}
