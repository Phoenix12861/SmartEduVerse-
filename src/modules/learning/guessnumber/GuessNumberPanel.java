package modules.learning.guessnumber;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class GuessNumberPanel extends JPanel {

    private final GuessGame game;
    private JTextField inputField;
    private JLabel feedbackLabel;
    private JLabel rangeLabel;
    private JLabel attemptsLabel;
    private JButton submitBtn;
    private JComboBox<GuessGame.Difficulty> difficultyBox;

    public GuessNumberPanel() {
        game = new GuessGame();
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 247));
        setBorder(new EmptyBorder(30, 40, 30, 40));


        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Guess The Number");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
        title.setForeground(new Color(33, 33, 33));
        header.add(title, BorderLayout.WEST);

        difficultyBox = new JComboBox<>(GuessGame.Difficulty.values());
        difficultyBox.setSelectedItem(GuessGame.Difficulty.MEDIUM);
        styleCombo(difficultyBox);
        difficultyBox.addActionListener(e -> restartGame());
        header.add(difficultyBox, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);


        JPanel mainCard = new JPanel();
        mainCard.setLayout(new BoxLayout(mainCard, BoxLayout.Y_AXIS));
        mainCard.setBackground(Color.WHITE);
        mainCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                new EmptyBorder(40, 40, 40, 40)
        ));

        rangeLabel = new JLabel("I'm thinking of a number between 1 and " + game.getMaxRange());
        rangeLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        rangeLabel.setForeground(new Color(100, 100, 100));
        rangeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        inputField = new JTextField();
        inputField.setFont(new Font(Font.MONOSPACED, Font.BOLD, 48));
        inputField.setHorizontalAlignment(JTextField.CENTER);
        inputField.setMaximumSize(new Dimension(200, 80));
        inputField.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, new Color(200, 200, 200)));
        
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleGuess();
                }
            }
        });

        feedbackLabel = new JLabel(game.getLastFeedback());
        feedbackLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        feedbackLabel.setForeground(new Color(60, 60, 60));
        feedbackLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        submitBtn = new JButton("Submit Guess");
        styleButton(submitBtn, new Color(0, 122, 255));
        submitBtn.addActionListener(e -> handleGuess());
        submitBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        attemptsLabel = new JLabel("Attempts: 0");
        attemptsLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        attemptsLabel.setForeground(new Color(150, 150, 150));
        attemptsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainCard.add(rangeLabel);
        mainCard.add(Box.createVerticalStrut(30));
        mainCard.add(inputField);
        mainCard.add(Box.createVerticalStrut(30));
        mainCard.add(feedbackLabel);
        mainCard.add(Box.createVerticalStrut(40));
        mainCard.add(submitBtn);
        mainCard.add(Box.createVerticalStrut(20));
        mainCard.add(attemptsLabel);

        add(mainCard, BorderLayout.CENTER);


        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setOpaque(false);
        JButton resetBtn = new JButton("Reset Game");
        styleButton(resetBtn, new Color(50, 50, 50));
        resetBtn.addActionListener(e -> restartGame());
        footer.add(resetBtn);
        add(footer, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(() -> inputField.requestFocusInWindow());
    }

    private void handleGuess() {
        if (game.isGameOver()) return;

        try {
            int guess = Integer.parseInt(inputField.getText().trim());
            boolean won = game.processGuess(guess);
            
            feedbackLabel.setText(game.getLastFeedback());
            attemptsLabel.setText("Attempts: " + game.getAttempts());
            inputField.setText("");
            
            if (won) {
                feedbackLabel.setForeground(new Color(40, 180, 99));
                inputField.setEditable(false);
                submitBtn.setEnabled(false);
                showWinEffect();
            } else {
                feedbackLabel.setForeground(new Color(231, 76, 60));
            }
        } catch (NumberFormatException e) {
            feedbackLabel.setText("Please enter a valid number!");
            feedbackLabel.setForeground(Color.ORANGE);
        }
        inputField.requestFocusInWindow();
    }

    private void restartGame() {
        GuessGame.Difficulty diff = (GuessGame.Difficulty) difficultyBox.getSelectedItem();
        game.startNewGame(diff);
        
        rangeLabel.setText("I'm thinking of a number between 1 and " + game.getMaxRange());
        feedbackLabel.setText(game.getLastFeedback());
        feedbackLabel.setForeground(new Color(60, 60, 60));
        attemptsLabel.setText("Attempts: 0");
        inputField.setText("");
        inputField.setEditable(true);
        submitBtn.setEnabled(true);
        inputField.requestFocusInWindow();
    }

    private void showWinEffect() {
        Timer timer = new Timer(500, e -> {
            feedbackLabel.setVisible(!feedbackLabel.isVisible());
        });
        timer.setRepeats(true);
        timer.start();
        

        new Timer(3000, e -> {
            timer.stop();
            feedbackLabel.setVisible(true);
        }).start();
    }

    private void styleButton(JButton b, Color bg) {
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(12, 30, 12, 30));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void styleCombo(JComboBox<?> cb) {
        cb.setBackground(Color.WHITE);
        cb.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        cb.setPreferredSize(new Dimension(120, 35));
    }
}
