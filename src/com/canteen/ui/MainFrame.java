package com.canteen.ui;

import com.canteen.model.Cart;
import com.canteen.model.User;
import com.canteen.util.Ui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;

/**
 * Main application window. Customers get Menu / Cart / My Orders;
 * admins get Dashboard / Orders / Menu / Reports.
 */
@SuppressWarnings("serial")
public class MainFrame extends JFrame {

    /** Panels that own background threads; stopped on logout/exit. */
    public interface Stoppable {
        void stop();
    }

    private final User user;
    private final Cart cart = new Cart();
    private final JTabbedPane tabs = new JTabbedPane();
    private final List<Stoppable> stoppables = new ArrayList<>();
    private int cartTabIndex = -1;

    public MainFrame(User user) {
        super("Campus Canteen — " + user.getFullName());
        this.user = user;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1024, 700);
        setLocationRelativeTo(null);

        // Unit IV — WindowListener confirms before quitting.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (Ui.confirm(MainFrame.this, "Log out and exit the canteen app?")) {
                    shutdown(true);
                }
            }
        });

        buildMenuBar();
        if (user.isAdmin()) {
            buildAdminTabs();
        } else {
            buildCustomerTabs();
        }
        add(tabs);
    }

    private void buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");

        JMenuItem logout = new JMenuItem("Log Out");
        logout.addActionListener(e -> {
            shutdown(false);
            new LoginFrame().setVisible(true);
        });

        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> {
            if (Ui.confirm(this, "Exit the canteen app?")) {
                shutdown(true);
            }
        });

        file.add(logout);
        file.addSeparator();
        file.add(exit);
        bar.add(file);
        setJMenuBar(bar);
    }

    private void buildCustomerTabs() {
        MenuPanel menu = new MenuPanel(cart, this::refreshCartTab);
        CartPanel cartPanel = new CartPanel(user, cart,
                () -> tabs.setSelectedIndex(2), // after order -> My Orders
                this::refreshCartTab);
        OrdersPanel orders = new OrdersPanel(user);
        stoppables.add(orders);

        tabs.addTab("Menu", menu);
        tabs.addTab("Cart", cartPanel);
        cartTabIndex = 1;
        tabs.addTab("My Orders", orders);
        refreshCartTab();
    }

    private void buildAdminTabs() {
        DashboardPanel dashboard = new DashboardPanel();
        KitchenPanel kitchen = new KitchenPanel();
        stoppables.add(kitchen);
        MenuAdminPanel menuAdmin = new MenuAdminPanel();
        ReportsPanel reports = new ReportsPanel();

        tabs.addTab("Dashboard", dashboard);
        tabs.addTab("Manage Orders", kitchen);
        tabs.addTab("Manage Menu", menuAdmin);
        tabs.addTab("Reports", reports);
    }

    /** Update the Cart tab title with the live item count. */
    public void refreshCartTab() {
        if (cartTabIndex >= 0) {
            int n = cart.getTotalQuantity();
            tabs.setTitleAt(cartTabIndex, n == 0 ? "Cart" : "Cart (" + n + ")");
        }
    }

    /**
     * Stop background threads and close this window.
     *
     * @param exitJvm true for a full quit (ends the JVM), false for a
     *                logout (the caller re-opens the login window next)
     */
    private void shutdown(boolean exitJvm) {
        for (Stoppable s : stoppables) {
            try {
                s.stop();
            } catch (Exception ignored) {
                // best effort
            }
        }
        dispose();
        if (exitJvm) {
            System.exit(0);
        }
    }
}
