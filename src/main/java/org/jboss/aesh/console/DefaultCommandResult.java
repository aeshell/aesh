package org.jboss.aesh.console;

import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.Formatter;
import org.jboss.aesh.console.command.Result;

import java.util.Set;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class DefaultCommandResult implements CommandResult<Set<Object>, Formatter<Set<Object>, CommandInvocation>> {

    private Result result;

    public DefaultCommandResult(Result result) {
        this.result = result;
    }

    @Override
    public Result getResult() {
        return result;
    }
}
