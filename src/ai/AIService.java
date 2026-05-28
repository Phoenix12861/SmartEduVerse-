package ai;

import database.DatabaseManager;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class AIService {

    public static String ask(String prompt) {
        String[] settings = DatabaseManager.getAISettings();
        String provider = settings[0];
        String groqKey = settings[1];

        if ("GROQ".equalsIgnoreCase(provider)) {
            return askGroq(groqKey, prompt);
        } else {
            return OllamaClient.ask("phi3", prompt);
        }
    }

    private static String askGroq(String apiKey, String prompt) {
        if (apiKey == null || apiKey.isEmpty()) return "Groq API Key missing in settings.";
        try {
            URL url = new URL("https://api.groq.com/openai/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);

            String escapedPrompt = escapeJson(prompt);
            String json = "{\"model\": \"llama-3.1-8b-instant\", \"messages\": [{\"role\": \"user\", \"content\": \"" + escapedPrompt + "\"}]}";
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes("utf-8"));
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                return parseError(conn);
            }

            return parseOpenAIResponse(conn);
        } catch (Exception e) {
            return "Groq Connection Error: " + e.getMessage();
        }
    }

    private static String parseOpenAIResponse(HttpURLConnection conn) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) response.append(line.trim());
        br.close();

        String res = response.toString();
        int contentKeyIndex = res.indexOf("\"content\"");
        if (contentKeyIndex == -1) return "AI Error: Unexpected Response Format.";
        
        int colonIndex = res.indexOf(":", contentKeyIndex);
        int start = res.indexOf("\"", colonIndex);
        start++; 
        
        int end = res.indexOf("\"", start);
        while (end != -1 && res.charAt(end - 1) == '\\') {
            end = res.indexOf("\"", end + 1);
        }
        
        String content = res.substring(start, end);
        return content.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String parseError(HttpURLConnection conn) throws IOException {
        InputStream es = conn.getErrorStream();
        if (es == null) return "AI Error: Unknown error (Code " + conn.getResponseCode() + ")";
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(es, "utf-8"));
        StringBuilder errorResponse = new StringBuilder();
        String line;
        while ((line = errorReader.readLine()) != null) errorResponse.append(line.trim());
        return "AI API Error (" + conn.getResponseCode() + "): " + errorResponse.toString();
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
