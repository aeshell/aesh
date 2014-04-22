package org.jboss.aesh.console.command;

import org.jboss.aesh.console.command.invocation.CommandInvocation;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface Formatter<T> {
    void format(T output, CommandInvocation commandInvocation);
}
