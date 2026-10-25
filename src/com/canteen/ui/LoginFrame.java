package com.canteen.ui;

import com.canteen.model.User;
import com.canteen.service.AuthService;
import com.canteen.util.Db;
import com.canteen.util.Ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

/**
 * Unit IV — GUI & Event Handling: login screen. Buttons use
 * ActionListener (delegation event model); the frame uses a
 * WindowListener to confirm exit.
 */
@SuppressWarnings("serial")
public class LoginFrame extends JFrame {

    private final AuthService auth = new AuthService();
    private final JTextField emailField = new JTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);
    private final JButton loginButton = new JButton("Log In");
    private final JButton registerButton = new JButton("Create account");

    public LoginFrame() {
        super("Canteen — Log In");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(420, 380);
        setLocationRelativeTo(null);

        // Unit IV — WindowListener (delegation event model)
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (Ui.confirm(LoginFrame.this, "Exit the canteen app?")) {
                    dispose();
                    System.exit(0);
                }
            }
        });

        JPanel card = new JPanel(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        card.setBackground(Color.WHITE);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Campus Canteen", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        JLabel sub = new JLabel("Order ahead, skip the queue.", SwingConstants.CENTER);
        sub.setForeground(Color.DARK_GRAY);

        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2;
        card.add(title, gc);
        gc.gridy = 1;
        card.add(sub, gc);

        gc.gridwidth = 1; gc.gridy = 2;
        card.add(new JLabel("Email:"), gc);
        gc.gridx = 1;
        card.add(emailField, gc);

        gc.gridx = 0; gc.gridy = 3;
        card.add(new JLabel("Password:"), gc);
        gc.gridx = 1;
        card.add(passwordField, gc);

        gc.gridx = 0; gc.gridy = 4; gc.gridwidth = 2;
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.add(loginButton, gc);
        gc.gridy = 5;
        registerButton.setContentAreaFilled(false);
        registerButton.setForeground(new Color(0x1a73e8));
        registerButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.add(registerButton, gc);

        add(card, BorderLayout.CENTER);
        getRootPane().setDefaultButton(loginButton);

        // Unit IV — ActionListener: delegation event model
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doLogin();
            }
        });
        registerButton.addActionListener(e ->
                new RegisterDialog(LoginFrame.this).setVisible(true));

        // Check the database is reachable as soon as the window opens.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                checkDatabase();
            }
        });
    }

    /** Unit II — DB check on a worker thread so the UI never freezes. */
    private void checkDatabase() {
        setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try (Connection c = Db.getConnection()) {
                    // Opening the connection proves reachability; a live
                    // ping makes sure the server really answers.
                    if (!c.isValid(3)) {
                        throw new java.sql.SQLException(
                                "Database server did not respond to ping.");
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                setEnabled(true);
                try {
                    get();
                } catch (Exception ex) {
                    Ui.error(LoginFrame.this,
                            "Cannot reach the database.\n\n"
                            + "1. Make sure MySQL 8 is running\n"
                            + "2. Run db/schema.sql and db/seed.sql once\n"
                            + "3. Check db.user / db.password in config.properties",
                            ex);
                }
            }
        }.execute();
    }

    /** Unit II — login runs on a SwingWorker; result handled on the EDT. */
    private void doLogin() {
        String email = emailField.getText();
        String password = new String(passwordField.getPassword());
        loginButton.setEnabled(false);
        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws Exception {
                return auth.login(email, password);
            }

            @Override
            protected void done() {
                loginButton.setEnabled(true);
                try {
                    User user = get();
                    dispose();
                    new MainFrame(user).setVisible(true);
                } catch (Exception ex) {
                    // Friendly message from AuthService, never a stack trace.
                    Ui.error(LoginFrame.this, friendly(ex));
                }
            }
        }.execute();
    }

    private static String friendly(Exception ex) {
        Throwable t = ex.getCause() != null ? ex.getCause() : ex;
        if (t instanceof IllegalArgumentException) {
            return t.getMessage();
        }
        return "Could not log you in. Please check the database connection"
                + " and try again.";
    }
}
