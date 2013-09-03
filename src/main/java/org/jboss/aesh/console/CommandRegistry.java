package org.jboss.aesh.console;

import java.util.Map;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface CommandRegistry {

    CommandContainer getCommand(String name) throws CommandNotFoundException;

    Map<String, CommandContainer> asMap();
}
