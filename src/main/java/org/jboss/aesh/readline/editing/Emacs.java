/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.readline.editing;

import org.jboss.aesh.edit.actions.Action;
import org.jboss.aesh.readline.KeyEvent;

import java.awt.event.ActionEvent;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class Emacs implements Mode {

    private EditStatus status = EditStatus.EDIT;

    private ActionMapper actionMapper = ActionMapper.getEmacs();

    public Emacs() {

    }

    @Override
    public ActionEvent parse(KeyEvent event) {
        return null;
    }
}
