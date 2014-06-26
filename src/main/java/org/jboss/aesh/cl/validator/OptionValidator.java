/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
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
