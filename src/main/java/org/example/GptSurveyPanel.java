package org.example;
import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class GptSurveyPanel {
    private JPanel panel;
    private JTextField txtTopic;
    private JTextField txtCount;
    private JButton btnGen;
    private Consumer<Triplet> onGenerate;

    public static class Triplet {
        public final String topic;
        public final int count;
        public final long delay;
        public Triplet(String topic, int count, long delay) {
            this.topic = topic; this.count = count; this.delay = delay;
        }
    }

    public GptSurveyPanel() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        content.setBackground(Color.WHITE);

        JLabel lblTopic = new JLabel("נושא לסקר:");
        lblTopic.setFont(lblTopic.getFont().deriveFont(14f));
        txtTopic = new JTextField();
        txtTopic.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel lblCount = new JLabel("מספר שאלות (1-3):");
        lblCount.setFont(lblCount.getFont().deriveFont(14f));
        txtCount = new JTextField();
        txtCount.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        btnGen = new JButton("צור סקר");
        btnGen.setFont(btnGen.getFont().deriveFont(16f));
        btnGen.setPreferredSize(new Dimension(180, 40));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.add(btnGen);

        btnGen.addActionListener(e -> {
            String topic = txtTopic.getText().trim();
            String countText = txtCount.getText().trim();
            if (topic.isEmpty()) {
                JOptionPane.showMessageDialog(content, "יש להזין נושא לסקר");
                return;
            }
            int count;
            try { count = Integer.parseInt(countText); }
            catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(content, "מספר שאלות חייב להיות מספר");
                return;
            }
            if (count < 1 || count > 3) {
                JOptionPane.showMessageDialog(content, "יש לבחור בין 1 ל-3 שאלות");
                return;
            }
            if (onGenerate != null) onGenerate.accept(new Triplet(topic, count, -1L));
        });

        content.add(lblTopic);
        content.add(txtTopic);
        content.add(Box.createRigidArea(new Dimension(0,12)));
        content.add(lblCount);
        content.add(txtCount);
        content.add(Box.createRigidArea(new Dimension(0,20)));
        content.add(Box.createVerticalGlue());
        content.add(btnPanel);
        content.add(Box.createVerticalGlue());

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.add(Box.createVerticalGlue());
        panel.add(content);
        panel.add(Box.createVerticalGlue());
    }

    public JPanel getPanel() { return panel; }
    public void setOnGenerate(Consumer<Triplet> onGenerate) { this.onGenerate = onGenerate; }
}
