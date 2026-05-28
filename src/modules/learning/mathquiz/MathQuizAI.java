package modules.learning.mathquiz;

import ai.OllamaClient;

public class MathQuizAI {

    public static String getChallenge(int level) {
        String prompt = "You are a math tutor. Generate a challenging math question for a student at level " + level + ". " +
            "Return only the question text. Do not provide the answer yet. Be professional.";
        return ai.AIService.ask(prompt);
    }

    public static boolean checkAnswer(String question, String answer) {
        String prompt = "Context: Math Question is [" + question + "]. Student answered [" + answer + "]. " +
            "Respond only with 'CORRECT' or 'INCORRECT'.";
        String resp = ai.AIService.ask(prompt);
        return resp.toUpperCase().contains("CORRECT") && !resp.toUpperCase().contains("INCORRECT");
    }

    public static String getFeedback(String question, String answer) {
        String prompt = "Context: Math Question is [" + question + "]. Student answered [" + answer + "]. " +
            "Provide a brief, professional explanation of the correct solution and why the student's answer was correct or incorrect.";
        return ai.AIService.ask(prompt);
    }
}
