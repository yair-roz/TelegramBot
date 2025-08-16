package org.example;

import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatGPTService {
    private static final String ENDPOINT = "https://app.seker.live/fm1/send-message";
    private static final String SENDER_ID = "216336883";
    private final OkHttpClient client = new OkHttpClient();

    public List<Survey.Question> generateSurveyFromTopic(String topic, int questionCount) throws IOException {
        String prompt = "צור " + questionCount + " שאלות סקר עם בחירה מרובה בנושא: " + topic + ". " +
                "כל שאלה צריכה לכלול בדיוק 3 תשובות אפשריות. פורמט:\n" +
                "ש: <שאלה>\nת: <תשובה1>, <תשובה2>, <תשובה3>";

        String url = ENDPOINT + "?id=" + SENDER_ID + "&text=" + prompt;

        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();
        JSONObject json = new JSONObject(response.body().string());

        String raw = json.getString("extra");
        return parseQuestions(raw);
    }

    private List<Survey.Question> parseQuestions(String content) {
        List<Survey.Question> questions = new ArrayList<>();
        String[] blocks = content.split("ש:");

        for (String block : blocks) {
            if (block.trim().isEmpty()) continue;
            String[] parts = block.split("ת:");
            if (parts.length != 2) continue;

            String question = parts[0].trim();
            String[] options = parts[1].split(",");

            List<String> trimmed = new ArrayList<>();
            for (String opt : options) {
                opt = opt.trim();
                if (!opt.isEmpty()) trimmed.add(opt);
            }
            if (!question.isEmpty() && trimmed.size() == 3) {
                questions.add(new Survey.Question(question, trimmed));
            }
        }
        return questions;
    }
}
