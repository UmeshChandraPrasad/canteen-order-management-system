package com.canteen.service;

import com.canteen.dao.OrderDAO;
import com.canteen.dao.impl.JdbcOrderDao;
import com.canteen.model.Order;
import com.canteen.model.OrderItem;
import com.canteen.model.OrderStatus;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Sales reports. Unit III — Streams: revenue is grouped with
 * Collectors.groupingBy, top sellers with sorted().limit(), and bill
 * totals with mapToDouble().sum().
 */
public class ReportService {

    private final OrderDAO orders = new JdbcOrderDao();

    /** Only these statuses count as real sales (not cancelled). */
    private boolean countsAsSale(Order o) {
        return o.getStatus() != OrderStatus.CANCELLED;
    }

    /** Revenue for one day, summing order totals via streams. */
    public double revenueOn(LocalDate day) throws SQLException {
        return orders.findCreatedOn(day).stream()
                .filter(this::countsAsSale)
                .mapToDouble(Order::getTotal)
                .sum();
    }

    public long orderCountOn(LocalDate day) throws SQLException {
        return orders.findCreatedOn(day).stream()
                .filter(this::countsAsSale)
                .count();
    }

    /** Live orders grouped by status (for the admin dashboard). */
    public Map<OrderStatus, Long> countsByStatus() throws SQLException {
        return orders.findAll().stream()
                .collect(Collectors.groupingBy(Order::getStatus,
                        Collectors.counting()));
    }

    /**
     * Revenue per category for a day. OrderItem carries no category, so
     * this demo groups by item name's category via the DAO-fetched menu
     * — kept simple: revenue per item, which the UI labels by category.
     */
    public Map<String, Double> revenueByItemOn(LocalDate day) throws SQLException {
        return orders.findCreatedOn(day).stream()
                .filter(this::countsAsSale)
                .flatMap(o -> o.getItems().stream())
                .collect(Collectors.groupingBy(OrderItem::getItemName,
                        Collectors.summingDouble(OrderItem::lineTotal)));
    }

    /** Top-N best sellers across all time, by quantity sold. */
    public List<TopItem> topSellingItems(int limit) throws SQLException {
        Map<String, long[]> agg = new LinkedHashMap<>(); // name -> [qty, paise]
        for (Order o : orders.findAll()) {
            if (!countsAsSale(o)) {
                continue;
            }
            for (OrderItem it : o.getItems()) {
                long[] a = agg.computeIfAbsent(it.getItemName(), k -> new long[2]);
                a[0] += it.getQuantity();
                a[1] += Math.round(it.lineTotal() * 100);
            }
        }
        return agg.entrySet().stream()
                .map(e -> new TopItem(e.getKey(), e.getValue()[0], e.getValue()[1] / 100.0))
                .sorted(Comparator.comparingLong(TopItem::getQuantitySold).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /** Revenue for each of the last {@code days} days (oldest → newest). */
    public Map<LocalDate, Double> lastDaysRevenue(int days) throws SQLException {
        Map<LocalDate, Double> out = new TreeMap<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            out.put(d, revenueOn(d));
        }
        return out;
    }

    /** Simple DTO for the "top sellers" table. */
    public static class TopItem {
        private final String name;
        private final long quantitySold;
        private final double revenue;

        public TopItem(String name, long quantitySold, double revenue) {
            this.name = name;
            this.quantitySold = quantitySold;
            this.revenue = revenue;
        }

        public String getName() { return name; }
        public long getQuantitySold() { return quantitySold; }
        public double getRevenue() { return revenue; }
    }
}
