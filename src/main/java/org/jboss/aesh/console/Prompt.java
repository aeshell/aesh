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

    public Prompt(String prompt) {
        this.prompt = prompt;
    }

    public Prompt(List<TerminalCharacter> characters) {
        this.characters = characters;
        StringBuilder builder = new StringBuilder(characters.size());
        for(TerminalCharacter c : characters)
            builder.append(c.getCharacter());

        this.prompt = builder.toString();
    }

    public String getPromptAsString() {
        return prompt;
    }

    public boolean hasChars() {
        return characters != null;
    }

    public List<TerminalCharacter> getCharacters() {
        return characters;
    }
}
