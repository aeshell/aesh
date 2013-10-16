package org.jboss.aesh.cl.validator;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface OptionValidator<T> {
    /**
     * Validate if the given value is valid
     * @param value
     * @throws OptionValidatorException if not valid
     */
    void validate(T value) throws OptionValidatorException;
}
