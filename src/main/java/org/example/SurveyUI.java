package org.example;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SurveyUI implements SurveyManager.SurveyClosedListener, SurveyManager.VoteUpdateListener {
    private final SurveyManager surveyManager;
    private final CommunityManager communityManager;
    private final TelegramBotHandler bot;
    private final long adminId;

    private final ManualSurveyPanel manualPanel;
    private final GptSurveyPanel gptPanel;
    private final ResultsPanel resultsPanel;
    private final LoadingPanel loadingPanel;

    private JFrame frame;
    private CardLayout cards;
    private JPanel mainPanel;

    public SurveyUI(SurveyManager sm, CommunityManager cm, TelegramBotHandler bot, long adminId) {
        this.surveyManager = sm;
        this.communityManager = cm;
        this.bot = bot;
        this.adminId = adminId;

        this.manualPanel = new ManualSurveyPanel();
        this.gptPanel = new GptSurveyPanel();
        this.resultsPanel = new ResultsPanel();
        this.loadingPanel = new LoadingPanel();

        sm.setSurveyClosedListener(this);
        sm.setVoteUpdateListener(this);
    }

    public void launch() {
        frame = new JFrame("POLL CREATOR");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setLocationRelativeTo(null);

        cards = new CardLayout();
        mainPanel = new JPanel(cards);

        mainPanel.add(createMenuPanel(), "MENU");
        mainPanel.add(manualPanel.getPanel(), "MANUAL");
        mainPanel.add(gptPanel.getPanel(), "GPT");
        mainPanel.add(loadingPanel.getPanel(), "LOADING");
        mainPanel.add(resultsPanel.getPanel(), "RESULTS");

        setupManualActions();
        setupGptActions();

        frame.setContentPane(mainPanel);
        frame.setVisible(true);
        cards.show(mainPanel, "MENU");
    }

    private JPanel createMenuPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        p.setBackground(Color.WHITE);

        JLabel title = new JLabel("POLL CREATOR", SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(new Font("Arial", Font.BOLD, 28));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        title.setForeground(new Color(34, 34, 34));

        JButton manualBtn = new JButton("צור סקר ידני");
        JButton gptBtn = new JButton("צור סקר מ-GPT");
        Dimension btnSize = new Dimension(240, 48);

        for (JButton btn : new JButton[]{manualBtn, gptBtn}) {
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setMaximumSize(btnSize);
            btn.setPreferredSize(btnSize);
            btn.setFont(btn.getFont().deriveFont(16f));
            btn.setFocusable(false);
            btn.setMargin(new Insets(8, 14, 8, 14));
        }

        manualBtn.addActionListener(e -> cards.show(mainPanel, "MANUAL"));
        gptBtn.addActionListener(e -> cards.show(mainPanel, "GPT"));

        p.add(Box.createVerticalGlue());
        p.add(title);
        p.add(Box.createRigidArea(new Dimension(0, 12)));
        p.add(manualBtn);
        p.add(Box.createRigidArea(new Dimension(0, 12)));
        p.add(gptBtn);
        p.add(Box.createVerticalGlue());

        return p;
    }

    private void setupManualActions() {
        manualPanel.setOnSend((questions, delaySignal) -> {
            if (!communityManager.hasEnoughMembers()) {
                JOptionPane.showMessageDialog(frame, "צריך לפחות 3 חברים בקהילה");
                return;
            }

            if (surveyManager.hasActiveSurvey()) {
                JOptionPane.showMessageDialog(frame, "כבר יש סקר פעיל בקהילה. אנא המתן לסיום הסקר הנוכחי.", "לא ניתן ליצור סקר", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Long chosenDelay = showDelayDialog();

            if (chosenDelay == null) {
                return;
            }

            final long delayToUse = chosenDelay;

            cards.show(mainPanel, "LOADING");
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    Survey survey = new Survey(adminId, questions, delayToUse);
                    boolean created = surveyManager.createSurvey(survey);
                    if (!created) throw new IllegalStateException("כבר יש סקר פעיל");
                    bot.sendSurveyToAllMembers(survey);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        Survey active = surveyManager.getActiveSurvey();
                        if (active != null) resultsPanel.updateResults(active);
                        cards.show(mainPanel, "RESULTS");
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(frame, "שגיאה בשליחת הסקר: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        cards.show(mainPanel, "MENU");
                    }
                }
            }.execute();
        });
    }

    private void setupGptActions() {
        gptPanel.setOnGenerate(triplet -> {
            if (!communityManager.hasEnoughMembers()) {
                JOptionPane.showMessageDialog(frame, "צריך לפחות 3 חברים בקהילה");
                return;
            }

            if (surveyManager.hasActiveSurvey()) {
                JOptionPane.showMessageDialog(frame, "כבר יש סקר פעיל בקהילה. אנא המתן לסיום הסקר הנוכחי.", "לא ניתן ליצור סקר", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Long chosenDelay = showDelayDialog();
            if (chosenDelay == null) return;
            long delay = chosenDelay;

            cards.show(mainPanel, "LOADING");

            new SwingWorker<java.util.List<Survey.Question>, Void>() {
                @Override
                protected java.util.List<Survey.Question> doInBackground() throws Exception {
                    return new ChatGPTService().generateSurveyFromTopic(triplet.topic, triplet.count);
                }

                @Override
                protected void done() {
                    try {
                        java.util.List<Survey.Question> qs = get();
                        Survey survey = new Survey(adminId, qs, delay);
                        if (!surveyManager.createSurvey(survey)) {
                            JOptionPane.showMessageDialog(frame, "כבר יש סקר פעיל");
                            cards.show(mainPanel, "MENU");
                            return;
                        }
                        new SwingWorker<Void, Void>() {
                            @Override
                            protected Void doInBackground() throws Exception {
                                bot.sendSurveyToAllMembers(survey);
                                return null;
                            }

                            @Override
                            protected void done() {
                                try {
                                    get();
                                    resultsPanel.updateResults(survey);
                                    cards.show(mainPanel, "RESULTS");
                                } catch (Exception ex) {
                                    JOptionPane.showMessageDialog(frame, "שגיאה בשליחת הסקר: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                                    cards.show(mainPanel, "MENU");
                                }
                            }
                        }.execute();

                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(frame, "שגיאה ביצירת סקר מ-GPT: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        cards.show(mainPanel, "MENU");
                    }
                }
            }.execute();
        });
    }

    private Long showDelayDialog() {
        JDialog dialog = new JDialog(frame, "הגדר השהייה", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(frame);

        JPanel main = new JPanel();
        main.setLayout(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        main.setBackground(Color.WHITE);

        JLabel title = new JLabel("<html><div style='text-align: center;'>עוד כמה זמן תרצה לשלוח את הסקר?<br>(או 0 לשליחה מיידית)</div></html>", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, (int) 14f));
        main.add(title, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        inputPanel.setBackground(Color.WHITE);
        JTextField delayField = new JTextField("0", 5);
        JLabel delayLabel = new JLabel("השהייה :");
        inputPanel.add(delayField);
        inputPanel.add(delayLabel);
        inputPanel.removeAll();
        inputPanel.add(delayLabel);
        inputPanel.add(delayField);
        main.add(inputPanel, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnRow.setBackground(Color.WHITE);
        JButton ok = new JButton("אישור");
        JButton cancel = new JButton("בטל");
        btnRow.add(ok);
        btnRow.add(cancel);
        main.add(btnRow, BorderLayout.SOUTH);

        dialog.setContentPane(main);

        final Long[] result = new Long[1];
        AtomicBoolean closed = new AtomicBoolean(false);

        cancel.addActionListener(e -> {
            result[0] = null;
            dialog.dispose();
            closed.set(true);
        });

        ok.addActionListener(e -> {
            String txt = delayField.getText().trim();
            try {
                long m = Long.parseLong(txt);
                if (m < 0) throw new NumberFormatException();
                result[0] = m;
                dialog.dispose();
                closed.set(true);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "הכנס מספר דקות תקין ", "קלט שגוי", JOptionPane.WARNING_MESSAGE);
            }
        });

        dialog.setVisible(true);

        while (!closed.get()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return result[0];
    }

    @Override
    public void onSurveyClosed(Survey survey) {
        resultsPanel.updateResults(survey);
        SwingUtilities.invokeLater(() -> cards.show(mainPanel, "RESULTS"));
    }

    @Override
    public void onVoteUpdated(Survey survey) {
        SwingUtilities.invokeLater(() -> resultsPanel.updateResults(survey));
    }
}