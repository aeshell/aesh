/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.renderer;

import org.jboss.aesh.terminal.CharacterType;
import org.jboss.aesh.terminal.Color;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class NullOptionRenderer implements OptionRenderer {
    @Override
    public Color getTextColor() {
        return Color.DEFAULT_TEXT;
    }

    @Override
    public Color getBackgroundColor() {
        return Color.DEFAULT_BG;
    }

    @Override
    public CharacterType getTextType() {
        return CharacterType.NORMAL;
    }
}
