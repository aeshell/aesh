package org.jboss.aesh.console.settings;

import org.jboss.aesh.terminal.Shell;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface CommandNotFoundHandler {

    void handleCommandNotFound(String line, Shell shell);
}
