package org.jboss.aesh.cl.result;

import org.jboss.aesh.console.command.CommandResult;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class NullResultHandler implements ResultHandler {

    @Override
    public void onSuccess() {
    }

    @Override
    public void onFailure(CommandResult result) {
    }

    @Override
    public void onValidationFailure(CommandResult result, Exception exception) {
    }
}
