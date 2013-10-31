package org.jboss.aesh.console.helper;

import java.io.InputStream;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface ManProvider {
    /**
     * Based on a command name, return the InputStream of
     * the man documentation for the specific command.
     * @param commandName command
     * @return stream
     */
    InputStream getManualDocument(String commandName);
}
