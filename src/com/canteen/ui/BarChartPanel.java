package com.canteen.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import javax.swing.JPanel;

/**
 * A simple hand-drawn bar chart (no chart library — the syllabus only
 * allows the MySQL connector as an external dependency).
 */
@SuppressWarnings("serial")
public class BarChartPanel extends JPanel {

    private List<String> labels = List.of();
    private List<Double> values = List.of();
    private String title = "";

    public BarChartPanel() {
        setPreferredSize(new Dimension(600, 260));
        setBackground(Color.WHITE);
    }

    public void setData(String title, List<String> labels, List<Double> values) {
        this.title = title;
        this.labels = labels;
        this.values = values;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        int padL = 60, padR = 16, padT = 36, padB = 44;

        g2.setColor(Color.DARK_GRAY);
        g2.drawString(title, padL, 20);

        if (labels.isEmpty()) {
            g2.drawString("No data yet.", padL, padT + 20);
            g2.dispose();
            return;
        }

        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (max <= 0) {
            max = 1.0;
        }
        int plotW = w - padL - padR;
        int plotH = h - padT - padB;
        int n = labels.size();
        int gap = 10;
        int barW = Math.max(8, (plotW - gap * (n + 1)) / n);

        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < n; i++) {
            int x = padL + gap + i * (barW + gap);
            int barH = (int) (values.get(i) / max * plotH);
            int y = padT + (plotH - barH);

            g2.setColor(new Color(0x2E7D32));
            g2.fillRect(x, y, barW, barH);

            g2.setColor(Color.DARK_GRAY);
            String val = "₹" + String.format("%.0f", values.get(i));
            g2.drawString(val, x, y - 4);
            String lab = labels.get(i);
            int lw = fm.stringWidth(lab);
            g2.drawString(lab, x + Math.max(0, (barW - lw) / 2), h - padB + 18);
        }
        g2.dispose();
    }
}
