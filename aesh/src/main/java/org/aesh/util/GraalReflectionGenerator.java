package org.aesh.util;

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.CommandRuntime;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.util.graal.GraalReflectionCommand;

import java.io.IOException;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class GraalReflectionGenerator {

    public static void main(String[] args) throws CommandRegistryException {
               CommandRuntime runtime = AeshCommandRuntimeBuilder
                                         .builder()
                                         .commandRegistry(AeshCommandRegistryBuilder.builder().command(GraalReflectionCommand.class).create())
                                         .build();

        if (args.length > 0) {
            StringBuilder sb = new StringBuilder("graalreflection ");
            if (args.length == 1) {
                sb.append(args[0]);
            } else {
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
            catch (CommandNotFoundException e) {
                System.err.println("Command not found: " + sb.toString());
            }
            catch (CommandException | CommandLineParserException | CommandValidatorException | OptionValidatorException | InterruptedException | IOException e) {
                System.err.println(e.getMessage());
            }
        }
        else {
            System.err.println(runtime.commandInfo("completer"));
        }
    }
}
