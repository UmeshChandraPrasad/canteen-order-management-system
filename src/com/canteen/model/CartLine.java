package com.canteen.model;

/** One line in the shopping cart: an item plus a chosen quantity. */
public class CartLine {
    private final MenuItem item;
    private int quantity;

    public CartLine(MenuItem item, int quantity) {
        this.item = item;
        this.quantity = quantity;
    }

    public MenuItem getItem() { return item; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    /** Line total from the DB price snapshot held by this item. */
    public double lineTotal() {
        return item.getPrice() * quantity;
    }
}
