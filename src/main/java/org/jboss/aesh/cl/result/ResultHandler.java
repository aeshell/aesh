package org.jboss.aesh.cl.result;

import org.jboss.aesh.console.command.CommandResult;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface ResultHandler {

    void onSuccess();

    void onFailure(CommandResult result);

    void onValidationFailure(CommandResult result, Exception exception);
}