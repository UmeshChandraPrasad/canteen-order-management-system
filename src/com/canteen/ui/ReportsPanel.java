package com.canteen.ui;

import com.canteen.service.ReportService;
import com.canteen.service.ReportService.TopItem;
import com.canteen.util.Ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * Admin sales reports: today's revenue per item, top-5 sellers of all
 * time, and a CSV export.
 */
@SuppressWarnings("serial")
public class ReportsPanel extends JPanel {

    private final ReportService reports = new ReportService();
    private final DefaultTableModel byItemModel =
            new DefaultTableModel(new String[]{"Item", "Revenue (₹)"}, 0) {
                @Override
                public boolean isCellEditable(int r, int c) { return false; }
            };
    private final DefaultTableModel topModel =
            new DefaultTableModel(new String[]{"Item", "Qty Sold", "Revenue (₹)"}, 0) {
                @Override
                public boolean isCellEditable(int r, int c) { return false; }
            };
    private Map<String, Double> lastByItem = Map.of();
    private List<TopItem> lastTop = List.of();

    public ReportsPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Sales reports"));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> load());
        JButton csvBtn = new JButton("Export CSV");
        csvBtn.addActionListener(e -> exportCsv());
        top.add(refreshBtn);
        top.add(csvBtn);
        add(top, BorderLayout.NORTH);

        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(BorderFactory.createTitledBorder("Today's revenue by item"));
        left.add(new JScrollPane(new JTable(byItemModel)), BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(BorderFactory.createTitledBorder("Top 5 selling items (all time)"));
        right.add(new JScrollPane(new JTable(topModel)), BorderLayout.CENTER);

        add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right),
                BorderLayout.CENTER);
        load();
    }

    private void load() {
        new SwingWorker<Void, Void>() {
            private Map<String, Double> byItem;
            private List<TopItem> top;

            @Override
            protected Void doInBackground() throws Exception {
                byItem = reports.revenueByItemOn(LocalDate.now());
                top = reports.topSellingItems(5);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    lastByItem = byItem;
                    lastTop = top;
                    byItemModel.setRowCount(0);
                    byItem.entrySet().stream()
                            .sorted(Map.Entry.<String, Double>comparingByValue()
                                    .reversed())
                            .forEach(e -> byItemModel.addRow(new Object[]{
                                    e.getKey(), Ui.money(e.getValue())}));
                    topModel.setRowCount(0);
                    for (TopItem t : top) {
                        topModel.addRow(new Object[]{
                                t.getName(), t.getQuantitySold(),
                                Ui.money(t.getRevenue())});
                    }
                } catch (Exception ex) {
                    Ui.error(ReportsPanel.this,
                            "Could not load reports.", ex);
                }
            }
        }.execute();
    }

    private void exportCsv() {
        // Unit II — try-with-resources for the file handle.
        try (PrintWriter w = new PrintWriter(new FileWriter("sales_report.csv"))) {
            w.println("section,item,quantity,revenue_inr");
            for (Map.Entry<String, Double> e : lastByItem.entrySet()) {
                w.printf("today,%s,,%.2f%n", csv(e.getKey()), e.getValue());
            }
            for (TopItem t : lastTop) {
                w.printf("top5,%s,%d,%.2f%n",
                        csv(t.getName()), t.getQuantitySold(), t.getRevenue());
            }
            Ui.info(this, "Report exported to sales_report.csv"
                    + " in the application folder.");
        } catch (IOException ex) {
            Ui.error(this, "Could not write sales_report.csv.", ex);
        }
    }

    private static String csv(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
