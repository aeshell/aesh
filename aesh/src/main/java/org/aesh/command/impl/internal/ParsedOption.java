package org.aesh.command.impl.internal;

public class ParsedOption {

    private final ProcessedOption processedOption;

    public ParsedOption(ProcessedOption po) {
        this.processedOption = po;
    }

    public String value() {
        if(processedOption != null)
            return processedOption.getValue();
        else
            return null;
    }

    public String name() {
        if(processedOption != null)
            return processedOption.name();
        else
            return null;

    }
}
