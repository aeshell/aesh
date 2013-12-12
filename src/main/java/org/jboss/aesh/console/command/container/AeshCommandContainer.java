package org.jboss.aesh.console.command.container;

import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.parser.AeshCommandLineParser;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.cl.parser.ParserGenerator;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.util.ReflectionUtil;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandContainer implements CommandContainer {

    private Command command;
    private CommandLineParser parser;
    private String errorMessage;

    public AeshCommandContainer(Command command) {
        addCommand(command);
    }

    public AeshCommandContainer(Class<? extends Command> command) {
        addCommand(command);
    }

    public AeshCommandContainer(CommandLineParser parser, Command command) {
        if (parser != null && parser.getCommand() != null) {
            this.parser = parser;
            this.command = command;
        }
    }

    public AeshCommandContainer(CommandLineParser parser,
                                Class<? extends Command> command) {
        if (parser != null && parser.getCommand() != null) {
            this.parser = parser;
            this.command = ReflectionUtil.newInstance(command);
        }
    }

    public AeshCommandContainer(ProcessedCommand processedCommand,
                                Class<? extends Command> command) {
        parser = new AeshCommandLineParser(processedCommand);
        this.command = ReflectionUtil.newInstance(command);
    }

    public AeshCommandContainer(ProcessedCommand processedCommand,
                                Command command) {
        parser = new AeshCommandLineParser(processedCommand);
        this.command = command;
    }

    private void addCommand(Class<? extends Command> command) {
        try {
            parser = ParserGenerator.generateCommandLineParser(command);
            this.command = ReflectionUtil.newInstance(command);
        } catch (CommandLineParserException e) {
            errorMessage = e.getMessage();
        }
    }

    private void addCommand(Command command) {
        try {
            parser = ParserGenerator.generateCommandLineParser(command);
            this.command = command;
        } catch (CommandLineParserException e) {
            errorMessage = e.getMessage();
        }
    }

    @Override
    public Command getCommand() {
        return command;
    }

    @Override
    public CommandLineParser getParser() {
        return parser;
    }

    @Override
    public boolean haveBuildError() {
        return errorMessage != null;
    }

    @Override
    public String getBuildErrorMessage() {
        return errorMessage;
    }

    @Override
    public void close() {

    }
}
