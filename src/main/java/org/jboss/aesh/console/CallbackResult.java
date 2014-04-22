package org.jboss.aesh.console;

import org.jboss.aesh.console.command.Result;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface CallbackResult<T,F extends ConsoleFormatter> {
    T getValue();

    F getConsoleFormatter();

    Result getResult();
}
