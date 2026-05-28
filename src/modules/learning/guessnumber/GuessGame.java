package modules.learning.guessnumber;

import java.util.Random;

public class GuessGame {

    private int targetNumber;
    private int attempts;
    private int maxRange;
    private boolean isGameOver;
    private String lastFeedback;

    public enum Difficulty {
        EASY(50), MEDIUM(100), HARD(500);
        
        private final int range;
        Difficulty(int range) { this.range = range; }
        public int getRange() { return range; }
    }

    public GuessGame() {
        startNewGame(Difficulty.MEDIUM);
    }

    public void startNewGame(Difficulty difficulty) {
        Random rand = new Random();
        this.maxRange = difficulty.getRange();
        this.targetNumber = rand.nextInt(maxRange) + 1;
        this.attempts = 0;
        this.isGameOver = false;
        this.lastFeedback = "Waiting for your first guess...";
    }

    public boolean processGuess(int guess) {
        attempts++;
        if (guess == targetNumber) {
            isGameOver = true;
            lastFeedback = "Correct! It took you " + attempts + " attempts.";
            return true;
        } else if (guess < targetNumber) {
            lastFeedback = "Higher! (Attempts: " + attempts + ")";
        } else {
            lastFeedback = "Lower! (Attempts: " + attempts + ")";
        }
        return false;
    }

    public String getLastFeedback() { return lastFeedback; }
    public int getAttempts() { return attempts; }
    public int getMaxRange() { return maxRange; }
    public boolean isGameOver() { return isGameOver; }
}
