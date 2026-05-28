package modules.learning.onlinequiz;

import ai.OllamaClient;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class OnlineQuizPanel extends JPanel {

    private JTextArea questionArea;
    private JPanel optionsPanel;
    private JLabel statusLbl;
    private String currentTopic = "General Knowledge";
    private String correctAnswer = "";

    public OnlineQuizPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Smart AI Online Quiz");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);

        questionArea = new JTextArea("Select a topic and start the quiz!");
        questionArea.setEditable(false);
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        questionArea.setFont(new Font("SansSerif", Font.BOLD, 18));
        questionArea.setBackground(new Color(250, 250, 250));
        questionArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        center.add(new JScrollPane(questionArea), BorderLayout.CENTER);

        optionsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        optionsPanel.setOpaque(false);
        center.add(optionsPanel, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);

        JPanel controls = new JPanel(new BorderLayout());
        controls.setOpaque(false);

        JTextField topicF = new JTextField(currentTopic);
        JButton startBtn = styleBtn("Start / Next Question");
        startBtn.addActionListener(e -> {
            currentTopic = topicF.getText();
            loadQuestion();
        });

        controls.add(new JLabel("Topic: "), BorderLayout.WEST);
        controls.add(topicF, BorderLayout.CENTER);
        controls.add(startBtn, BorderLayout.EAST);

        statusLbl = new JLabel(" ");
        statusLbl.setFont(new Font("SansSerif", Font.ITALIC, 14));
        
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(controls, BorderLayout.NORTH);
        south.add(statusLbl, BorderLayout.SOUTH);
        
        add(south, BorderLayout.SOUTH);
    }

    private void loadQuestion() {
        statusLbl.setText("Generating question for topic: " + currentTopic + "...");
        optionsPanel.removeAll();
        revalidate(); repaint();

        new Thread(() -> {
            String prompt = "Generate a multiple choice question about [" + currentTopic + "]. " +
                "STRICT FORMAT: Question | Option A | Option B | Option C | Option D | Correct Letter (A, B, C, or D)\n" +
                "EXAMPLE: What is 2+2? | 3 | 4 | 5 | 6 | B\n" +
                "Include exactly 5 pipe symbols. Return ONLY the pipe-separated line.";
            String resp = ai.AIService.ask(prompt);
            
            SwingUtilities.invokeLater(() -> {
                try {
                    String[] parts = resp.split("\\|");
                    if (parts.length < 6) throw new Exception("Invalid format");
                    
                    questionArea.setText(parts[0].trim());
                    correctAnswer = parts[5].trim().toUpperCase();
                    
                    for (int i = 1; i <= 4; i++) {
                        String optText = parts[i].trim();
                        String letter = String.valueOf((char)('A' + i - 1));
                        
                        // Fix "A)A)" duplication: if it already starts with "A)", don't add it again
                        String prefix = letter + ")";
                        if (!optText.toUpperCase().startsWith(prefix)) {
                            optText = prefix + " " + optText;
                        }

                        JButton btn = styleBtn(optText);
                        String finalLetter = letter;
                        btn.addActionListener(ae -> {
                            // Extract letter from button text or use finalLetter
                            if (finalLetter.equals(correctAnswer)) {
                                statusLbl.setText("CORRECT! Well done.");
                                btn.setBackground(new Color(0, 120, 0));
                            } else {
                                statusLbl.setText("INCORRECT. The correct answer was " + correctAnswer);
                                btn.setBackground(new Color(150, 0, 0));
                            }
                        });
                        optionsPanel.add(btn);
                    }
                    statusLbl.setText("Question Loaded.");
                } catch (Exception e) {
                    questionArea.setText("AI Error: " + resp);
                    statusLbl.setText("Failed to parse AI response.");
                }
                revalidate(); repaint();
            });
        }).start();
    }

    private JButton styleBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(Color.BLACK);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        return b;
    }
}
