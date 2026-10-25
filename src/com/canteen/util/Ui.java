package com.canteen.util;

import java.awt.Component;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

/**
 * Small UI helpers: friendly dialogs (never raw stack traces) and
 * rupee formatting. Every background DB call in the UI goes through
 * SwingWorker so the interface never freezes (Unit II).
 */
public final class Ui {

    private Ui() {
    }

    /** Friendly error dialog — message only, no stack trace. */
    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message,
                "Something went wrong", JOptionPane.ERROR_MESSAGE);
    }

    /** Friendly error dialog that also logs the technical cause. */
    public static void error(Component parent, String message, Throwable cause) {
        System.err.println("[UI-ERROR] " + message);
        if (cause != null) {
            cause.printStackTrace(System.err);
        }
        error(parent, message);
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message,
                "Canteen", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String question) {
        return JOptionPane.showConfirmDialog(parent, question, "Please confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    /** 1234.5 -> "₹1,234.50". */
    public static String money(double amount) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        return nf.format(Math.round(amount * 100.0) / 100.0);
    }

    /**
     * Run {@code work} off the EDT and deliver the result back on it.
     * Used by screens that want a one-liner background call.
     */
    public static <T> void runAsync(BackgroundWork<T> work, UiCallback<T> callback) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.run();
            }

            @Override
            protected void done() {
                try {
                    callback.onSuccess(get());
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        }.execute();
    }

    @FunctionalInterface
    public interface BackgroundWork<T> {
        T run() throws Exception;
    }

    public interface UiCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }
}
