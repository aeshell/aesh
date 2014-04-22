package org.jboss.aesh.console;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface ConsoleFormatter<T> {
    void format(T output, ConsoleOperation consoleOperation);
}
