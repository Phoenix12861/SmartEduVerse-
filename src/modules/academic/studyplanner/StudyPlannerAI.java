package modules.academic.studyplanner;

import ai.OllamaClient;
import core.SessionManager;
import modules.academic.resultanalyzer.ResultAnalyzer;
import java.util.List;
import java.util.stream.Collectors;

public class StudyPlannerAI {

    public static String getAnalysis(String username) {
        List<modules.academic.resultanalyzer.ResultAnalyzer.MarkEntry> results = modules.academic.resultanalyzer.ResultAnalyzer.getResults(username);
        List<modules.management.library.Book> books = modules.management.library.LibraryManager.getAllBooks("All");
        
        if (results.isEmpty()) {
            return "No academic records found. Please update your marks in the Result Analyzer module first.";
        }

        String marksSummary = results.stream()
            .map(r -> r.subject + ": " + r.marks + "/" + r.totalMarks)
            .collect(Collectors.joining(", "));
            
        String bookList = books.stream()
            .map(b -> b.title + " (Category: " + b.category + ")")
            .collect(Collectors.joining(", "));

        String prompt = "You are a professional educational advisor. Analyze these student marks: [" + marksSummary + "]. " +
            "Also, here is a list of available books in the library: [" + bookList + "]. " +
            "Suggest which modules in SmartEduVerse would be most beneficial for their improvement. " +
            "Additionally, recommend at least 2-3 specific books from the provided library list that would help them in their weak areas or enhance their strengths. " +
            "Be concise, professional, and use a structured format.";

        return ai.AIService.ask(prompt);
    }

    public static String chat(String username, String userMessage) {
        List<ResultAnalyzer.MarkEntry> results = ResultAnalyzer.getResults(username);
        String marksSummary = results.stream()
            .map(r -> r.subject + ": " + r.marks + "/" + r.totalMarks)
            .collect(Collectors.joining(", "));

        String prompt = "Context: Student is " + username + " with marks: [" + marksSummary + "]. " +
            "User says: " + userMessage + ". " +
            "Respond professionally as an educational consultant. Keep it concise and helpful.";

        return ai.AIService.ask(prompt);
    }
}
