package org.jboss.aesh.cl.validator;

import org.jboss.aesh.cl.internal.ProcessedCommand;

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

    /**
     * Enables the current option to not be "visible" if not
     * the requirement is set.
     * Eg. during completion this method will not be visible
     * if not this method returns true.
     *
     * @param processedCommand
     * @return
     */
    boolean isEnabled(ProcessedCommand processedCommand);
}
