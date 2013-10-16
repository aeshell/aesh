package org.jboss.aesh.cl.activation;

import org.jboss.aesh.cl.internal.ProcessedCommand;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class NullActivator implements OptionActivator {
    @Override
    public boolean isActivated(ProcessedCommand processedCommand) {
        return true;
    }
}
