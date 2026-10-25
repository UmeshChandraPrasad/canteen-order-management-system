package com.canteen.ui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Application entry point. Run this class to start the canteen app. */
public class CanteenApp {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("[CanteenApp] System look-and-feel unavailable: "
                    + e.getMessage());
        }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
