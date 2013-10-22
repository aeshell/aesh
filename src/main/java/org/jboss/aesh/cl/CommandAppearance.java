/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.terminal.CharacterType;
import org.jboss.aesh.terminal.Color;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public @interface CommandAppearance {

    Color textColor() default Color.DEFAULT_TEXT;

    Color backgroundColor() default Color.DEFAULT_BG;

    CharacterType textType() default CharacterType.NORMAL;

    Color optionTextColor() default Color.DEFAULT_TEXT;

    Color optionBackgroundColor() default Color.DEFAULT_BG;

    CharacterType optionTextType() default CharacterType.NORMAL;

    CharacterType requiredOptionTextType() default CharacterType.NORMAL;

    Color requiredOptionTextColor() default Color.DEFAULT_TEXT;

}
