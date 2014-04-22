package org.jboss.aesh.console;

import org.jboss.aesh.console.command.Result;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class DefaultCallbackResult implements CallbackResult {

    private final Result result;

    public DefaultCallbackResult(Result result) {
        this.result = result;
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public ConsoleFormatter getConsoleFormatter() {
        return null;
    }

    @Override
    public Result getResult() {
        return result;
    }
}
