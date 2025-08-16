package org.example;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ResultsPanel {
    private final JPanel panel;

    private static final int BAR_WIDTH = 160;
    private static final int BAR_HEIGHT = 16;
    private static final int SIGNIFICANT_DIFF_PCT = 20;

    public ResultsPanel() {
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        panel.setBackground(Color.WHITE);
    }

    public JPanel getPanel() {
        return panel;
    }

    public void updateResults(Survey survey) {
        panel.removeAll();

        JLabel title = new JLabel("תוצאות הסקר");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(new Color(34,34,34));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0,12)));

        if (survey == null || survey.questions == null || survey.questions.isEmpty()) {
            JLabel none = new JLabel("אין תוצאות להצגה כרגע.");
            none.setForeground(Color.DARK_GRAY);
            none.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(none);
            panel.revalidate();
            panel.repaint();
            return;
        }

        for (int i = 0; i < survey.questions.size(); i++) {
            Survey.Question q = survey.questions.get(i);

            JLabel qLabel = new JLabel("שאלה " + (i+1) + ": " + q.text);
            qLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            qLabel.setForeground(new Color(34,34,34));
            qLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(qLabel);
            panel.add(Box.createRigidArea(new Dimension(0,8)));

            Map<String, Integer> results = q.getResults();
            int total = results.values().stream().mapToInt(Integer::intValue).sum();

            int topCount = -1;
            for (var e : results.entrySet()) {
                if (e.getValue() > topCount) topCount = e.getValue();
            }
            int numberWithTopCount = 0;
            for (var e : results.entrySet()) {
                if (e.getValue() == topCount) numberWithTopCount++;
            }

            double topPct = total == 0 ? 0.0 : (topCount * 100.0 / total);

            boolean highlightUnique = (topCount > 0 && numberWithTopCount == 1);
            boolean highlightTieAll = (topCount > 0 && numberWithTopCount == results.size()); // כולם שווים (>0)

            List<Map.Entry<String,Integer>> entries = new ArrayList<>(results.entrySet());
            for (var entry : entries) {
                String opt = entry.getKey();
                int count = entry.getValue();
                int pct = total == 0 ? 0 : (int) Math.round(count * 100.0 / total);

                JPanel row = new JPanel(new BorderLayout(8, 0));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, BAR_HEIGHT + 8));
                row.setBackground(Color.WHITE);

                JLabel lblOpt = new JLabel("• " + opt);
                lblOpt.setForeground(new Color(34,34,34));
                row.add(lblOpt, BorderLayout.WEST);

                JProgressBar bar = new JProgressBar(0, 100);
                bar.setValue(pct);
                bar.setStringPainted(false);
                bar.setPreferredSize(new Dimension(BAR_WIDTH, BAR_HEIGHT));
                bar.setMaximumSize(new Dimension(BAR_WIDTH, BAR_HEIGHT));
                bar.setMinimumSize(new Dimension(BAR_WIDTH, BAR_HEIGHT));
                bar.setBackground(new Color(245,245,245));
                bar.setOpaque(true);
                bar.setBorderPainted(true);

                Color defaultFill = new Color(180, 190, 200);
                Color highlightFill = new Color(0, 160, 80);
                Color tieFill = new Color(100, 150, 240);
                Color lowFill = new Color(220, 70, 70);

                if (highlightTieAll) {
                    bar.setForeground(tieFill);
                } else if (highlightUnique && count == topCount) {
                    bar.setForeground(highlightFill);
                } else {
                    if (topCount > 0 && (topPct - pct) >= SIGNIFICANT_DIFF_PCT) {
                        bar.setForeground(lowFill);
                    } else {
                        bar.setForeground(defaultFill);
                    }
                }

                row.add(bar, BorderLayout.CENTER);

                JLabel pctLabel = new JLabel(pct + "%   ");
                pctLabel.setFont(pctLabel.getFont().deriveFont(Font.BOLD));
                if (highlightTieAll) {
                    pctLabel.setForeground(tieFill);
                } else if (highlightUnique && count == topCount) {
                    pctLabel.setForeground(highlightFill);
                } else if (topCount > 0 && (topPct - pct) >= SIGNIFICANT_DIFF_PCT) {
                    pctLabel.setForeground(lowFill);
                } else {
                    pctLabel.setForeground(Color.DARK_GRAY);
                }
                row.add(pctLabel, BorderLayout.EAST);

                panel.add(row);
                panel.add(Box.createRigidArea(new Dimension(0,8)));
            }
            panel.add(Box.createRigidArea(new Dimension(0,16)));
        }
        panel.revalidate();
        panel.repaint();
    }
}
