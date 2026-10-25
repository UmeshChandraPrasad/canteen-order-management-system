package com.canteen.ui;

import com.canteen.exception.OrderNotFoundException;
import com.canteen.model.Order;
import com.canteen.model.OrderStatus;
import com.canteen.service.OrderService;
import com.canteen.util.Ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * Admin "Manage Orders" (kitchen screen): advance orders through
 * PLACED → CONFIRMED → PREPARING → READY FOR PICKUP → COMPLETED.
 * Invalid jumps are rejected with a friendly dialog.
 * Unit II — a background thread auto-refreshes the queue.
 */
@SuppressWarnings("serial")
public class KitchenPanel extends JPanel implements MainFrame.Stoppable {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    private final OrderService orders = new OrderService();
    private final DefaultTableModel model =
            new DefaultTableModel(new String[]{
                    "Order #", "Time", "Customer", "Items",
                    "Total", "Payment", "Status"}, 0) {
                @Override
                public boolean isCellEditable(int r, int c) { return false; }
            };
    private final JTable table = new JTable(model);
    private final JComboBox<String> filterCombo = new JComboBox<>(
            new String[]{"All active", "PLACED", "CONFIRMED",
                    "PREPARING", "READY FOR PICKUP"});
    private final JComboBox<OrderStatus> targetCombo =
            new JComboBox<>(OrderStatus.values());
    private final JTextField noteField = new JTextField(20);

    private volatile boolean running = true;
    private final Thread refresher;
    private List<Order> current = List.of();

    public KitchenPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Show:"));
        top.add(filterCombo);
        JButton refreshBtn = new JButton("Refresh");
        top.add(refreshBtn);
        top.add(new JLabel("(auto-refreshes every 15 seconds)"));
        filterCombo.addActionListener(e -> render(current));
        refreshBtn.addActionListener(e -> load());
        add(top, BorderLayout.NORTH);

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.setBorder(BorderFactory.createTitledBorder("Change status"));
        bottom.add(new JLabel("Move selected order to:"));
        bottom.add(targetCombo);
        bottom.add(new JLabel("Note:"));
        bottom.add(noteField);
        JButton updateBtn = new JButton("Update Status");
        updateBtn.addActionListener(e -> updateStatus());
        bottom.add(updateBtn);
        add(bottom, BorderLayout.SOUTH);

        refresher = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(15_000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                if (running) {
                    load();
                }
            }
        }, "kitchen-auto-refresh");
        refresher.setDaemon(true);
        refresher.start();

        load();
    }

    private void load() {
        new SwingWorker<List<Order>, Void>() {
            @Override
            protected List<Order> doInBackground() throws Exception {
                return orders.kitchenQueue();
            }

            @Override
            protected void done() {
                try {
                    current = get();
                    render(current);
                } catch (Exception ex) {
                    System.err.println("[KitchenPanel] refresh failed: "
                            + ex.getMessage());
                }
            }
        }.execute();
    }

    private void render(List<Order> list) {
        String filter = (String) filterCombo.getSelectedItem();
        model.setRowCount(0);
        for (Order o : list) {
            if (!"All active".equals(filter)
                    && !o.getStatus().label().equals(filter)) {
                continue;
            }
            model.addRow(new Object[]{
                    o.getId(),
                    o.getCreatedAt() == null ? "" : o.getCreatedAt().format(FMT),
                    o.getCustomerName(),
                    o.itemsSummary(),
                    Ui.money(o.getTotal()),
                    o.getPaymentMethod().label(),
                    o.getStatus().label()
            });
        }
    }

    /** Unit IV — ListSelectionListener-style flow via table selection. */
    private void updateStatus() {
        int row = table.getSelectedRow();
        if (row < 0) {
            Ui.error(this, "Select an order in the table first.");
            return;
        }
        int orderId = (Integer) model.getValueAt(row, 0);
        OrderStatus to = (OrderStatus) targetCombo.getSelectedItem();
        String note = noteField.getText().trim();

        new SwingWorker<Order, Void>() {
            @Override
            protected Order doInBackground() throws Exception {
                return orders.advanceStatus(orderId, to, note);
            }

            @Override
            protected void done() {
                try {
                    Order updated = get();
                    Ui.info(KitchenPanel.this, "Order #" + orderId
                            + " is now " + updated.getStatus().label() + ".");
                    noteField.setText("");
                    load();
                } catch (Exception ex) {
                    Throwable t = ex.getCause() != null ? ex.getCause() : ex;
                    if (t instanceof OrderNotFoundException
                            || t instanceof IllegalStateException) {
                        // Invalid transition -> friendly rejection.
                        Ui.error(KitchenPanel.this, t.getMessage());
                    } else if (t instanceof SQLException) {
                        Ui.error(KitchenPanel.this,
                                "Could not update the order (database error).", t);
                    } else {
                        Ui.error(KitchenPanel.this,
                                "Could not update the order.", t);
                    }
                }
            }
        }.execute();
    }

    @Override
    public void stop() {
        running = false;
        refresher.interrupt();
    }
}
