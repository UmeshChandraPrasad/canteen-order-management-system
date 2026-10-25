package com.canteen.ui;

import com.canteen.model.OrderStatus;
import com.canteen.service.ReportService;
import com.canteen.util.Ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

/**
 * Admin dashboard: today's revenue, today's order count, live orders
 * per status, and a 7-day revenue bar chart.
 */
@SuppressWarnings("serial")
public class DashboardPanel extends JPanel {

    private final ReportService reports = new ReportService();
    private final JLabel revenueLabel = statLabel();
    private final JLabel countLabel = statLabel();
    private final JLabel statusLabel = statLabel();
    private final BarChartPanel chart = new BarChartPanel();

    public DashboardPanel() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel stats = new JPanel(new GridLayout(1, 3, 12, 12));
        stats.add(statCard("Today's Revenue", revenueLabel));
        stats.add(statCard("Today's Orders", countLabel));
        stats.add(statCard("Live Orders by Status", statusLabel));
        add(stats, BorderLayout.NORTH);

        JPanel chartCard = new JPanel(new BorderLayout());
        chartCard.setBorder(BorderFactory.createTitledBorder("Revenue — last 7 days"));
        chartCard.add(chart, BorderLayout.CENTER);
        add(chartCard, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> load());
        bottom.add(refreshBtn);
        add(bottom, BorderLayout.SOUTH);

        load();
    }

    private static JLabel statLabel() {
        JLabel l = new JLabel("—");
        l.setFont(l.getFont().deriveFont(Font.BOLD, 20f));
        return l;
    }

    private JPanel statCard(String title, JLabel value) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xDDDDDD)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        card.setBackground(Color.WHITE);
        JLabel t = new JLabel(title);
        t.setForeground(Color.DARK_GRAY);
        card.add(t, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    /** Unit II — all numbers load on a SwingWorker. */
    private void load() {
        new SwingWorker<DashboardData, Void>() {
            @Override
            protected DashboardData doInBackground() throws Exception {
                LocalDate today = LocalDate.now();
                DashboardData d = new DashboardData();
                d.revenue = reports.revenueOn(today);
                d.count = reports.orderCountOn(today);
                d.byStatus = reports.countsByStatus();
                d.last7 = reports.lastDaysRevenue(7);
                return d;
            }

            @Override
            protected void done() {
                try {
                    DashboardData d = get();
                    revenueLabel.setText(Ui.money(d.revenue));
                    countLabel.setText(String.valueOf(d.count));
                    StringBuilder sb = new StringBuilder("<html>");
                    for (OrderStatus s : OrderStatus.values()) {
                        long n = d.byStatus.getOrDefault(s, 0L);
                        if (n > 0) {
                            sb.append(s.label()).append(": ").append(n).append("<br>");
                        }
                    }
                    sb.append("</html>");
                    statusLabel.setFont(statusLabel.getFont().deriveFont(13f));
                    statusLabel.setText(sb.toString());

                    DateTimeFormatter f = DateTimeFormatter.ofPattern("dd MMM");
                    List<String> labels = new ArrayList<>();
                    List<Double> values = new ArrayList<>();
                    for (Map.Entry<java.time.LocalDate, Double> e
                            : d.last7.entrySet()) {
                        labels.add(e.getKey().format(f));
                        values.add(e.getValue());
                    }
                    chart.setData("Daily revenue (₹)", labels, values);
                } catch (Exception ex) {
                    Ui.error(DashboardPanel.this,
                            "Could not load dashboard data.", ex);
                }
            }
        }.execute();
    }

    private static class DashboardData {
        double revenue;
        long count;
        Map<OrderStatus, Long> byStatus;
        Map<java.time.LocalDate, Double> last7;
    }
}
