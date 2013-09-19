package org.jboss.aesh.console.command;

import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.cl.parser.ParserGenerator;
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

    public CommandContainer(CommandLineParser parser, Command command) {
        if(parser != null && parser.getCommand() != null) {
            this.parser = parser;
            this.command = command;
        }
    }

    public CommandContainer(CommandLineParser parser,
                            Class<? extends Command> command) {
        if(parser != null && parser.getCommand() != null) {
            this.parser = parser;
            this.command = ReflectionUtil.newInstance(command);
        }
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
