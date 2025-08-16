package org.example;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {

        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("FlatLaf".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        CommunityManager cm = new CommunityManager();
        cm.loadMembers();
        CommunityManagerHolder.setInstance(cm);
        SurveyManager sm = new SurveyManager();

        try {
            TelegramBotHandler bot = new TelegramBotHandler(cm, sm);

            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            try {
                api.registerBot(bot);
            } catch (TelegramApiRequestException ex) {
                if (ex.getErrorCode() != 401) throw ex;
                System.err.println("Webhook removal failed (401), continuing with Long Polling.");
            }

            long creatorId = 123456789L;

            SwingUtilities.invokeLater(() -> new SurveyUI(sm, cm, bot, creatorId).launch());

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    " שגיאה בהרצת הבוט:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
