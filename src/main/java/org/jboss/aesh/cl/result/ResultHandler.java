package org.jboss.aesh.cl.result;

import org.jboss.aesh.console.command.Result;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface ResultHandler {

    void onSuccess();

    void onFailure(Result result);

    void onValidationFailure(Result result);
}