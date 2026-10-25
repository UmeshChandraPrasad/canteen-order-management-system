package com.canteen.model;

/**
 * One persisted order line. unitPrice is the DB price snapshot taken at
 * checkout time — the bill never trusts a price typed in the UI.
 */
public class OrderItem {
    private int id;
    private int orderId;
    private int menuItemId;
    private String itemName; // snapshot for display
    private int quantity;
    private double unitPrice;

    public OrderItem() {
    }

    public OrderItem(int menuItemId, String itemName, int quantity, double unitPrice) {
        this.menuItemId = menuItemId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public int getMenuItemId() { return menuItemId; }
    public void setMenuItemId(int menuItemId) { this.menuItemId = menuItemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public double lineTotal() {
        return unitPrice * quantity;
    }
}
