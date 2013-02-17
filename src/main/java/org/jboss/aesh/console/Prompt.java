/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.terminal.TerminalCharacter;

import java.util.List;

/**
 * The Prompt:
 * If created with a String value that value will be the prompt
 * with the default back and foreground colors.
 * If created with TerminalCharacters the colors can be set individually.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class Prompt {

    private List<TerminalCharacter> characters;
    private String prompt;
    private Character mask;

    public Prompt(String prompt) {
        if(prompt != null)
            this.prompt = prompt;
        else
            this.prompt = "";
    }

    public Prompt(String prompt, Character mask) {
        if(prompt != null)
            this.prompt = prompt;
        else
            this.prompt = "";
        this.mask = mask;
    }

    public Prompt(List<TerminalCharacter> characters) {
        this.characters = characters;
        StringBuilder builder = new StringBuilder(characters.size());
        for(TerminalCharacter c : characters)
            builder.append(c.getCharacter());

        this.prompt = builder.toString();
    }

    public Prompt(List<TerminalCharacter> characters, Character mask) {
        this.characters = characters;
        StringBuilder builder = new StringBuilder(characters.size());
        for(TerminalCharacter c : characters)
            builder.append(c.getCharacter());

        this.prompt = builder.toString();
        this.mask = mask;
    }

    public Character getMask() {
        return mask;
    }

    public boolean isMasking() {
        return mask != null;
    }

    public String getPromptAsString() {
        return prompt;
    }

    public int getLength() {
        return prompt.length();
    }

    public boolean hasChars() {
        return characters != null;
    }

    public List<TerminalCharacter> getCharacters() {
        return characters;
    }
}
