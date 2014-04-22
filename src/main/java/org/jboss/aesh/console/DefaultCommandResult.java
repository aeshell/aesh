package org.jboss.aesh.console;

import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.Formatter;
import org.jboss.aesh.console.command.Result;

import java.util.Set;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class DefaultCommandResult implements CommandResult<Set<Object>, Formatter<Set<Object>>> {

    private Result result;
    private Set<Object> values;
    private Formatter<Set<Object>> formatter;

    public DefaultCommandResult(Result result) {
        this.result = result;
    }

    public DefaultCommandResult(Result result, Set<Object> values) {
        this.result = result;
        this.values = values;
    }

    public DefaultCommandResult(Result result, Set<Object> values,
                                Formatter<Set<Object>> formatter) {
        this.result = result;
        this.values = values;
        this.formatter = formatter;
    }

    @Override
    public Set<Object> getValue() {
        return values;
    }

    @Override
    public Formatter<Set<Object>> getFormatter() {
        return formatter;
    }

    @Override
    public Result getResult() {
        return result;
    }
}
