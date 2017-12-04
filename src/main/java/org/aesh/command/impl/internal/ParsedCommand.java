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

    public ParsedOption getArgument() {
        if(processedCommand.hasArgument())
            return new ParsedOption(processedCommand.getArgument());
        else
            return null;
    }

    public ParsedOption getArguments() {
        if(processedCommand.hasArguments())
            return new ParsedOption(processedCommand.getArguments());
        else
            return null;
    }

    public ParsedOption findLongOptionNoActivatorCheck(String name) {
        return new ParsedOption(processedCommand.findLongOptionNoActivatorCheck(name));
    }

}
