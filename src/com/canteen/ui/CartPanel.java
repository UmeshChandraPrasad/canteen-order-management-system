package com.canteen.ui;

import com.canteen.exception.EmptyCartException;
import com.canteen.exception.ItemUnavailableException;
import com.canteen.exception.PaymentFailedException;
import com.canteen.model.Cart;
import com.canteen.model.CartLine;
import com.canteen.model.CashOnPickupPayment;
import com.canteen.model.Order;
import com.canteen.model.Payment;
import com.canteen.model.SimulatedUpiPayment;
import com.canteen.model.User;
import com.canteen.service.OrderService;
import com.canteen.util.Ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;

/**
 * Two-step checkout: STEP 1 reviews the cart, STEP 2 takes the payment
 * choice and pickup notes, then places the order on a SwingWorker.
 */
@SuppressWarnings("serial")
public class CartPanel extends JPanel {

    private static final String STEP_REVIEW = "REVIEW";
    private static final String STEP_PAY = "PAY";

    private final User user;
    private final Cart cart;
    private final Runnable onOrderPlaced; // switch to My Orders tab
    private final Runnable onCartChanged; // refresh tab badge
    private final OrderService orderService = new OrderService();

    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final CartTableModel tableModel = new CartTableModel();
    private final JLabel subtotalLabel = new JLabel();
    private final JLabel chargesLabel = new JLabel();
    private final JLabel totalLabel = new JLabel();

    private final JRadioButton cashRadio =
            new JRadioButton("Cash on Pickup (pay at the counter)", true);
    private final JRadioButton upiRadio =
            new JRadioButton("UPI — " + SimulatedUpiPayment.DEMO_LABEL);
    private final JTextArea notesArea = new JTextArea(3, 30);

    public CartPanel(User user, Cart cart, Runnable onOrderPlaced,
                     Runnable onCartChanged) {
        super(new BorderLayout());
        this.user = user;
        this.cart = cart;
        this.onOrderPlaced = onOrderPlaced;
        this.onCartChanged = onCartChanged;

        cardPanel.add(buildReviewStep(), STEP_REVIEW);
        cardPanel.add(buildPayStep(), STEP_PAY);
        add(cardPanel, BorderLayout.CENTER);
        refresh();
    }

