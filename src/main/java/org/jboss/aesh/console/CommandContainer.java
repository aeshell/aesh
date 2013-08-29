package org.jboss.aesh.console;

import org.jboss.aesh.cl.CommandLineParser;
import org.jboss.aesh.cl.ParserGenerator;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.util.ReflectionUtil;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandContainer {

    private Command command;
    private CommandLineParser parser;
    private String errorMessage;

    public CommandContainer(Command command) {
        addCommand(command);
    }

    public CommandContainer(Class<? extends Command> command) {
        addCommand(command);
    }

    private void addCommand(Class<? extends Command> command) {
        try {
            parser = ParserGenerator.generateCommandLineParser(command);
            this.command = ReflectionUtil.newInstance(command);
        }
        catch (CommandLineParserException e) {
            errorMessage = e.getMessage();
        }
    }

    private void addCommand(Command command) {
        try {
            parser = ParserGenerator.generateCommandLineParser(command);
            this.command = command;
        }
        catch (CommandLineParserException e) {
            errorMessage = e.getMessage();
        }
    }

    public Command getCommand() {
        return command;
    }

    public CommandLineParser getParser() {
        return parser;
    }

    public boolean hasError() {
        return errorMessage != null;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
