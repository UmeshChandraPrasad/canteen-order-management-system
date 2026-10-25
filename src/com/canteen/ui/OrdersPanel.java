package com.canteen.ui;

import com.canteen.model.Order;
import com.canteen.model.User;
import com.canteen.service.OrderService;
import com.canteen.util.Ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * "My Orders" — the customer watches live order statuses here.
 * Unit II — Multithreading: a dedicated background thread polls the
 * database every 20 seconds; the SwingWorker posts results to the EDT.
 */
@SuppressWarnings("serial")
public class OrdersPanel extends JPanel implements MainFrame.Stoppable {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM, HH:mm");

    private final User user;
    private final OrderService orders = new OrderService();
    private final DefaultTableModel model =
            new DefaultTableModel(new String[]{
                    "Order #", "Placed", "Items", "Total", "Payment", "Status"},
                    0) {
                @Override
                public boolean isCellEditable(int r, int c) { return false; }
            };

    private volatile boolean running = true;
    private final Thread refresher;

    public OrdersPanel(User user) {
        super(new BorderLayout(8, 8));
        this.user = user;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Your orders (auto-refreshes every 20 seconds):"));
        JButton refreshBtn = new JButton("Refresh now");
        refreshBtn.addActionListener(e -> load());
        top.add(refreshBtn);
        add(top, BorderLayout.NORTH);

        add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);

        // Unit II — background thread for live status updates.
        refresher = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(20_000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                if (running) {
                    load();
                }
            }
        }, "orders-auto-refresh");
        refresher.setDaemon(true);
        refresher.start();

        load();
    }

    private void load() {
        new SwingWorker<List<Order>, Void>() {
            @Override
            protected List<Order> doInBackground() throws Exception {
                return orders.myOrders(user.getId());
            }

            @Override
            protected void done() {
                try {
                    render(get());
                } catch (Exception ex) {
                    // Silent on background polls; loud on manual refresh.
                    System.err.println("[OrdersPanel] refresh failed: "
                            + ex.getMessage());
                }
            }
        }.execute();
    }

    private void render(List<Order> list) {
        model.setRowCount(0);
        for (Order o : list) {
            model.addRow(new Object[]{
                    o.getId(),
                    o.getCreatedAt() == null ? "" : o.getCreatedAt().format(FMT),
                    o.itemsSummary(),
                    Ui.money(o.getTotal()),
                    o.getPaymentMethod().label(),
                    o.getStatus().label()
            });
        }
    }

    @Override
    public void stop() {
        running = false;
        refresher.interrupt();
    }
}