    /** Re-render table + totals (call after every cart mutation). */
    public void refresh() {
        tableModel.refresh();
        double subtotal = cart.getSubtotal();
        double charges = subtotal > 0 ? OrderService.PACKAGING_CHARGES : 0;
        subtotalLabel.setText("Subtotal: " + Ui.money(subtotal));
        chargesLabel.setText("Packaging charges: " + Ui.money(charges));
        totalLabel.setText("Total: " + Ui.money(subtotal + charges));
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 15f));
    }

    private JPanel buildReviewStep() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTable table = new JTable(tableModel);
        table.getColumnModel().getColumn(4).setPreferredWidth(60);
        p.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel totals = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        totals.add(subtotalLabel);
        totals.add(chargesLabel);
        totals.add(totalLabel);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton removeBtn = new JButton("Remove Selected");
        removeBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                Ui.error(this, "Select an item in the table first.");
                return;
            }
            cart.removeItem(tableModel.lineAt(row).getItem().getId());
            refresh();
            onCartChanged.run();
        });
        JButton clearBtn = new JButton("Clear Cart");
        clearBtn.addActionListener(e -> {
            if (cart.isEmpty() || Ui.confirm(this, "Remove everything from your cart?")) {
                cart.clear();
                refresh();
                onCartChanged.run();
            }
        });
        JButton nextBtn = new JButton("Proceed to Payment →");
        nextBtn.addActionListener(e -> {
            if (cart.isEmpty()) {
                Ui.error(this, "Your cart is empty. Add something tasty first!");
                return;
            }
            refresh();
            cards.show(cardPanel, STEP_PAY);
        });
        buttons.add(removeBtn);
        buttons.add(clearBtn);
        buttons.add(nextBtn);

        JPanel south = new JPanel(new BorderLayout());
        south.add(totals, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildPayStep() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel center = new JPanel(new BorderLayout(8, 8));
        JLabel heading = new JLabel("Step 2 of 2 — Payment");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));
        center.add(heading, BorderLayout.NORTH);

        JPanel methods = new JPanel(new FlowLayout(FlowLayout.LEFT));
        methods.setBorder(BorderFactory.createTitledBorder("Payment method"));
        ButtonGroup group = new ButtonGroup();
        group.add(cashRadio);
        group.add(upiRadio);
        methods.add(cashRadio);
        methods.add(upiRadio);
        center.add(methods, BorderLayout.CENTER);

        // The UPI option is simulated: the label is always visible.
        JLabel demoNote = new JLabel(
                "UPI here is " + SimulatedUpiPayment.DEMO_LABEL + ".");
        demoNote.setForeground(new Color(0xB3261E));
        demoNote.setFont(demoNote.getFont().deriveFont(Font.BOLD));

        JPanel notesPanel = new JPanel(new BorderLayout());
        notesPanel.setBorder(BorderFactory.createTitledBorder(
                "Pickup notes (optional, e.g. 'no onion')"));
        notesPanel.add(new JScrollPane(notesArea), BorderLayout.CENTER);

        JPanel mid = new JPanel(new BorderLayout(8, 8));
        mid.add(demoNote, BorderLayout.NORTH);
        mid.add(notesPanel, BorderLayout.CENTER);
        center.add(mid, BorderLayout.SOUTH);
        p.add(center, BorderLayout.CENTER);

        JLabel amountLabel = new JLabel();
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton backBtn = new JButton("← Back to Cart");
        backBtn.addActionListener(e -> cards.show(cardPanel, STEP_REVIEW));
        JButton placeBtn = new JButton("Place Order");
        placeBtn.setFont(placeBtn.getFont().deriveFont(Font.BOLD, 14f));
        placeBtn.addActionListener(e -> placeOrder(placeBtn));
        south.add(amountLabel);
        south.add(backBtn);
        south.add(placeBtn);
        p.add(south, BorderLayout.SOUTH);

        // Keep the payable amount label fresh whenever this step shows.
        p.addHierarchyListener(e -> {
            double subtotal = cart.getSubtotal();
            double total = subtotal + (subtotal > 0 ? OrderService.PACKAGING_CHARGES : 0);
            amountLabel.setText("Payable: " + Ui.money(total));
        });
        return p;
    }

    /** Unit II — checkout on a SwingWorker; UI stays responsive. */
    private void placeOrder(JButton placeBtn) {
        Payment payment = cashRadio.isSelected()
                ? new CashOnPickupPayment()
                : new SimulatedUpiPayment();
        String notes = notesArea.getText();
        placeBtn.setEnabled(false);

        new SwingWorker<Order, Void>() {
            @Override
            protected Order doInBackground() throws Exception {
                // Polymorphic pay(): the service only sees Payment.
                return orderService.placeOrder(user, cart, payment, notes);
            }

            @Override
            protected void done() {
                placeBtn.setEnabled(true);
                try {
                    Order order = get();
                    cart.clear();
                    notesArea.setText("");
                    refresh();
                    onCartChanged.run();
                    cards.show(cardPanel, STEP_REVIEW);
                    Ui.info(CartPanel.this,
                            "Order #" + order.getId() + " placed!\n"
                            + "Total " + Ui.money(order.getTotal()) + ". "
                            + paymentMessage(payment, order));
                    onOrderPlaced.run();
                } catch (Exception ex) {
                    Throwable t = ex.getCause() != null ? ex.getCause() : ex;
                    if (t instanceof EmptyCartException
                            || t instanceof ItemUnavailableException
                            || t instanceof PaymentFailedException
                            || t instanceof IllegalArgumentException
                            || t instanceof IllegalStateException) {
                        Ui.error(CartPanel.this, t.getMessage());
                    } else if (t instanceof SQLException) {
                        Ui.error(CartPanel.this,
                                "Could not save your order (database error). "
                                + "Please try again.", t);
                    } else {
                        Ui.error(CartPanel.this,
                                "Something went wrong while placing the order.", t);
                    }
                }
            }
        }.execute();
    }

    private static String paymentMessage(Payment payment, Order order) {
        if (payment instanceof SimulatedUpiPayment) {
            return "Simulated UPI approved (demo — no real money moved).";
        }
        return "Please pay " + Ui.money(order.getTotal())
                + " in cash at the pickup counter.";
    }

    /** Editable-qty table model over the cart snapshot. */
    @SuppressWarnings("serial")
    private class CartTableModel extends AbstractTableModel {
        private final String[] cols = {"Item", "Price", "Qty", "Line Total"};
        private List<CartLine> rows = List.of();

        void refresh() {
            rows = cart.getLines();
            fireTableDataChanged();
        }

        CartLine lineAt(int row) {
            return rows.get(row);
        }

        @Override
        public int getRowCount() { return rows.size(); }

        @Override
        public int getColumnCount() { return cols.length; }

        @Override
        public String getColumnName(int col) { return cols[col]; }

        @Override
        public boolean isCellEditable(int row, int col) { return col == 2; }

        @Override
        public Object getValueAt(int row, int col) {
            CartLine l = rows.get(row);
            switch (col) {
                case 0: return l.getItem().getName();
                case 1: return Ui.money(l.getItem().getPrice());
                case 2: return l.getQuantity();
                default: return Ui.money(l.lineTotal());
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col != 2) {
                return;
            }
            try {
                int qty = Integer.parseInt(String.valueOf(value).trim());
                CartLine l = rows.get(row);
                cart.updateQuantity(l.getItem().getId(), qty);
                refresh();
                onCartChanged.run();
                CartPanel.this.refresh();
            } catch (NumberFormatException ex) {
                Ui.error(CartPanel.this, "Quantity must be a whole number.");
            } catch (ItemUnavailableException ex) {
                Ui.error(CartPanel.this, ex.getMessage());
            }
        }
    }
}
