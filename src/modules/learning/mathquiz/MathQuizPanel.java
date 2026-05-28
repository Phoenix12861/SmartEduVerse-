package modules.learning.mathquiz;

import javax.swing.*;
import java.awt.*;

public class MathQuizPanel extends JPanel {

    private JTextArea questionArea;
    private JTextArea feedbackArea;
    private JTextField answerField;
    private int score = 0;
    private int level = 1;
    private String currentQuestion = "";

    public MathQuizPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // HEADER
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        
        JLabel title = new JLabel("Smart AI Math Quiz");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        header.add(title, BorderLayout.WEST);

        JLabel scoreLbl = new JLabel("Score: " + score + " | Level: " + level);
        scoreLbl.setFont(new Font("SansSerif", Font.BOLD, 16));
        header.add(scoreLbl, BorderLayout.EAST);
        
        add(header, BorderLayout.NORTH);

        // QUESTION AREA
        questionArea = new JTextArea("Click 'Next Challenge' to begin.");
        questionArea.setEditable(false);
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        questionArea.setFont(new Font("Monospaced", Font.BOLD, 18));
        questionArea.setBackground(new Color(250, 250, 250));
        questionArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        add(new JScrollPane(questionArea), BorderLayout.CENTER);

        // BOTTOM CONTROLS
        JPanel south = new JPanel(new BorderLayout(10, 10));
        south.setBackground(Color.WHITE);

        JPanel inputRow = new JPanel(new BorderLayout(5, 5));
        inputRow.setBackground(Color.WHITE);
        answerField = new JTextField();
        answerField.setFont(new Font("SansSerif", Font.PLAIN, 18));
        answerField.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        
        JButton submitBtn = styleBtn("Submit Answer");
        inputRow.add(answerField, BorderLayout.CENTER);
        inputRow.add(submitBtn, BorderLayout.EAST);
        
        south.add(inputRow, BorderLayout.NORTH);

        feedbackArea = new JTextArea();
        feedbackArea.setEditable(false);
        feedbackArea.setLineWrap(true);
        feedbackArea.setWrapStyleWord(true);
        feedbackArea.setPreferredSize(new Dimension(0, 80));
        feedbackArea.setBackground(Color.WHITE);
        feedbackArea.setFont(new Font("SansSerif", Font.ITALIC, 14));
        south.add(new JScrollPane(feedbackArea), BorderLayout.CENTER);

        JButton nextBtn = styleBtn("Next Challenge");
        south.add(nextBtn, BorderLayout.SOUTH);

        add(south, BorderLayout.SOUTH);

        // ACTIONS
        nextBtn.addActionListener(e -> {
            feedbackArea.setText("");
            answerField.setText("");
            questionArea.setText("Generating challenge using phi3...");
            new Thread(() -> {
                currentQuestion = MathQuizAI.getChallenge(level);
                SwingUtilities.invokeLater(() -> questionArea.setText(currentQuestion));
            }).start();
        });

        submitBtn.addActionListener(e -> {
            String ans = answerField.getText().trim();
            if (ans.isEmpty()) return;
            
            feedbackArea.setText("Verifying with AI...");
            new Thread(() -> {
                boolean correct = MathQuizAI.checkAnswer(currentQuestion, ans);
                String feedback = MathQuizAI.getFeedback(currentQuestion, ans);
                SwingUtilities.invokeLater(() -> {
                    if (correct) {
                        score += 10;
                        if (score % 30 == 0) level++;
                        feedbackArea.setText("EXCELLENT! " + feedback);
                    } else {
                        feedbackArea.setText("INCORRECT. " + feedback);
                    }
                    scoreLbl.setText("Score: " + score + " | Level: " + level);
                });
            }).start();
        });
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
