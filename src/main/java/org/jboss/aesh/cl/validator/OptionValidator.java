package org.jboss.aesh.cl.validator;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface OptionValidator<T> {
    void validate(T value) throws OptionValidatorException;
}
