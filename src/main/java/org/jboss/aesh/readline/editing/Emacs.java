/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.readline.editing;

import org.jboss.aesh.readline.Action;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.util.LoggerUtil;

import java.util.logging.Logger;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class Emacs extends BaseEditMode {

    private Status status = Status.EDIT;

    private EditModeMapper editModeMapper = EditModeMapper.getEmacs();

    private static final Logger LOGGER = LoggerUtil.getLogger(Emacs.class.getName());

    public Emacs() {
    }

    @Override
    public Action parse(Key event) {
        if(editModeMapper.getMapping().containsKey(event)) {
            return editModeMapper.getMapping().get(event);
        }
        else {
            return null;
        }
    }
}
