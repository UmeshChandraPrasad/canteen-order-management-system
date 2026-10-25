package com.canteen.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** A placed canteen order with its lines. */
public class Order {
    private int id;
    private int userId;
    private String customerName; // denormalised for the kitchen screen
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private double subtotal;
    private double charges;
    private double total;
    private String pickupNotes;
    private String idempotencyKey;
    private LocalDateTime createdAt;
    private List<OrderItem> items = new ArrayList<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getCharges() { return charges; }
    public void setCharges(double charges) { this.charges = charges; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getPickupNotes() { return pickupNotes; }
    public void setPickupNotes(String pickupNotes) { this.pickupNotes = pickupNotes; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    /** "2 x Masala Dosa, 1 x Chai" — one-line summary for tables. */
    public String itemsSummary() {
        StringBuilder sb = new StringBuilder();
        for (OrderItem it : items) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(it.getQuantity()).append(" x ").append(it.getItemName());
        }
        return sb.toString();
    }
}
