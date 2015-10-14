/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.readline.editing;

import org.jboss.aesh.readline.Action;
import org.jboss.aesh.readline.KeyEvent;

import java.util.logging.Logger;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class Emacs implements Mode {

    private EditStatus status = EditStatus.EDIT;

    private ActionMapper actionMapper = ActionMapper.getEmacs();

    private static final Logger LOGGER = Logger.getLogger(Emacs.class.getName());

    public Emacs() {
    }

    @Override
    public Action parse(KeyEvent event) {
        if(actionMapper.getMapping().containsKey(event)) {
            return actionMapper.getMapping().get(event);

        }
        else {
            LOGGER.info("have not mapped: "+event);
            return null;
        }
    }
}
