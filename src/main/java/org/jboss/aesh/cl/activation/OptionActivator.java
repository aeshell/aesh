package org.jboss.aesh.cl.activation;

import org.jboss.aesh.cl.internal.ProcessedCommand;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface OptionActivator {

    /**
     * Activates the current option to be "visible".
     * Eg: While completing options this option will not be
     * displayed if this method return false.
     *
     * @param processedCommand
     * @return
     */
    boolean isActivated(ProcessedCommand processedCommand);
}
