package org.jboss.aesh.cl.result;

import org.jboss.aesh.console.command.Result;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class NullResultHandler implements ResultHandler {

    @Override
    public void onSuccess() {
    }

    @Override
    public void onFailure(Result result) {

    }

    @Override
    public void onValidationFailure(Result result) {

    }
}
