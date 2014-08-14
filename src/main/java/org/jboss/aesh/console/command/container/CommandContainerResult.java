package org.jboss.aesh.console.command.container;

import org.jboss.aesh.cl.result.ResultHandler;
import org.jboss.aesh.console.command.CommandResult;

public final class CommandContainerResult {

    private ResultHandler resultHandler;
    private CommandResult commandResult;

    public CommandContainerResult(ResultHandler resultHandler, CommandResult commandResult) {
        this.resultHandler = resultHandler;
        this.commandResult = commandResult;
    }

    public CommandResult getCommandResult() {
        return commandResult;
    }

    public void setCommandResult(CommandResult commandResult) {
        this.commandResult = commandResult;
    }

    public ResultHandler getResultHandler() {
        return resultHandler;
    }

    public void setResultHandler(ResultHandler resultHandler) {
        this.resultHandler = resultHandler;
    }
}
