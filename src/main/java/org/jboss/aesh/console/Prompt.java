/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.terminal.TerminalCharacter;
import org.jboss.aesh.terminal.TerminalString;

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

    private String prompt;
    private Character mask;
    private String ansiString;

    public Prompt(String prompt) {
        if(prompt != null)
            this.prompt = prompt;
        else
            this.prompt = "";
    }

    public Prompt(String prompt, String ansiString) {
        if(prompt != null)
            this.prompt = prompt;
        else
            this.prompt = "";
        this.ansiString = ansiString;
    }

    public Prompt(String prompt, Character mask) {
        if(prompt != null)
            this.prompt = prompt;
        else
            this.prompt = "";
        this.mask = mask;
    }

    public Prompt(TerminalString terminalString) {
        if(terminalString != null) {
            ansiString = terminalString.toString();
            this.prompt = terminalString.getCharacters();
        }
        else
            this.prompt = "";
    }

    public Prompt(List<TerminalCharacter> characters) {
        generateOutString(characters);
    }

    public Prompt(List<TerminalCharacter> characters, Character mask) {
        this.mask = mask;
        generateOutString(characters);
    }

    private void generateOutString(List<TerminalCharacter> chars) {
        StringBuilder promptBuilder = new StringBuilder();
        StringBuilder builder = new StringBuilder();
        TerminalCharacter prev = null;
        for(TerminalCharacter c : chars) {
            if(prev == null)
                builder.append(c.toString());
            else
                builder.append(c.toString(prev));
            prev = c;
            promptBuilder.append(c.getCharacter());
        }
        ansiString = builder.toString();
        this.prompt = promptBuilder.toString();
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

    public boolean hasANSI() {
        return ansiString != null;
    }

    public String getANSI() {
        return ansiString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Prompt)) return false;

        Prompt prompt1 = (Prompt) o;

        if (ansiString != null ? !ansiString.equals(prompt1.ansiString) : prompt1.ansiString != null) return false;
        if (mask != null ? !mask.equals(prompt1.mask) : prompt1.mask != null) return false;
        if (!prompt.equals(prompt1.prompt)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ansiString != null ? ansiString.hashCode() : 0;
        result = 31 * result + prompt.hashCode();
        result = 31 * result + (mask != null ? mask.hashCode() : 0);
        return result;
    }
}
