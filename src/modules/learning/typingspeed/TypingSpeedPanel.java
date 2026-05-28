package modules.learning.typingspeed;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class TypingSpeedPanel extends JPanel {

    private final TypingEngine engine;
    private JTextPane displayPane;
    private JLabel timerLabel;
    private JComboBox<String> timeSelector;
    
    private boolean isDarkMode = true;
    private Timer testTimer;
    private int timeLeft = 30;
    private List<Integer> wpmHistory = new ArrayList<>();


    private Color bg;
    private Color mainText;
    private Color subText;
    private Color accent = new Color(232, 184, 30);
    private Color error = new Color(239, 68, 68);

    public TypingSpeedPanel() {
        engine = new TypingEngine();
        setLayout(new BorderLayout(20, 20));
        applyTheme();
        initUI();
    }

    private void applyTheme() {
        if (isDarkMode) {
            bg = new Color(30, 31, 34);
            mainText = new Color(209, 213, 219);
            subText = new Color(107, 114, 128);
        } else {
            bg = new Color(243, 244, 246);
            mainText = new Color(31, 41, 55);
            subText = new Color(107, 114, 128);
        }
    }

    private void initUI() {
        removeAll();
        setBackground(bg);
        setBorder(new EmptyBorder(40, 60, 40, 60));


        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftControls.setOpaque(false);

        JButton themeBtn = new JButton(isDarkMode ? "☀ Light" : "🌙 Dark");
        styleButton(themeBtn);
        themeBtn.addActionListener(e -> {
            isDarkMode = !isDarkMode;
            applyTheme();
            initUI();
        });

        JButton restartBtn = new JButton("↻ Restart");
        styleButton(restartBtn);
        restartBtn.addActionListener(e -> restartTest());

        leftControls.add(themeBtn);
        leftControls.add(restartBtn);
        header.add(leftControls, BorderLayout.WEST);


        JPanel centerHeader = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        centerHeader.setOpaque(false);
        JLabel timeIcon = new JLabel("🕒");
        timeIcon.setForeground(subText);
        
        timeSelector = new JComboBox<>(new String[]{"15", "30", "60", "120"});
        timeSelector.setSelectedItem(String.valueOf(timeLeft));
        timeSelector.setBackground(bg);
        timeSelector.setForeground(accent);
        timeSelector.setFocusable(false);
        timeSelector.setBorder(BorderFactory.createLineBorder(subText));
        timeSelector.addActionListener(e -> {
            timeLeft = Integer.parseInt(timeSelector.getSelectedItem().toString());
            restartTest();
        });

        centerHeader.add(timeIcon);
        centerHeader.add(timeSelector);
        header.add(centerHeader, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);


        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;


        timerLabel = new JLabel(timeLeft + "s");
        timerLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 48));
        timerLabel.setForeground(accent);
        timerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 0;
        centerPanel.add(timerLabel, gbc);


        displayPane = new JTextPane();
        displayPane.setEditable(false);
        displayPane.setBackground(bg);
        displayPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 28));
        displayPane.setCaretColor(accent);
        
        JPanel displayWrapper = new JPanel(new BorderLayout());
        displayWrapper.setBackground(bg);
        displayWrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(subText, 1),
            new EmptyBorder(15, 15, 15, 15)
        ));
        displayWrapper.add(displayPane);
        displayWrapper.setPreferredSize(new Dimension(850, 160));

        displayPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    restartTest();
                    e.consume();
                    return;
                }
                
                if (engine.isPrepared() && !engine.isRunning() && !Character.isISOControl(e.getKeyChar()) && e.getKeyCode() != KeyEvent.VK_SHIFT) {
                    startTest();
                }

                if (engine.isRunning()) {
                    engine.processKey(e.getKeyChar());
                    updateDisplay();
                }
            }
        });

        gbc.gridy = 1;
        gbc.insets = new Insets(30, 0, 0, 0);
        centerPanel.add(displayWrapper, gbc);

        add(centerPanel, BorderLayout.CENTER);


        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setOpaque(false);
        JLabel hint = new JLabel("press Tab to restart | Stats will show after test");
        hint.setForeground(subText);
        hint.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 14));
        footer.add(hint);
        add(footer, BorderLayout.SOUTH);


        engine.prepareTest(TypingEngine.Mode.WORDS, TypingEngine.Difficulty.MEDIUM, 100);
        updateDisplay();

        revalidate();
        repaint();
        SwingUtilities.invokeLater(() -> displayPane.requestFocusInWindow());
    }

    private void startTest() {
        wpmHistory.clear();
        timeLeft = Integer.parseInt(timeSelector.getSelectedItem().toString());
        timeSelector.setEnabled(false);
        
        engine.startTest();
        
        if (testTimer != null) testTimer.stop();
        testTimer = new Timer(1000, e -> {
            timeLeft--;
            timerLabel.setText(timeLeft + "s");
            
            wpmHistory.add(engine.getNetWPM());

            if (timeLeft <= 0) {
                testTimer.stop();
                engine.stopTest();
                showFinalResults();
            }
        });
        testTimer.start();
    }

    private void restartTest() {
        if (testTimer != null) testTimer.stop();
        engine.stopTest();
        timeLeft = Integer.parseInt(timeSelector.getSelectedItem().toString());
        initUI();
    }

    private void updateDisplay() {
        StyledDocument doc = displayPane.getStyledDocument();
        try { doc.remove(0, doc.getLength()); } catch (Exception e) {}

        List<String> targetWords = engine.getTargetWords();
        List<String> typedWords = engine.getTypedWords();
        String currentInput = engine.getCurrentTyped();
        int currentIdx = engine.getCurrentWordIndex();

        for (int i = 0; i < targetWords.size(); i++) {
            String target = targetWords.get(i);
            
            if (i < currentIdx) {
                String typed = typedWords.get(i);
                for (int j = 0; j < Math.max(target.length(), typed.length()); j++) {
                    if (j < target.length()) {
                        char tChar = target.charAt(j);
                        if (j < typed.length()) {
                            char pChar = typed.charAt(j);
                            appendStyled(doc, String.valueOf(pChar), pChar == tChar ? mainText : error);
                        } else {
                            appendStyled(doc, String.valueOf(tChar), error);
                        }
                    } else {
                        appendStyled(doc, String.valueOf(typed.charAt(j)), error.darker());
                    }
                }
                appendStyled(doc, " ", mainText);
            } else if (i == currentIdx) {
                for (int j = 0; j < Math.max(target.length(), currentInput.length()); j++) {
                    if (j < target.length()) {
                        char tChar = target.charAt(j);
                        if (j < currentInput.length()) {
                            char pChar = currentInput.charAt(j);
                            appendStyled(doc, String.valueOf(pChar), pChar == tChar ? mainText : error);
                        } else {
                            appendStyled(doc, String.valueOf(tChar), subText);
                        }
                    } else {
                        appendStyled(doc, String.valueOf(currentInput.charAt(j)), error.darker());
                    }
                }
                appendStyled(doc, " ", subText);
            } else {
                appendStyled(doc, target + " ", subText);
            }
        }

        try {
            int pos = 0;
            for(int i=0; i<currentIdx; i++) {
                pos += Math.max(targetWords.get(i).length(), typedWords.get(i).length()) + 1;
            }
            pos += currentInput.length();
            
            displayPane.setCaretPosition(pos);
            Rectangle rect = displayPane.modelToView2D(pos).getBounds();
            rect.y -= 50;
            rect.height += 100;
            displayPane.scrollRectToVisible(rect);
        } catch (Exception e) {}
    }

    private void appendStyled(StyledDocument doc, String text, Color c) {
        Style style = displayPane.addStyle("Style", null);
        StyleConstants.setForeground(style, c);
        try { doc.insertString(doc.getLength(), text, style); } catch (Exception e) {}
    }

    private void showFinalResults() {
        int finalWpm = engine.getNetWPM();
        int finalAcc = engine.getAccuracy();

        JPanel resultPanel = new JPanel(new BorderLayout(10, 20));
        resultPanel.setBackground(bg);
        resultPanel.setPreferredSize(new Dimension(500, 400));

        JLabel statsLabel = new JLabel(String.format("WPM: %d | Accuracy: %d%%", finalWpm, finalAcc));
        statsLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        statsLabel.setForeground(accent);
        statsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        resultPanel.add(statsLabel, BorderLayout.NORTH);

        JPanel graph = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth();
                int h = getHeight();
                
                g2.setColor(subText);
                g2.drawLine(40, h-40, w-20, h-40);
                g2.drawLine(40, 20, 40, h-40);

                if (wpmHistory.size() < 2) return;

                int maxWpm = wpmHistory.stream().max(Integer::compare).orElse(100);
                maxWpm = Math.max(maxWpm, 60);

                g2.setColor(accent);
                g2.setStroke(new BasicStroke(3f));

                for (int i = 0; i < wpmHistory.size() - 1; i++) {
                    int x1 = 40 + (i * (w - 60) / wpmHistory.size());
                    int y1 = (h - 40) - (wpmHistory.get(i) * (h - 60) / maxWpm);
                    int x2 = 40 + ((i + 1) * (w - 60) / wpmHistory.size());
                    int y2 = (h - 40) - (wpmHistory.get(i + 1) * (h - 60) / maxWpm);
                    g2.drawLine(x1, y1, x2, y2);
                }
            }
        };
        graph.setBackground(bg);
        resultPanel.add(graph, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(this, resultPanel, "Test Results", JOptionPane.PLAIN_MESSAGE);
        restartTest();
    }

    private void styleButton(JButton b) {
        b.setBackground(bg);
        b.setForeground(subText);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(subText, 1));
        b.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { b.setForeground(mainText); b.setBorder(BorderFactory.createLineBorder(mainText, 1)); }
            @Override
            public void mouseExited(MouseEvent e) { b.setForeground(subText); b.setBorder(BorderFactory.createLineBorder(subText, 1)); }
        });
    }
}