package com.canteen.ui;

import com.canteen.exception.ItemUnavailableException;
import com.canteen.model.Cart;
import com.canteen.model.Category;
import com.canteen.model.MenuItem;
import com.canteen.service.MenuService;
import com.canteen.util.Ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;

/**
 * Customer menu browser: text search, category pills, veg-only filter
 * and price/name sorting. Every reload runs on a SwingWorker (Unit II);
 * filtering/sorting are stream pipelines in MenuService (Unit III).
 */
@SuppressWarnings("serial")
public class MenuPanel extends JPanel {

    private final MenuService menuService = new MenuService();
    private final Cart cart;
    private final Runnable onCartChanged;

    private final JTextField searchField = new JTextField(18);
    private final JPanel pillsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    private final ButtonGroup pillGroup = new ButtonGroup();
    private final JCheckBox vegOnly = new JCheckBox("Veg only");
    private final JComboBox<MenuService.SortOrder> sortCombo =
            new JComboBox<>(MenuService.SortOrder.values());
    private final JPanel itemsPanel = new JPanel(new GridLayout(0, 3, 12, 12));

    private List<Category> categories = List.of();
    private Integer selectedCategoryId = null; // null = All

    public MenuPanel(Cart cart, Runnable onCartChanged) {
        super(new BorderLayout(8, 8));
        this.cart = cart;
        this.onCartChanged = onCartChanged;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildToolbar(), BorderLayout.NORTH);
        itemsPanel.setBackground(Color.WHITE);
        JScrollPane scroll = new JScrollPane(itemsPanel);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // Unit IV — delegation: every control funnels into reload().
        searchField.addActionListener(e -> reload());
        vegOnly.addActionListener(e -> reload());
        sortCombo.addActionListener(e -> reload());

        loadCategoriesThenMenu();
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Search:"));
        top.add(searchField);
        JButton searchBtn = new JButton("Go");
        searchBtn.addActionListener(e -> reload());
        top.add(searchBtn);
        top.add(vegOnly);
        top.add(new JLabel("Sort:"));
        top.add(sortCombo);
        bar.add(top, BorderLayout.NORTH);

        pillsPanel.setBorder(BorderFactory.createTitledBorder("Category"));
        bar.add(pillsPanel, BorderLayout.CENTER);
        return bar;
    }

    private void loadCategoriesThenMenu() {
        new SwingWorker<List<Category>, Void>() {
            @Override
            protected List<Category> doInBackground() throws Exception {
                return menuService.listCategories();
            }

            @Override
            protected void done() {
                try {
                    categories = get();
                    buildPills();
                    reload();
                } catch (Exception ex) {
                    Ui.error(MenuPanel.this,
                            "Could not load the menu. Check the database connection.", ex);
                }
            }
        }.execute();
    }

    private void buildPills() {
        pillsPanel.removeAll();
        pillGroup.clearSelection();
        addPill("All", null);
        for (Category c : categories) {
            addPill(c.getName(), c.getId());
        }
        pillsPanel.revalidate();
        pillsPanel.repaint();
    }

    private void addPill(String label, Integer categoryId) {
        JToggleButton pill = new JToggleButton(label, categoryId == null);
        pillGroup.add(pill);
        // Unit IV — ActionListener per pill (delegation event model)
        pill.addActionListener(e -> {
            selectedCategoryId = categoryId;
            reload();
        });
        pillsPanel.add(pill);
    }

    /** Reload the item grid on a background thread (Unit II). */
    private void reload() {
        String q = searchField.getText();
        Integer cat = selectedCategoryId;
        boolean veg = vegOnly.isSelected();
        MenuService.SortOrder sort = (MenuService.SortOrder) sortCombo.getSelectedItem();

        new SwingWorker<List<MenuItem>, Void>() {
            @Override
            protected List<MenuItem> doInBackground() throws Exception {
                return menuService.browseMenu(q, cat, veg, sort);
            }

            @Override
            protected void done() {
                try {
                    renderItems(get());
                } catch (Exception ex) {
                    Ui.error(MenuPanel.this,
                            "Could not load the menu. Check the database connection.", ex);
                }
            }
        }.execute();
    }

    private void renderItems(List<MenuItem> items) {
        itemsPanel.removeAll();
        if (items.isEmpty()) {
            itemsPanel.setLayout(new BorderLayout());
            itemsPanel.add(new JLabel("No items match. Try clearing the filters.",
                    JLabel.CENTER), BorderLayout.CENTER);
        } else {
            itemsPanel.setLayout(new GridLayout(0, 3, 12, 12));
            for (MenuItem m : items) {
                itemsPanel.add(itemCard(m));
            }
        }
        itemsPanel.revalidate();
        itemsPanel.repaint();
    }

    private JPanel itemCard(MenuItem m) {
        JPanel card = new JPanel(new BorderLayout(6, 6));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xDDDDDD)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        card.setBackground(Color.WHITE);
        card.setPreferredSize(new Dimension(220, 190));

        JLabel name = new JLabel(m.getName());
        name.setFont(name.getFont().deriveFont(Font.BOLD, 14f));

        JLabel veg = new JLabel(m.isVeg() ? "\u25CF VEG" : "\u25CF NON-VEG");
        veg.setForeground(m.isVeg() ? new Color(0x1B7A2E) : new Color(0xB3261E));
        veg.setFont(veg.getFont().deriveFont(Font.BOLD, 11f));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.add(name, BorderLayout.CENTER);
        header.add(veg, BorderLayout.EAST);

        JTextArea desc = new JTextArea(m.getDescription());
        desc.setEditable(false);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setBackground(Color.WHITE);
        desc.setForeground(Color.DARK_GRAY);
        desc.setFont(desc.getFont().deriveFont(12f));

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(Color.WHITE);
        JLabel price = new JLabel(Ui.money(m.getPrice()));
        price.setFont(price.getFont().deriveFont(Font.BOLD, 14f));
        JSpinner qty = new JSpinner(new SpinnerNumberModel(1, 1,
                Math.max(1, m.getStockQty()), 1));
        JButton add = new JButton("Add");
        // Unit IV — ActionListener: add-to-cart (delegation event model)
        add.addActionListener(e -> {
            try {
                cart.addItem(m, (Integer) qty.getValue());
                onCartChanged.run();
                Ui.info(this, (Integer) qty.getValue() + " x " + m.getName()
                        + " added to cart.");
            } catch (ItemUnavailableException ex) {
                Ui.error(this, ex.getMessage());
            }
        });
        footer.add(price);
        footer.add(qty);
        footer.add(add);

        card.add(header, BorderLayout.NORTH);
        card.add(desc, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }
}
