/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.jreadline.edit;

import org.jboss.jreadline.edit.actions.Action;
import org.jboss.jreadline.edit.actions.Operation;

/**
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public interface EditMode {

    Operation parseInput(int[] input);

    Action getCurrentAction();

    Mode getMode();
}
