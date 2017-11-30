package org.aesh.command.impl.internal;

import org.aesh.command.Command;

public class ParsedCommand {

    private final ProcessedCommand processedCommand;

    public ParsedCommand(ProcessedCommand pc) {
        this.processedCommand = pc;
    }

    public ParsedOption findLongOption(String name) {
        ProcessedOption po = processedCommand.findLongOption(name);
        if(po != null)
            return new ParsedOption(po);
        else
            return null;

    }

    public Command getCommand() {
        return processedCommand.getCommand();
    }
}
