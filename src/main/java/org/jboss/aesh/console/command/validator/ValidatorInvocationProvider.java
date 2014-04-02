/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.command.validator;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface ValidatorInvocationProvider<T extends ValidatorInvocation, V, C> {
    T enhanceValidatorInvocation(ValidatorInvocation<V,C> validatorInvocation);
}
