package org.jboss.aesh.cl.validator;

import org.jboss.aesh.console.command.Command;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class NullCommandValidator implements CommandValidator {
    @Override
    public void validate(Command command) throws CommandValidatorException {
    }
}
