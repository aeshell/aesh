package org.jboss.aesh.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable value object that contain a parsed command line.
 * The command line is splitted into words based on white spaces.
 * Escaped whitespaces, single and double quotes are also parsed.
 *
 * This object have not been populated to any Commands yet.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshLine {
    private boolean error;
    private String errorMessage;
    private List<String> words;

    public AeshLine(List<String> words, boolean error, String errorMessage) {
        if(words == null)
            this.words = new ArrayList<String>(0);
        else {
            this.words = new ArrayList<String>(words.size());
            this.words.addAll(words);
        }

        this.error = error;
        this.errorMessage = errorMessage;
    }

    public boolean hasError() {
        return error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<String> getWords() {
        return words;
    }
}
