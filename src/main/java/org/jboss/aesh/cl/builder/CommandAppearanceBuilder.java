/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.builder;

import org.jboss.aesh.cl.internal.ProcessedCommandAppearance;
import org.jboss.aesh.terminal.CharacterType;
import org.jboss.aesh.terminal.Color;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandAppearanceBuilder {
    private Color textColor;
    private Color backgroundColor;
    private CharacterType textType;
    private Color optionTextColor;
    private Color optionBackgroundColor;
    private CharacterType optionTextType;
    private CharacterType requiredOptionTextType;
    private Color requiredOptionTextColor;

    public CommandAppearanceBuilder() {

    }

    public CommandAppearanceBuilder textColor(Color textColor) {
        this.textColor = textColor;
        return this;
    }

     public CommandAppearanceBuilder backgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    public CommandAppearanceBuilder textType(CharacterType textType) {
        this.textType = textType;
        return this;
    }

    public CommandAppearanceBuilder optionTextColor(Color optionTextColor) {
        this.optionTextColor = optionTextColor;
        return this;
    }

     public CommandAppearanceBuilder optionBackgroundColor(Color optionBackgroundColor) {
        this.optionBackgroundColor = optionBackgroundColor;
        return this;
    }

    public CommandAppearanceBuilder optionTextType(CharacterType optionTextType) {
        this.optionTextType = optionTextType;
        return this;
    }

    public CommandAppearanceBuilder requiredOptionTextType(CharacterType requiredOptionTextType) {
        this.requiredOptionTextType = requiredOptionTextType;
        return this;
    }

    public CommandAppearanceBuilder requiredOptionTextColor(Color requiredOptionTextColor) {
        this.requiredOptionTextColor = requiredOptionTextColor;
        return this;
    }

    public ProcessedCommandAppearance generateAppearance() {
        if(textColor == null)
            textColor = Color.DEFAULT_TEXT;
        if(backgroundColor == null)
            backgroundColor = Color.DEFAULT_BG;
        if(textType == null)
            textType = CharacterType.NORMAL;
        if(optionTextColor == null)
            optionTextColor = Color.DEFAULT_TEXT;
        if(optionBackgroundColor == null)
            optionBackgroundColor = Color.DEFAULT_BG;
        if(requiredOptionTextColor == null)
            requiredOptionTextColor = Color.DEFAULT_TEXT;
        if(requiredOptionTextType == null)
            requiredOptionTextType = CharacterType.NORMAL;

        return new ProcessedCommandAppearance(textColor, backgroundColor, textType,
                optionTextColor, optionBackgroundColor,
                optionTextType, requiredOptionTextType, requiredOptionTextColor);
    }

}
