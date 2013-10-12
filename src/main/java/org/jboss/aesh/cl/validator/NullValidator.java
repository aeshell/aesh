package org.jboss.aesh.cl.validator;

import org.jboss.aesh.cl.internal.ProcessedCommand;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class NullValidator implements OptionValidator {
    @Override
    public void validate(Object value) throws OptionValidatorException {
    }

    /**
     * @return true
     */
    @Override
    public boolean isEnabled(ProcessedCommand processedCommand) {
        return true;
    }

}
