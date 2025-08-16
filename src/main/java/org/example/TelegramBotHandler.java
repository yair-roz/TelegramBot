package org.example;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.util.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class TelegramBotHandler extends TelegramLongPollingBot {
    private final CommunityManager communityManager;
    private final SurveyManager surveyManager;
    private final AtomicBoolean isSendingSurvey = new AtomicBoolean(false);

    public TelegramBotHandler(CommunityManager cm, SurveyManager sm) {
        this.communityManager = cm;
        this.surveyManager = sm;
    }

    @Override
    public void clearWebhook() {
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message msg = update.getMessage();
            long chatId = msg.getChatId();
            String text = msg.getText().trim().toLowerCase();

            if (text.equals("start") || text.equals("היי") || text.equals("hi")) {
                boolean added = communityManager.addMember(chatId, msg.getFrom().getFirstName());
                if (added) {
                    broadcastJoin(chatId);
                    sendText(chatId, "ברוך הבא לקהילה!");
                } else {
                    sendText(chatId, "אתה כבר חבר בקהילה");
                }
            }
        } else if (update.hasCallbackQuery()) {
            handleVote(update.getCallbackQuery());
        }
    }

    private void broadcastJoin(long newUserId) {
        String name = communityManager.getMemberName(newUserId);
        String msg = name + " הצטרף לקהילה! כעת יש: " + communityManager.getSize() + " חברים";
        for (Long id : communityManager.getAllMemberIds()) {
            if (!id.equals(newUserId)) sendText(id, msg);
        }
    }

    private void handleVote(CallbackQuery callback) {
        long userId = callback.getFrom().getId();
        String data = callback.getData();
        long chatId = callback.getMessage().getChatId();
        Survey survey = surveyManager.getActiveSurvey();

        if (survey == null || !survey.isOpen()) {
            sendText(chatId, "אין כרגע סקר פעיל");
            return;
        }

        String[] parts = data.split("_");
        if (parts.length != 2) {
            sendText(chatId, "בחירה לא תקינה");
            return;
        }

        int qIdx = Integer.parseInt(parts[0]);
        int optIdx = Integer.parseInt(parts[1]);

        if (qIdx < 0 || qIdx >= survey.questions.size()) {
            sendText(chatId, "שאלה לא קיימת");
            return;
        }

        Survey.Question question = survey.questions.get(qIdx);

        if (!question.canVote(userId)) {
            sendText(chatId, "כבר הצבעת על שאלה זו");
            return;
        }

        String selected = question.options.get(optIdx);
        question.vote(selected);
        question.markVoted(userId);
        survey.markRespondent(userId);
        sendText(chatId, "הצבעת בהצלחה!");

        surveyManager.notifyVoteUpdated(survey);

        if (survey.allAnswered()) {
            surveyManager.closeSurvey();
            sendResults(survey);
        }
    }

    public void sendSurveyToAllMembers(Survey survey) {
        if (!isSendingSurvey.compareAndSet(false, true)) {
            System.err.println("Attempted to send a survey while another one is in progress.");
            return;
        }

        for (Long userId : communityManager.getAllMemberIds()) {
            for (int i = 0; i < survey.questions.size(); i++) {
                Survey.Question q = survey.questions.get(i);
                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                for (int j = 0; j < q.options.size(); j++) {
                    InlineKeyboardButton btn = new InlineKeyboardButton();
                    btn.setText(q.options.get(j));
                    btn.setCallbackData(i + "_" + j);
                    rows.add(Collections.singletonList(btn));
                }

                markup.setKeyboard(rows);
                sendKeyboard(userId, q.text, markup);
            }
        }

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (surveyManager.hasActiveSurvey() && survey.isOpen()) {
                    surveyManager.closeSurvey();
                    sendResults(survey);
                }
                isSendingSurvey.set(false);
            }
        }, 5 * 60 * 1000L);
    }

    private void sendResults(Survey survey) {
        StringBuilder sb = new StringBuilder("📊 *תוצאות הסקר:*\n\n");

        for (int i = 0; i < survey.questions.size(); i++) {
            Survey.Question q = survey.questions.get(i);
            sb.append("שאלה ").append(i + 1).append(": ").append(q.text).append("\n");

            Map<String, Integer> results = q.getResults();
            int totalVotes = results.values().stream().mapToInt(Integer::intValue).sum();

            for (Map.Entry<String, Integer> entry : results.entrySet()) {
                String option = entry.getKey();
                int count = entry.getValue();
                double percent = totalVotes == 0 ? 0 : (count * 100.0 / totalVotes);
                sb.append(String.format(" - %s: %d (%.1f%%)\n", option, count, percent));
            }
            sb.append("\n");
        }

        sendText(survey.creatorId, sb.toString());
    }

    private void sendText(long chatId, String text) {
        try {
            execute(SendMessage.builder()
                    .chatId(String.valueOf(chatId))
                    .text(text)
                    .parseMode("Markdown")
                    .build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendKeyboard(long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage msg = new SendMessage(String.valueOf(chatId), text);
        msg.setReplyMarkup(keyboard);
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return "Survey2026Bot";
    }

    @Override
    public String getBotToken() {
        return "7589808634:AAHwexFlz6BGBAp1BfNnX5foxzXFI5Uiz3Q";
    }
}