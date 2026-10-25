package com.canteen.service;

import com.canteen.dao.MenuItemDAO;
import com.canteen.dao.OrderDAO;
import com.canteen.dao.impl.JdbcMenuItemDao;
import com.canteen.dao.impl.JdbcOrderDao;
import com.canteen.exception.EmptyCartException;
import com.canteen.exception.ItemUnavailableException;
import com.canteen.exception.OrderNotFoundException;
import com.canteen.exception.PaymentFailedException;
import com.canteen.model.Cart;
import com.canteen.model.CartLine;
import com.canteen.model.MenuItem;
import com.canteen.model.Order;
import com.canteen.model.OrderItem;
import com.canteen.model.OrderStatus;
import com.canteen.model.Payment;
import com.canteen.model.PaymentResult;
import com.canteen.model.User;
import com.canteen.util.Db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Checkout and order lifecycle.
 *
 * RULE: prices are ALWAYS recomputed here from the database — the UI
 * only sends item ids and quantities, never prices.
 *
 * Unit II — Multithreading: the whole checkout is guarded by a lock so
 * two checkouts can never oversell the same stock row; inside the DB
 * transaction rows are additionally locked with SELECT ... FOR UPDATE.
 */
public class OrderService {

    /** Flat packaging charge per order (₹0 when the cart is empty). */
    public static final double PACKAGING_CHARGES = 5.00;

    private final OrderDAO orders = new JdbcOrderDao();
    private final MenuItemDAO menuItems = new JdbcMenuItemDao();

    /** Unit II — synchronized stock updates: one checkout at a time. */
    private final Object stockLock = new Object();

    /**
     * Place an order. Everything happens in ONE database transaction:
     * price re-read → payment → insert order/items/history → decrement
     * stock → commit. Any failure rolls everything back.
     */
    public Order placeOrder(User user, Cart cart, Payment payment,
                            String pickupNotes)
            throws EmptyCartException, ItemUnavailableException,
                   PaymentFailedException, SQLException {
        if (cart == null || cart.isEmpty()) {
            throw new EmptyCartException(
                    "Your cart is empty. Add something tasty first!");
        }
        if (payment == null) {
            throw new PaymentFailedException("Please choose a payment method.");
        }

        synchronized (stockLock) {
            Connection tx = null;
            try {
                tx = Db.getConnection();
                tx.setAutoCommit(false);

                // 1) Re-read every price from the DB (locked rows) and
                //    validate availability + stock. UI prices are ignored.
                List<OrderItem> lines = new ArrayList<>();
                double subtotal = 0.0;
                for (CartLine cl : cart.getLines()) {
                    MenuItem fresh = menuItems.findByIdForUpdate(tx, cl.getItem().getId());
                    if (fresh == null || !fresh.isAvailable()) {
                        throw new ItemUnavailableException(
                                "'" + cl.getItem().getName()
                                + "' is no longer available. Please remove it from your cart.");
                    }
                    if (fresh.getStockQty() < cl.getQuantity()) {
                        throw new ItemUnavailableException(
                                "Only " + fresh.getStockQty() + " x '"
                                + fresh.getName() + "' left in stock.");
                    }
                    double lineTotal = fresh.getPrice() * cl.getQuantity();
                    subtotal += lineTotal;
                    lines.add(new OrderItem(fresh.getId(), fresh.getName(),
                            cl.getQuantity(), fresh.getPrice()));
                }
                subtotal = round2(subtotal);
                double charges = subtotal > 0 ? PACKAGING_CHARGES : 0.0;
                double total = round2(subtotal + charges);

                // 2) Payment (polymorphic: cash vs simulated UPI).
                PaymentResult pr = payment.pay(total);
                if (pr == null || !pr.isSuccess()) {
                    throw new PaymentFailedException(
                            "Payment did not go through. No order was placed.");
                }

                // 3) Persist order + lines + history, then decrement stock.
                Order order = new Order();
                order.setUserId(user.getId());
                order.setStatus(OrderStatus.PLACED);
                order.setPaymentMethod(payment.getMethod());
                order.setSubtotal(subtotal);
                order.setCharges(charges);
                order.setTotal(total);
                order.setPickupNotes(pickupNotes == null ? "" : pickupNotes.trim());
                order.setIdempotencyKey(UUID.randomUUID().toString());

                int orderId;
                try {
                    orderId = orders.insertOrder(tx, order);
                } catch (SQLException e) {
                    if (isDuplicateKey(e)) {
                        throw new PaymentFailedException(
                                "This order was already placed (duplicate prevented). "
                                + "Check 'My Orders' instead of ordering again.", e);
                    }
                    throw e;
                }
                orders.insertItems(tx, orderId, lines);
                orders.insertStatusHistory(tx, orderId, null, OrderStatus.PLACED,
                        "Order placed. " + pr.getMessage());
                for (OrderItem li : lines) {
                    menuItems.decrementStock(tx, li.getMenuItemId(), li.getQuantity());
                }

                tx.commit();

                order.setId(orderId);
                order.setItems(lines);
                return order;

            } catch (ItemUnavailableException | PaymentFailedException
                    | SQLException e) {
                Db.rollbackQuietly(tx);
                throw e;
            } catch (RuntimeException e) {
                Db.rollbackQuietly(tx);
                throw e;
            } finally {
                // Unit II — try/finally guarantees cleanup on every path.
                Db.closeQuietly(tx);
            }
        }
    }

    /**
     * Move an order to a new status. Illegal jumps (e.g. PLACED → READY)
     * are rejected with a friendly message.
     */
    public Order advanceStatus(int orderId, OrderStatus to, String note)
            throws OrderNotFoundException, SQLException {
        Connection tx = null;
        try {
            tx = Db.getConnection();
            tx.setAutoCommit(false);

            OrderStatus current = orders.lockStatus(tx, orderId);
            if (current == null) {
                throw new OrderNotFoundException(
                        "Order #" + orderId + " was not found.");
            }
            if (!current.canTransitionTo(to)) {
                throw new IllegalStateException(
                        "Cannot move order #" + orderId + " from "
                        + current.label() + " to " + to.label()
                        + ". Follow the order: PLACED → CONFIRMED → "
                        + "PREPARING → READY FOR PICKUP → COMPLETED.");
            }
            orders.updateStatus(tx, orderId, to);
            orders.insertStatusHistory(tx, orderId, current, to,
                    note == null ? "" : note);
            tx.commit();
            return orders.findById(orderId);
        } catch (OrderNotFoundException | IllegalStateException | SQLException e) {
            Db.rollbackQuietly(tx);
            throw e;
        } catch (RuntimeException e) {
            Db.rollbackQuietly(tx);
            throw e;
        } finally {
            Db.closeQuietly(tx);
        }
    }

    public List<Order> myOrders(int userId) throws SQLException {
        return orders.findByUser(userId);
    }

    public List<Order> kitchenQueue() throws SQLException {
        return orders.findActive();
    }

    public List<Order> allOrders() throws SQLException {
        return orders.findAll();
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static boolean isDuplicateKey(SQLException e) {
        return "23000".equals(e.getSQLState()); // integrity constraint violation
    }
}
