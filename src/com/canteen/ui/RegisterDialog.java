package com.canteen.ui;

import com.canteen.model.Customer;
import com.canteen.service.AuthService;
import com.canteen.util.Ui;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/** Self-service customer registration dialog with full validation. */
@SuppressWarnings("serial")
public class RegisterDialog extends JDialog {

    private final AuthService auth = new AuthService();
    private final JTextField nameField = new JTextField(22);
    private final JTextField emailField = new JTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);
    private final JPasswordField confirmField = new JPasswordField(22);
    private final JButton registerButton = new JButton("Register");

    public RegisterDialog(Frame owner) {
        super(owner, "Create Account", true);
        setSize(400, 320);
        setLocationRelativeTo(owner);

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        addRow(p, gc, 0, "Full name:", nameField);
        addRow(p, gc, 1, "Email:", emailField);
        addRow(p, gc, 2, "Password:", passwordField);
        addRow(p, gc, 3, "Confirm password:", confirmField);

        gc.gridx = 0; gc.gridy = 4; gc.gridwidth = 2;
        p.add(registerButton, gc);
        add(p);
        getRootPane().setDefaultButton(registerButton);

        // Unit IV — ActionListener (delegation event model)
        registerButton.addActionListener(e -> doRegister());
    }

    private void addRow(JPanel p, GridBagConstraints gc, int row,
                        String label, JTextField field) {
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 1;
        p.add(new JLabel(label), gc);
        gc.gridx = 1;
        p.add(field, gc);
    }

    private void doRegister() {
        registerButton.setEnabled(false);
        new SwingWorker<Customer, Void>() {
            @Override
            protected Customer doInBackground() throws Exception {
                return auth.register(
                        nameField.getText(),
                        emailField.getText(),
                        new String(passwordField.getPassword()),
                        new String(confirmField.getPassword()));
            }

            @Override
            protected void done() {
                registerButton.setEnabled(true);
                try {
                    Customer c = get();
                    Ui.info(RegisterDialog.this,
                            "Welcome, " + c.getFullName()
                            + "! Your account is ready — please log in.");
                    dispose();
                } catch (Exception ex) {
                    Throwable t = ex.getCause() != null ? ex.getCause() : ex;
                    if (t instanceof IllegalArgumentException) {
                        Ui.error(RegisterDialog.this, t.getMessage());
                    } else {
                        Ui.error(RegisterDialog.this,
                                "Could not create the account. Please try again.", t);
                    }
                }
            }
        }.execute();
    }
}
