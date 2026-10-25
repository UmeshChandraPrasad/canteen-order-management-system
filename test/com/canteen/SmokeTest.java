package com.canteen;

import com.canteen.exception.EmptyCartException;
import com.canteen.exception.ItemUnavailableException;
import com.canteen.model.Cart;
import com.canteen.model.CashOnPickupPayment;
import com.canteen.model.MenuItem;
import com.canteen.model.Order;
import com.canteen.model.OrderStatus;
import com.canteen.model.SimulatedUpiPayment;
import com.canteen.model.User;
import com.canteen.service.AuthService;
import com.canteen.service.MenuService;
import com.canteen.service.OrderService;
import com.canteen.service.ReportService;

import java.util.List;
import java.util.UUID;

/**
 * Headless smoke test for the service + DAO layers (no Swing).
 * Run AFTER the DB is set up: java -cp ... com.canteen.SmokeTest
 * Exit code 0 = all checks passed.
 */
public class SmokeTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        AuthService auth = new AuthService();
        MenuService menu = new MenuService();
        OrderService orders = new OrderService();
        ReportService reports = new ReportService();

        // 1) register + login
        String email = "smoke" + UUID.randomUUID().toString().substring(0, 8)
                + "@test.local";
        User me = auth.register("Smoke Tester", email, "test1234", "test1234");
        check("register returns CUSTOMER user", me.getId() > 0 && !me.isAdmin());
        User logged = auth.login(email, "test1234");
        check("login works", logged.getId() == me.getId());
        try {
            auth.login(email, "wrong");
            check("wrong password rejected", false);
        } catch (IllegalArgumentException e) {
            check("wrong password rejected", true);
        }
        try {
            auth.register("Dup", email, "test1234", "test1234");
            check("duplicate email rejected", false);
        } catch (IllegalArgumentException e) {
            check("duplicate email rejected", true);
        }

        // 2) menu browse: veg filter + sort + search
        List<MenuItem> all = menu.browseMenu("", null, false,
                MenuService.SortOrder.NAME_ASC);
        check("seed menu loaded (>=15 items)", all.size() >= 15);
        List<MenuItem> veg = menu.browseMenu("", null, true,
                MenuService.SortOrder.PRICE_ASC);
        check("veg-only filter", veg.stream().allMatch(MenuItem::isVeg)
                && veg.size() < all.size());
        check("price sort ascending", isSortedAsc(veg));
        List<MenuItem> dosa = menu.browseMenu("dosa", null, false,
                MenuService.SortOrder.NAME_ASC);
        check("search finds Masala Dosa",
                dosa.stream().anyMatch(m -> m.getName().contains("Dosa")));

        // 3) cart rules
        Cart cart = new Cart();
        MenuItem first = all.get(0);
        cart.addItem(first, 2);
        check("cart subtotal via streams",
                Math.abs(cart.getSubtotal() - first.getPrice() * 2) < 0.001);
        try {
            cart.addItem(first, 10_000);
            check("over-stock add rejected", false);
        } catch (ItemUnavailableException e) {
            check("over-stock add rejected", true);
        }

        // 4) checkout with cash: prices recomputed from DB
        Order placed = orders.placeOrder(me, cart, new CashOnPickupPayment(),
                "no onion");
        double expectedSubtotal = Math.round(first.getPrice() * 2 * 100) / 100.0;
        double expectedTotal = Math.round(
                (expectedSubtotal + OrderService.PACKAGING_CHARGES) * 100) / 100.0;
        check("order placed with id", placed.getId() > 0);
        check("subtotal recomputed from DB",
                Math.abs(placed.getSubtotal() - expectedSubtotal) < 0.001);
        check("charges + total correct",
                Math.abs(placed.getTotal() - expectedTotal) < 0.001);
        check("status starts PLACED", placed.getStatus() == OrderStatus.PLACED);
        check("payment method recorded",
                placed.getPaymentMethod()
                        == com.canteen.model.PaymentMethod.CASH_ON_PICKUP);

        // 5) checkout with simulated UPI
        Cart cart2 = new Cart();
        cart2.addItem(all.get(1), 1);
        Order upiOrder = orders.placeOrder(me, cart2,
                new SimulatedUpiPayment(), "");
        check("simulated UPI order placed",
                upiOrder.getPaymentMethod()
                        == com.canteen.model.PaymentMethod.UPI_SIMULATED);

        // 6) empty cart rejected
        try {
            orders.placeOrder(me, new Cart(), new CashOnPickupPayment(), "");
            check("empty cart rejected", false);
        } catch (EmptyCartException e) {
            check("empty cart rejected", true);
        }

        // 7) status transitions
        try {
            orders.advanceStatus(placed.getId(), OrderStatus.READY_FOR_PICKUP,
                    "skip");
            check("invalid transition rejected", false);
        } catch (IllegalStateException e) {
            check("invalid transition rejected", true);
        }
        Order confirmed = orders.advanceStatus(placed.getId(),
                OrderStatus.CONFIRMED, "ok");
        check("valid transition PLACED->CONFIRMED",
                confirmed.getStatus() == OrderStatus.CONFIRMED);

        // 8) reports
        double rev = reports.revenueOn(java.time.LocalDate.now());
        check("today revenue > 0", rev > 0);
        List<ReportService.TopItem> top = reports.topSellingItems(5);
        check("top sellers list non-empty", !top.isEmpty());
        check("my orders visible",
                orders.myOrders(me.getId()).size() >= 2);

        System.out.println("\nSMOKE TEST: " + passed + " passed, "
                + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static boolean isSortedAsc(List<MenuItem> items) {
        for (int i = 1; i < items.size(); i++) {
            if (items.get(i).getPrice() < items.get(i - 1).getPrice()) {
                return false;
            }
        }
        return true;
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("  PASS: " + name);
        } else {
            failed++;
            System.out.println("  FAIL: " + name);
        }
    }
}
