package ai;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class OllamaClient {

    private static final String API_URL =
            "http://localhost:11434/api/generate";

    public static String ask(
            String model,
            String prompt
    ) {

        try {

            URL url =
                    new URL(API_URL);

            HttpURLConnection conn =
                    (HttpURLConnection)
                            url.openConnection();

            conn.setRequestMethod("POST");

            conn.setRequestProperty(
                    "Content-Type",
                    "application/json"
            );

            conn.setDoOutput(true);

            // ================= JSON =================

            String json =
                    """
                    {
                      "model":"%s",
                      "prompt":"%s",
                      "stream":false
                    }
                    """
                    .formatted(
                            model,
                            prompt.replace("\"","\\\"")
                    );

            // ================= SEND =================

            try(
                    OutputStream os =
                            conn.getOutputStream()
            ) {

                byte[] input =
                        json.getBytes("utf-8");

                os.write(input,0,input.length);
            }

            // ================= READ =================

            BufferedReader br =
                    new BufferedReader(
                            new InputStreamReader(
                                    conn.getInputStream(),
                                    "utf-8"
                            )
                    );

            StringBuilder response =
                    new StringBuilder();

            String line;

            while(
                    (line = br.readLine()) != null
            ) {

                response.append(line.trim());
            }

            br.close();

            // crude extraction

            String result =
                    response.toString();

            int start =
                    result.indexOf("\"response\":\"");

            if(start == -1)
                return "No response";

            start += 12;

            int end =
                    result.indexOf(
                            "\",\"done\""
                            ,
                            start
                    );

            if(end == -1)
                return result;

            return result
                    .substring(start,end)
                    .replace("\\n","\n")
                    .replace("\\\"", "\"");

        } catch (Exception e) {

            e.printStackTrace();

            return "AI Error";
        }
    }
}
