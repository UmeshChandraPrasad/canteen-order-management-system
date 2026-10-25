package com.canteen.ui;

import com.canteen.model.Category;
import com.canteen.model.MenuItem;
import com.canteen.service.MenuService;
import com.canteen.util.Ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/** Admin "Manage Menu": full CRUD plus the availability toggle. */
@SuppressWarnings("serial")
public class MenuAdminPanel extends JPanel {

    private final MenuService menuService = new MenuService();
    private final DefaultTableModel model =
            new DefaultTableModel(new String[]{
                    "ID", "Name", "Category", "Price",
                    "Veg", "Available", "Stock"}, 0) {
                @Override
                public boolean isCellEditable(int r, int c) { return false; }
            };
    private final JTable table = new JTable(model);
    private List<MenuItem> current = List.of();
    private List<Category> categories = List.of();

    public MenuAdminPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("Add Item");
        JButton editBtn = new JButton("Edit Selected");
        JButton toggleBtn = new JButton("Toggle Availability");
        JButton deleteBtn = new JButton("Delete");
        JButton refreshBtn = new JButton("Refresh");
        addBtn.addActionListener(e -> openEditor(null));
        editBtn.addActionListener(e -> {
            MenuItem m = selected();
            if (m == null) {
                Ui.error(this, "Select an item in the table first.");
            } else {
                openEditor(m);
            }
        });
        toggleBtn.addActionListener(e -> toggleSelected());
        deleteBtn.addActionListener(e -> deleteSelected());
        refreshBtn.addActionListener(e -> load());
        buttons.add(addBtn);
        buttons.add(editBtn);
        buttons.add(toggleBtn);
        buttons.add(deleteBtn);
        buttons.add(refreshBtn);
        add(buttons, BorderLayout.SOUTH);

