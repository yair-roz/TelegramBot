package org.example;
import javax.swing.*;
import java.awt.*;

public class LoadingPanel {
    private final JPanel panel;
    private final JProgressBar bar;

    public LoadingPanel() {
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(40,40,40,40));
        panel.setBackground(Color.WHITE);

        JLabel lbl = new JLabel("Please wait...");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 20));
        lbl.setForeground(new Color(34,34,34));
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setAlignmentX(Component.CENTER_ALIGNMENT);
        bar.setPreferredSize(new Dimension(260, 16));
        bar.setMaximumSize(new Dimension(400, 16));
        panel.add(Box.createVerticalGlue());
        panel.add(lbl);
        panel.add(Box.createRigidArea(new Dimension(0,14)));
        panel.add(bar);
        panel.add(Box.createVerticalGlue());
    }

    public JPanel getPanel() { return panel; }
}
