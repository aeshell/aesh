package org.jboss.aesh.cl.validator;

import org.jboss.aesh.console.command.Command;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface CommandValidator<T extends Command> {

    /**
     * Validate that all the option values combined are valid
     *
     * @param command command
     * @throws CommandValidatorException
     */
    void validate(T command) throws CommandValidatorException;
}