        load();
    }

    private MenuItem selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : current.get(row);
    }

    private void load() {
        new SwingWorker<List<MenuItem>, Void>() {
            @Override
            protected List<MenuItem> doInBackground() throws Exception {
                categories = menuService.listCategories();
                return menuService.listAllItems();
            }

            @Override
            protected void done() {
                try {
                    current = get();
                    render();
                } catch (Exception ex) {
                    Ui.error(MenuAdminPanel.this,
                            "Could not load the menu.", ex);
                }
            }
        }.execute();
    }

    private void render() {
        model.setRowCount(0);
        for (MenuItem m : current) {
            model.addRow(new Object[]{
                    m.getId(), m.getName(), m.getCategoryName(),
                    Ui.money(m.getPrice()),
                    m.isVeg() ? "Yes" : "No",
                    m.isAvailable() ? "Yes" : "No",
                    m.getStockQty()
            });
        }
    }

    private void toggleSelected() {
        MenuItem m = selected();
        if (m == null) {
            Ui.error(this, "Select an item in the table first.");
            return;
        }
        boolean to = !m.isAvailable();
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                menuService.toggleAvailability(m.getId(), to);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    Ui.info(MenuAdminPanel.this, "'" + m.getName() + "' is now "
                            + (to ? "available" : "unavailable") + ".");
                    load();
                } catch (Exception ex) {
                    Ui.error(MenuAdminPanel.this,
                            "Could not update availability.", ex);
                }
            }
        }.execute();
    }

    private void deleteSelected() {
        MenuItem m = selected();
        if (m == null) {
            Ui.error(this, "Select an item in the table first.");
            return;
        }
        if (!Ui.confirm(this, "Delete '" + m.getName()
                + "'? Items used in past orders cannot be deleted.")) {
            return;
        }
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                menuService.deleteItem(m.getId());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    Ui.info(MenuAdminPanel.this, "Item deleted.");
                    load();
                } catch (Exception ex) {
                    Throwable t = ex.getCause() != null ? ex.getCause() : ex;
                    Ui.error(MenuAdminPanel.this, t.getMessage());
                }
            }
        }.execute();
    }

    private void openEditor(MenuItem existing) {
        new ItemDialog((JFrame) SwingUtilities.getWindowAncestor(this),
                existing, categories, () -> load()).setVisible(true);
    }

    /** Add/Edit dialog with full input validation. */
    @SuppressWarnings("serial")
    private static class ItemDialog extends JDialog {
        interface SavedListener {
            void saved();
        }

        private final MenuService service = new MenuService();
        private final MenuItem item; // null = new item
        private final JTextField nameField = new JTextField(24);
        private final JComboBox<Category> categoryCombo = new JComboBox<>();
        private final JTextArea descArea = new JTextArea(3, 24);
        private final JTextField priceField = new JTextField(10);
        private final JCheckBox vegBox = new JCheckBox("Veg", true);
        private final JCheckBox availBox = new JCheckBox("Available", true);
        private final JSpinner stockSpinner =
                new JSpinner(new SpinnerNumberModel(50, 0, 10000, 1));

        ItemDialog(JFrame owner, MenuItem existing, List<Category> categories,
                   SavedListener listener) {
            super(owner, existing == null ? "Add Menu Item" : "Edit Menu Item", true);
            this.item = existing == null ? new MenuItem() : copyOf(existing);
            for (Category c : categories) {
                categoryCombo.addItem(c);
            }
            if (existing != null) {
                nameField.setText(item.getName());
                descArea.setText(item.getDescription());
                priceField.setText(String.valueOf(item.getPrice()));
                vegBox.setSelected(item.isVeg());
                availBox.setSelected(item.isAvailable());
                stockSpinner.setValue(item.getStockQty());
                selectCategory(item.getCategoryId());
            }

            JPanel p = new JPanel(new java.awt.GridBagLayout());
            java.awt.GridBagConstraints gc = new java.awt.GridBagConstraints();
            gc.insets = new java.awt.Insets(5, 5, 5, 5);
            gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
            int row = 0;
            addRow(p, gc, row++, "Name:", nameField);
            addRow(p, gc, row++, "Category:", categoryCombo);
            addRow(p, gc, row++, "Description:", new JScrollPane(descArea));
            addRow(p, gc, row++, "Price (₹):", priceField);
            addRow(p, gc, row++, "Stock:", stockSpinner);
            gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
            JPanel checks = new JPanel(new FlowLayout(FlowLayout.LEFT));
            checks.add(vegBox);
            checks.add(availBox);
            p.add(checks, gc);

            JButton saveBtn = new JButton("Save");
            saveBtn.addActionListener(e -> {
                if (readForm()) {
                    saveAsync(listener);
                }
            });
            gc.gridy = row + 1;
            p.add(saveBtn, gc);

            add(p);
            pack();
            setLocationRelativeTo(owner);
            getRootPane().setDefaultButton(saveBtn);
        }

        private void addRow(JPanel p, java.awt.GridBagConstraints gc,
                            int row, String label, java.awt.Component field) {
            gc.gridx = 0; gc.gridy = row; gc.gridwidth = 1;
            p.add(new JLabel(label), gc);
            gc.gridx = 1;
            p.add(field, gc);
        }

        private void selectCategory(int id) {
            for (int i = 0; i < categoryCombo.getItemCount(); i++) {
                if (categoryCombo.getItemAt(i).getId() == id) {
                    categoryCombo.setSelectedIndex(i);
                    return;
                }
            }
        }

        private static MenuItem copyOf(MenuItem m) {
            MenuItem c = new MenuItem();
            c.setId(m.getId());
            c.setCategoryId(m.getCategoryId());
            c.setName(m.getName());
            c.setDescription(m.getDescription());
            c.setPrice(m.getPrice());
            c.setVeg(m.isVeg());
            c.setAvailable(m.isAvailable());
            c.setStockQty(m.getStockQty());
            return c;
        }

        /** Validate and copy the form into the item. */
        private boolean readForm() {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                Ui.error(this, "Item name cannot be empty.");
                return false;
            }
            double price;
            try {
                price = Double.parseDouble(priceField.getText().trim());
            } catch (NumberFormatException ex) {
                Ui.error(this, "Price must be a number (e.g. 45.50).");
                return false;
            }
            if (price <= 0) {
                Ui.error(this, "Price must be greater than zero.");
                return false;
            }
            Category cat = (Category) categoryCombo.getSelectedItem();
            if (cat == null) {
                Ui.error(this, "Please choose a category.");
                return false;
            }
            item.setName(name);
            item.setCategoryId(cat.getId());
            item.setDescription(descArea.getText().trim());
            item.setPrice(price);
            item.setVeg(vegBox.isSelected());
            item.setAvailable(availBox.isSelected());
            item.setStockQty((Integer) stockSpinner.getValue());
            return true;
        }

        private void saveAsync(SavedListener listener) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    service.saveItem(item);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        Ui.info(ItemDialog.this, "Menu item saved.");
                        listener.saved();
                        dispose();
                    } catch (Exception ex) {
                        Throwable t = ex.getCause() != null ? ex.getCause() : ex;
                        if (t instanceof IllegalArgumentException
                                || t instanceof SQLException) {
                            Ui.error(ItemDialog.this, t.getMessage());
                        } else {
                            Ui.error(ItemDialog.this,
                                    "Could not save the item.", t);
                        }
                    }
                }
            }.execute();
        }
    }
}
