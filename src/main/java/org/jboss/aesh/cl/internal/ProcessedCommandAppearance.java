/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.internal;

import org.jboss.aesh.terminal.CharacterType;
import org.jboss.aesh.terminal.Color;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ProcessedCommandAppearance {
    private Color textColor;
    private Color backgroundColor;
    private CharacterType textType;
    private Color optionTextColor;
    private Color optionBackgroundColor;
    private CharacterType optionTextType;
    private CharacterType requiredOptionTextType;
    private Color requiredOptionTextColor;

    public ProcessedCommandAppearance() {
        this(Color.DEFAULT_TEXT, Color.DEFAULT_BG, CharacterType.NORMAL,
                Color.DEFAULT_TEXT, Color.DEFAULT_BG, CharacterType.NORMAL,
                CharacterType.NORMAL, Color.DEFAULT_TEXT);
    }

    public ProcessedCommandAppearance(Color textColor, Color backgroundColor, CharacterType textType,
                                      Color optionTextColor, Color optionBackgroundColor,
                                      CharacterType optionTextType, CharacterType requiredOptionTextType,
                                      Color requiredOptionTextColor) {
        this.textColor = textColor;
        this.backgroundColor = backgroundColor;
        this.textType = textType;
        this.optionTextColor = optionTextColor;
        this.optionBackgroundColor = optionBackgroundColor;
        this.optionTextType = optionTextType;
        this.requiredOptionTextType = requiredOptionTextType;
        this.requiredOptionTextColor = requiredOptionTextColor;
    }

    public Color getTextColor() {
        return textColor;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public CharacterType getTextType() {
        return textType;
    }

    public Color getOptionTextColor() {
        return optionTextColor;
    }

    public Color getOptionBackgroundColor() {
        return optionBackgroundColor;
    }

    public CharacterType getOptionTextType() {
        return optionTextType;
    }

    public CharacterType getRequiredOptionTextType() {
        return requiredOptionTextType;
    }

    public Color getRequiredOptionTextColor() {
        return requiredOptionTextColor;
    }
}
