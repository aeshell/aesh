package org.jboss.aesh.console;

import org.jboss.aesh.cl.CommandDefinition;
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

    public CommandContainer(Command command) {
        addCommand(command);
    }

    public CommandContainer(Class<? extends Command> command) {
        addCommand(command);
    }

    private void addCommand(Class<? extends Command> command) {
        if(verifyCommand(command)) {
            try {
                parser = ParserGenerator.generateCommandLineParser(command);
                this.command = ReflectionUtil.newInstance(command);
            } catch (CommandLineParserException e) {
                e.printStackTrace();
            }
        }
    }

    private void addCommand(Command command) {
        if(verifyCommand(command)) {
            try {
                parser = ParserGenerator.generateCommandLineParser(command);
                this.command = command;
            } catch (CommandLineParserException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean verifyCommand(Class<? extends Command> command) {
        return command.getAnnotation(CommandDefinition.class) != null;
    }

    private boolean verifyCommand(Command command) {
        return command.getClass().getAnnotation(CommandDefinition.class) != null;
    }

    public Command getCommand() {
        return command;
    }

    public CommandLineParser getParser() {
        return parser;
    }
}
