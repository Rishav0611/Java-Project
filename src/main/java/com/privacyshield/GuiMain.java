package com.privacyshield;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingUtilities;

/**
 * Starts the PrivacyShield desktop window.
 */
public final class GuiMain {
    private GuiMain() {
    }

    public static void main(String[] args) {
        // FlatLaf gives the window a modern look and a matching dark theme.
        FlatLightLaf.setup();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new PrivacyShieldFrame().setVisible(true);
            }
        });
    }
}
