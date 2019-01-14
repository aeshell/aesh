package examples;

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;

import java.io.IOException;

public class RuntimeExample {

    public static void main(String[] args) throws CommandLineParserException, CommandRegistryException {
      CommandRuntime runtime = AeshCommandRuntimeBuilder
              .builder()
              .commandRegistry(AeshCommandRegistryBuilder.builder().command(TestConsoleCommand.class).create())
              .build();

        StringBuilder sb = new StringBuilder("test ");
        if (args.length == 1) {
            sb.append(args[0]);
        }
        else {
            for (String arg : args) {
                if (arg.indexOf(' ') >= 0) {
                    sb.append('"').append(arg).append("\" ");
                } else {
                    sb.append(arg).append(' ');
                }
            }
        }

        try {
            runtime.executeCommand(sb.toString());
        }
        catch (CommandNotFoundException  e) {
            System.out.println("Command not found: "+sb.toString());

        }
        catch (OptionValidatorException | CommandException | CommandValidatorException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @CommandDefinition(name = "test", description = "testing")
    public static class TestConsoleCommand implements Command {

        @Option
        private String bar;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            commandInvocation.println("bar is: "+bar);

            return CommandResult.SUCCESS;
        }
    }
}
