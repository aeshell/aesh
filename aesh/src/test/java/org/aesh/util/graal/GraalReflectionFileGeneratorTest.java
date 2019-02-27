package org.aesh.util.graal;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.container.CommandContainerBuilder;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.io.Resource;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.aesh.utils.Config.getLineSeparator;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class GraalReflectionFileGeneratorTest {

    @Test
    public void testSimpleCommand() throws IOException {

        GraalReflectionFileGenerator generator = new GraalReflectionFileGenerator();
        CommandLineParser parser = getParser(TestCommand1.class);

        String out = generator.generateReflection(parser);
        assertEquals(readFile("src/test/resources/graal1"), out);
    }

    @Test
    public void testCommand() throws IOException {

        GraalReflectionFileGenerator generator = new GraalReflectionFileGenerator();
        CommandLineParser parser = getParser(TestCommand2.class);

        String out = generator.generateReflection(parser);
        assertEquals(readFile("src/test/resources/graal2"), out);
    }

    @Test
    public void testCommandWithFileOption() throws IOException {

        GraalReflectionFileGenerator generator = new GraalReflectionFileGenerator();
        CommandLineParser parser = getParser(TestCommand3.class);

        String out = generator.generateReflection(parser);
        assertEquals(readFile("src/test/resources/graal3"), out);
    }


    private String readFile(String path) throws IOException {
        StringBuilder builder = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(path));
        br.lines().forEach(line -> {
            if(builder.length() > 0)
                builder.append(getLineSeparator());
            builder.append(line);
        });
        return builder.toString();
    }

    private CommandLineParser getParser(Class clazz) {
        //Class<Command<CommandInvocation>> clazz = loadCommand(command);
        if (clazz != null) {
            CommandContainerBuilder<CommandInvocation> builder = new AeshCommandContainerBuilder<>();
            try {
                CommandContainer container = builder.create(clazz);
                return container.getParser();
            } catch (CommandLineParserException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @CommandDefinition(name = "test1", description = "")
    public static class TestCommand1 implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "test2", description = "")
    public static class TestCommand2 implements Command {

        @Option(shortName = 'h', hasValue = false)
        private boolean help;

        @Option(shortName = 'o')
        private boolean override;

        @Option(shortName = 't', defaultValue = {"FOO","BAR"})
        private String test;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "test3", description = "")
    public static class TestCommand3 implements Command {

        @Option(shortName = 'h', hasValue = false)
        private boolean help;

        @Option(shortName = 'o')
        private boolean override;

        @Option(shortName = 't')
        private Resource test;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }


}
