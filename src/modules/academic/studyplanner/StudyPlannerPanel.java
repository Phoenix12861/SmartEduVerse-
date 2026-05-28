package modules.academic.studyplanner;

import core.SessionManager;

import javax.swing.*;
import java.awt.*;

public class StudyPlannerPanel extends JPanel {

    private JTextArea analysisArea;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton analyzeBtn;

    public StudyPlannerPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));


        JLabel title = new JLabel("Smart AI Study Planner");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(Color.BLACK);
        add(title, BorderLayout.NORTH);


        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(450);
        split.setBackground(Color.WHITE);
        split.setBorder(null);


        JPanel left = new JPanel(new BorderLayout(10, 10));
        left.setBackground(Color.WHITE);
        
        JLabel sub1 = new JLabel("Performance Analysis");
        sub1.setFont(new Font("SansSerif", Font.BOLD, 18));
        left.add(sub1, BorderLayout.NORTH);

        analysisArea = new JTextArea();
        analysisArea.setEditable(false);
        analysisArea.setLineWrap(true);
        analysisArea.setWrapStyleWord(true);
        analysisArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        analysisArea.setBackground(new Color(250, 250, 250));
        analysisArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        
        left.add(new JScrollPane(analysisArea), BorderLayout.CENTER);

        analyzeBtn = styleBtn("Analyze My Report Card");
        analyzeBtn.addActionListener(e -> {
            analyzeBtn.setEnabled(false);
            analysisArea.setText("Analyzing records using phi3 model...");
            new Thread(() -> {
                String response = StudyPlannerAI.getAnalysis(SessionManager.getCurrentUser());
                SwingUtilities.invokeLater(() -> {
                    analysisArea.setText(response);
                    analyzeBtn.setEnabled(true);
                });
            }).start();
        });
        left.add(analyzeBtn, BorderLayout.SOUTH);


        JPanel right = new JPanel(new BorderLayout(10, 10));
        right.setBackground(Color.WHITE);

        JLabel sub2 = new JLabel("AI Educational Consultant");
        sub2.setFont(new Font("SansSerif", Font.BOLD, 18));
        right.add(sub2, BorderLayout.NORTH);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBackground(Color.WHITE);
        chatArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        right.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBackground(Color.WHITE);
        
        inputField = new JTextField();
        inputField.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        inputField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        JButton sendBtn = styleBtn("Ask");
        sendBtn.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);
        right.add(inputPanel, BorderLayout.SOUTH);

        split.setLeftComponent(left);
        split.setRightComponent(right);
        add(split, BorderLayout.CENTER);
    }

    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (msg.isEmpty()) return;

        chatArea.append("You: " + msg + "\n\n");
        inputField.setText("");
        
        new Thread(() -> {
            String resp = StudyPlannerAI.chat(SessionManager.getCurrentUser(), msg);
            SwingUtilities.invokeLater(() -> {
                chatArea.append("AI: " + resp + "\n\n");
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            });
        }).start();
    }

    private JButton styleBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(Color.BLACK);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        return b;
    }
}
