package org.aesh.command.impl.internal;

public class ParsedOption {

    private final ProcessedOption processedOption;

    public ParsedOption(ProcessedOption po) {
        this.processedOption = po;
    }

    public String value() {
        return processedOption.getValue();
    }

    public String name() {
        return processedOption.name();
    }
}
