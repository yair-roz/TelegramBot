package org.example;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class ManualSurveyPanel {
    private final JPanel root;
    private final JPanel cardHolder;
    private final CardLayout cardLayout = new CardLayout();
    private final List<QuestionPanel> questionPanels = new ArrayList<>();
    private int currentIndex = 0;

    private final JButton btnPrev = new JButton("שאלה קודמת");
    private final JButton btnNext = new JButton("שאלה הבאה");
    private final JButton btnAdd  = new JButton("שאלה חדשה");
    private final JButton btnSend = new JButton("שלח סקר");

    private BiConsumer<List<Survey.Question>, Long> onSend;

    public ManualSurveyPanel() {
        JPanel content = new JPanel(new BorderLayout(8,8));
        content.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        content.setBackground(Color.WHITE);

        cardHolder = new JPanel(cardLayout);
        content.add(cardHolder, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(Color.WHITE);

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT));
        nav.setBackground(Color.WHITE);
        nav.add(btnPrev);
        nav.add(btnNext);
        bottom.add(nav, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setBackground(Color.WHITE);
        actions.add(btnAdd);
        actions.add(btnSend);
        bottom.add(actions, BorderLayout.EAST);

        content.add(bottom, BorderLayout.SOUTH);

        btnPrev.addActionListener(e -> showQuestion(currentIndex - 1));
        btnNext.addActionListener(e -> showQuestion(currentIndex + 1));
        btnAdd.addActionListener(e -> {
            if (!canAddNewQuestion()) {
                JOptionPane.showMessageDialog(content, "מלא את השאלה הנוכחית (טקסט + לפחות 2 תשובות) לפני הוספה.", "שגיאה", JOptionPane.WARNING_MESSAGE);
                return;
            }
            addQuestion();
            showQuestion(questionPanels.size() - 1);
        });
        btnSend.addActionListener(e -> {
            if (onSend == null) return;
            List<Survey.Question> qs = new ArrayList<>();
            for (QuestionPanel qp : questionPanels) {
                if (!qp.isValid()) {
                    JOptionPane.showMessageDialog(content, "כל שאלה חייבת לכלול טקסט ו-לפחות 2 תשובות", "שגיאה", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                qs.add(qp.toSurveyQuestion());
            }
            onSend.accept(qs, -1L);
        });

        root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Color.WHITE);
        root.add(Box.createVerticalGlue());
        root.add(content);
        root.add(Box.createVerticalGlue());

        addQuestion();
        showQuestion(0);
    }

    public JPanel getPanel() { return root; }

    public void setOnSend(BiConsumer<List<Survey.Question>, Long> onSend) {
        this.onSend = onSend;
    }

    private void addQuestion() {
        if (questionPanels.size() >= 3) {
            JOptionPane.showMessageDialog(root, "ניתן להוסיף עד 3 שאלות בלבד", "הודעה", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        QuestionPanel qp = new QuestionPanel(questionPanels.size() + 1);
        qp.setValidityListener(this::updateButtonsState);
        questionPanels.add(qp);
        cardHolder.add(qp.getPanel(), String.valueOf(questionPanels.size() - 1));
        cardHolder.revalidate();
        cardHolder.repaint();
        updateButtonsState();
    }

    private void showQuestion(int idx) {
        if (idx < 0 || idx >= questionPanels.size()) return;
        currentIndex = idx;
        cardLayout.show(cardHolder, String.valueOf(idx));
        updateButtonsState();
    }

    private boolean canAddNewQuestion() {
        if (questionPanels.isEmpty()) return true;
        QuestionPanel cur = questionPanels.get(currentIndex);
        return cur.isValid() && questionPanels.size() < 3;
    }

    private void updateButtonsState() {
        btnPrev.setEnabled(currentIndex > 0);
        btnNext.setEnabled(currentIndex < questionPanels.size() - 1);
        btnAdd.setEnabled(canAddNewQuestion());
        btnSend.setVisible(currentIndex == questionPanels.size() - 1 && questionPanels.get(currentIndex).isValid());
    }

    private static class QuestionPanel {
        private final JPanel panel;
        private final JTextField txtQuestion = new JTextField();
        private final java.util.List<JTextField> answerFields = new ArrayList<>();
        private Runnable validityListener;

        QuestionPanel(int idx) {
            panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(BorderFactory.createTitledBorder("שאלה " + idx));
            panel.setBackground(Color.WHITE);

            JLabel lblQ = new JLabel("שאלה:");
            lblQ.setFont(lblQ.getFont().deriveFont(14f));
            panel.add(lblQ);
            panel.add(Box.createRigidArea(new Dimension(0,4)));
            txtQuestion.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            panel.add(txtQuestion);
            panel.add(Box.createRigidArea(new Dimension(0,8)));

            for (int i = 1; i <= 4; i++) {
                JLabel lblA = new JLabel("תשובה " + i + ":");
                lblA.setFont(lblA.getFont().deriveFont(13f));
                JTextField f = new JTextField();
                f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
                answerFields.add(f);
                panel.add(lblA);
                panel.add(Box.createRigidArea(new Dimension(0,3)));
                panel.add(f);
                panel.add(Box.createRigidArea(new Dimension(0,6)));
            }

            DocumentListener dl = new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { notifyListener(); }
                public void removeUpdate(DocumentEvent e) { notifyListener(); }
                public void changedUpdate(DocumentEvent e) { notifyListener(); }
            };
            txtQuestion.getDocument().addDocumentListener(dl);
            answerFields.forEach(f -> f.getDocument().addDocumentListener(dl));
        }

        JPanel getPanel() { return panel; }

        boolean isValid() {
            if (txtQuestion.getText().trim().isEmpty()) return false;
            long cnt = answerFields.stream().filter(f -> !f.getText().trim().isEmpty()).count();
            return cnt >= 2;
        }

        Survey.Question toSurveyQuestion() {
            java.util.List<String> opts = new ArrayList<>();
            for (JTextField f : answerFields) {
                String t = f.getText().trim();
                if (!t.isEmpty()) opts.add(t);
            }
            return new Survey.Question(txtQuestion.getText().trim(), opts);
        }

        void setValidityListener(Runnable r) { this.validityListener = r; }

        private void notifyListener() {
            if (validityListener != null) validityListener.run();
        }
    }
}
