package modules.utility.numbersystem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;

public class NumberSystemPanel extends JPanel {

    private final NumberSystemEngine engine;
    private JTextField inputField;

    private JLabel indianLabel;
    private JLabel internationalLabel;
    private JLabel binaryLabel;
    private JLabel octalLabel;
    private JLabel hexLabel;
    private JLabel romanLabel;
    private JLabel asciiLabel;
    private JTextArea wordsArea;

    public NumberSystemPanel() {
        engine = new NumberSystemEngine();
        
        setLayout(new BorderLayout());
        setBackground(new Color(240, 240, 240));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // ================= TITLE =================
        JLabel title = new JLabel("Number System Explorer");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        title.setBorder(new EmptyBorder(0, 0, 15, 0));
        add(title, BorderLayout.NORTH);

        // ================= MAIN CARD =================
        JPanel card = new JPanel(new BorderLayout(0, 20));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                new EmptyBorder(25, 25, 25, 25)
        ));

        // 1. TOP SECTION (Input)
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.setOpaque(false);

        JLabel inputLabel = createSectionTitle("Enter Number");
        inputField = new JTextField();
        inputField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        inputField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                new EmptyBorder(8, 12, 8, 12)
        ));

        inputPanel.add(inputLabel);
        inputPanel.add(Box.createVerticalStrut(8));
        inputPanel.add(inputField);
        
        card.add(inputPanel, BorderLayout.NORTH);

        // 2. CENTER SECTION (Two Column Grid)
        JPanel gridPanel = new JPanel(new GridLayout(4, 2, 25, 15));
        gridPanel.setOpaque(false);

        indianLabel = createOutputLabel();
        internationalLabel = createOutputLabel();
        binaryLabel = createOutputLabel();
        octalLabel = createOutputLabel();
        hexLabel = createOutputLabel();
        romanLabel = createOutputLabel();
        asciiLabel = createOutputLabel();

        gridPanel.add(createGridItem("Indian Format", indianLabel));
        gridPanel.add(createGridItem("International Format", internationalLabel));
        gridPanel.add(createGridItem("Binary (Base 2)", binaryLabel));
        gridPanel.add(createGridItem("Octal (Base 8)", octalLabel));
        gridPanel.add(createGridItem("Hexadecimal (Base 16)", hexLabel));
        gridPanel.add(createGridItem("Roman Numerals", romanLabel));
        gridPanel.add(createGridItem("ASCII Character", asciiLabel));
        gridPanel.add(new JPanel() {{ setOpaque(false); }}); // Placeholder for 8th slot

        // 3. BOTTOM SECTION (Words + Invisible Box)
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);

        wordsArea = new JTextArea();
        wordsArea.setEditable(false);
        wordsArea.setLineWrap(true);
        wordsArea.setWrapStyleWord(true);
        wordsArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        wordsArea.setBackground(new Color(248, 248, 248));
        wordsArea.setBorder(new EmptyBorder(15, 15, 15, 15));

        bottomPanel.add(createSectionTitle("In Words"));
        bottomPanel.add(Box.createVerticalStrut(8));
        bottomPanel.add(wordsArea);
        
        // The requested invisible box at the very bottom
        bottomPanel.add(Box.createVerticalStrut(50));

        JPanel centerContent = new JPanel(new BorderLayout(0, 20));
        centerContent.setOpaque(false);
        centerContent.add(gridPanel, BorderLayout.NORTH);
        centerContent.add(bottomPanel, BorderLayout.CENTER);

        card.add(centerContent, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        // ================= LIVE UPDATE =================
        inputField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateConversion(); }
            public void removeUpdate(DocumentEvent e) { updateConversion(); }
            public void changedUpdate(DocumentEvent e) { updateConversion(); }
        });
    }

    private JPanel createGridItem(String title, JComponent component) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        p.add(createSectionTitle(title), BorderLayout.NORTH);
        p.add(component, BorderLayout.CENTER);
        return p;
    }

    private void updateConversion() {
        try {
            String text = inputField.getText().replace(",", "").trim();
            if (text.isEmpty()) {
                clearFields();
                return;
            }

            long number = Long.parseLong(text);

            internationalLabel.setText(NumberFormat.getNumberInstance(Locale.US).format(number));
            indianLabel.setText(formatIndian(number));
            binaryLabel.setText(engine.decimalToBinary(number));
            octalLabel.setText(engine.decimalToOctal(number));
            hexLabel.setText("0x" + engine.decimalToHex(number));
            romanLabel.setText(engine.toRoman(number));
            asciiLabel.setText(engine.toAsciiString(number));
            wordsArea.setText(engine.convertToWords(number));

        } catch (NumberFormatException e) {
            clearFields();
            if (!inputField.getText().isEmpty()) {
                wordsArea.setText("Error: Please enter a valid whole number.");
            }
        } catch (Exception e) {
            clearFields();
        }
    }

    private void clearFields() {
        indianLabel.setText("--");
        internationalLabel.setText("--");
        binaryLabel.setText("--");
        octalLabel.setText("--");
        hexLabel.setText("--");
        romanLabel.setText("--");
        asciiLabel.setText("--");
        wordsArea.setText("");
    }

    private String formatIndian(long num) {
        String s = String.valueOf(num);
        if (s.length() <= 3) return s;
        String last3 = s.substring(s.length() - 3);
        String remaining = s.substring(0, s.length() - 3);
        StringBuilder builder = new StringBuilder();
        while (remaining.length() > 2) {
            builder.insert(0, "," + remaining.substring(remaining.length() - 2));
            remaining = remaining.substring(0, remaining.length() - 2);
        }
        builder.insert(0, remaining);
        return builder + "," + last3;
    }

    private JLabel createSectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        return label;
    }

    private JLabel createOutputLabel() {
        JLabel label = new JLabel("--");
        label.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 18));
        label.setOpaque(true);
        label.setBackground(new Color(248, 248, 248));
        label.setBorder(new EmptyBorder(10, 15, 10, 15));
        return label;
    }
}
