package modules.learning.typingspeed;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class TypingEngine {

    public enum Mode { TIME, WORDS, QUOTE, CODE }
    public enum Difficulty { EASY, MEDIUM, HARD, PROGRAMMING }

    private final String[] easyWords = {
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i", "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her", "she", "or", "an", "will", "my", "one", "all", "would", "there", "their", "what"
    };

    private final String[] mediumWords = {
        "keyboard", "science", "education", "internet", "software", "monitor", "database", "security", "network", "program", "laptop", "wireless",
        "system", "logic", "memory", "storage", "binary", "compiler", "variable", "function", "object", "class", "method", "import", "package",
        "public", "private", "static", "final", "string", "character", "integer", "boolean", "double", "float", "array", "collection", "stream", "thread"
    };

    private final String[] hardWords = {
        "authentication", "artificial", "microprocessor", "cybersecurity", "infrastructure", "synchronization", "multithreading", "virtualization",
        "polymorphism", "encapsulation", "inheritance", "abstraction", "asynchronous", "concurrency", "distributed", "scalability", "architecture",
        "deployment", "optimization", "refactoring", "documentation", "integration", "repository", "dependency", "framework", "middleware", "algorithm",
        "bandwidth", "encryption", "cryptography", "containerization", "microservices", "orchestration"
    };

    private final String[] codeSnippets = {
        "public static void main(String[] args)", "System.out.println(\"Hello World\");", "for (int i = 0; i < list.size(); i++)",
        "if (condition != null && condition.isValid())", "List<String> result = new ArrayList<>();", "return result.stream().filter(s -> s.startsWith(\"A\")).collect(Collectors.toList());",
        "try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }", "Map<Integer, String> map = new HashMap<>();",
        "Optional<User> user = userRepository.findById(id);", "@Override public String toString() { return super.toString(); }",
        "import java.util.stream.*;", "private final List<String> data = new ArrayList<>();"
    };

    private final String[] quotes = {
        "The only way to do great work is to love what you do.",
        "Innovation distinguishes between a leader and a follower.",
        "Your time is limited, so don't waste it living someone else's life.",
        "Stay hungry, stay foolish.",
        "The future belongs to those who believe in the beauty of their dreams.",
        "Success is not final, failure is not fatal: it is the courage to continue that counts.",
        "Believe you can and you're halfway there."
    };

    private List<String> targetWords = new ArrayList<>();
    private List<String> typedWords = new ArrayList<>();
    private int currentWordIndex = 0;
    private StringBuilder currentWordInput = new StringBuilder();
    
    private long startTime;
    private long endTime;
    private boolean isRunning = false;
    private boolean isPrepared = false;

    private int totalTypedChars = 0;
    private int totalCorrectChars = 0;
    private int totalIncorrectChars = 0;

    // Prepare the test (generate words but don't start the clock)
    public void prepareTest(Mode mode, Difficulty diff, int limit) {
        generateContent(mode, diff, limit);
        typedWords = new ArrayList<>(Collections.nCopies(targetWords.size(), ""));
        currentWordIndex = 0;
        currentWordInput = new StringBuilder();
        totalTypedChars = totalCorrectChars = totalIncorrectChars = 0;
        isRunning = false;
        isPrepared = true;
    }

    // Start the clock
    public void startTest() {
        if (!isPrepared) return;
        startTime = System.currentTimeMillis();
        isRunning = true;
    }

    public void stopTest() {
        if (!isRunning) return;
        endTime = System.currentTimeMillis();
        isRunning = false;
        isPrepared = false;
    }

    private void generateContent(Mode mode, Difficulty diff, int limit) {
        targetWords.clear();
        Random r = new Random();
        
        if (mode == Mode.QUOTE) {
            String q = quotes[r.nextInt(quotes.length)];
            targetWords.addAll(Arrays.asList(q.split(" ")));
        } else if (mode == Mode.CODE) {
            String c = codeSnippets[r.nextInt(codeSnippets.length)];
            targetWords.addAll(Arrays.asList(c.split(" ")));
        } else {
            String[] src = (diff == Difficulty.HARD) ? hardWords : (diff == Difficulty.MEDIUM) ? mediumWords : (diff == Difficulty.PROGRAMMING) ? mediumWords : easyWords;
            for (int i = 0; i < limit; i++) {
                targetWords.add(src[r.nextInt(src.length)]);
            }
        }
    }

    public void processKey(char c) {
        if (!isRunning) return;

        if (c == ' ') {
            typedWords.set(currentWordIndex, currentWordInput.toString());
            if (currentWordIndex < targetWords.size() - 1) {
                currentWordIndex++;
                currentWordInput = new StringBuilder();
            } else {
                stopTest();
            }
        } else if (c == '\b') {
            if (currentWordInput.length() > 0) {
                currentWordInput.deleteCharAt(currentWordInput.length() - 1);
            } else if (currentWordIndex > 0) {
                currentWordIndex--;
                currentWordInput = new StringBuilder(typedWords.get(currentWordIndex));
            }
        } else if (c != '\uFFFF' && !Character.isISOControl(c)) {
            currentWordInput.append(c);
            totalTypedChars++;
        }
        
        calculateLiveStats();
    }

    private void calculateLiveStats() {
        int correct = 0;
        int incorrect = 0;
        
        for (int i = 0; i < currentWordIndex; i++) {
            String t = targetWords.get(i);
            String p = typedWords.get(i);
            for (int j = 0; j < Math.max(t.length(), p.length()); j++) {
                if (j < t.length() && j < p.length()) {
                    if (t.charAt(j) == p.charAt(j)) correct++;
                    else incorrect++;
                } else {
                    incorrect++;
                }
            }
        }
        
        String ct = targetWords.get(currentWordIndex);
        String cp = currentWordInput.toString();
        for (int j = 0; j < cp.length(); j++) {
            if (j < ct.length()) {
                if (ct.charAt(j) == cp.charAt(j)) correct++;
                else incorrect++;
            } else {
                incorrect++;
            }
        }
        
        totalCorrectChars = correct;
        totalIncorrectChars = incorrect;
    }

    public int getNetWPM() {
        long now = isRunning ? System.currentTimeMillis() : endTime;
        long elapsed = now - startTime;
        if (elapsed <= 1000) return 0;
        double minutes = elapsed / 60000.0;
        return (int) Math.round((totalCorrectChars / 5.0) / minutes);
    }

    public int getRawWPM() {
        long now = isRunning ? System.currentTimeMillis() : endTime;
        long elapsed = now - startTime;
        if (elapsed <= 1000) return 0;
        double minutes = elapsed / 60000.0;
        return (int) Math.round((totalTypedChars / 5.0) / minutes);
    }

    public int getAccuracy() {
        if (totalTypedChars == 0) return 100;
        int total = totalCorrectChars + totalIncorrectChars;
        if (total == 0) return 100;
        return (int) Math.round(((double) totalCorrectChars / total) * 100);
    }

    public int getTotalCorrectChars() { return totalCorrectChars; }
    public int getTotalIncorrectChars() { return totalIncorrectChars; }
    public int getTotalTypedChars() { return totalTypedChars; }
    public List<String> getTargetWords() { return targetWords; }
    public List<String> getTypedWords() { return typedWords; }
    public String getCurrentTyped() { return currentWordInput.toString(); }
    public int getCurrentWordIndex() { return currentWordIndex; }
    public boolean isRunning() { return isRunning; }
    public boolean isPrepared() { return isPrepared; }
}
