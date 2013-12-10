package org.jboss.aesh.cl.validator;

import org.jboss.aesh.console.command.validator.ValidatorInvocation;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface OptionValidator<T extends ValidatorInvocation> {
    /**
     * Validate if the given value is valid
     * @param validatorInvocation validator
     * @throws OptionValidatorException if not valid
     */
    void validate(T validatorInvocation) throws OptionValidatorException;
}
