package com.privacyshield;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test for the Swing window. It is skipped when no display is available,
 * so the normal test run also works on a build server.
 */
class PrivacyShieldFrameTest {

    @Test
    void frameCanBeCreatedAndDisposed() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "No display available");

        final PrivacyShieldFrame[] frame = new PrivacyShieldFrame[1];

        SwingUtilities.invokeAndWait(() -> frame[0] = new PrivacyShieldFrame());

        assertNotNull(frame[0]);
        assertTrue(frame[0].getTitle().startsWith("PrivacyShield"));

        SwingUtilities.invokeAndWait(() -> frame[0].dispose());
    }
}
