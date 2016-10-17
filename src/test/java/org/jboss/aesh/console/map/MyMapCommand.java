/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.aesh.console.map;

import java.util.ArrayList;
import java.util.List;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.cl.internal.ProcessedOptionBuilder;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.parser.OptionParserException;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.map.MapCommand;
import org.jboss.aesh.console.command.map.MapProcessedCommandBuilder;

/**
 *
 * @author jdenise@redhat.com
 */
public class MyMapCommand extends MapCommand<CommandInvocation> {

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        String v1 = (String) getValue("opt1");
        String v2 = (String) getValue("opt2");
        commandInvocation.println("opt1=" + v1 + "," + "opt2=" + v2);
        return CommandResult.SUCCESS;
    }

    public ProcessedCommand getProcessedCommand() throws OptionParserException, CommandLineParserException {
        return new MapProcessedCommandBuilder().
                name("dyn-cmd").
                command(this).optionProvider(new MapProcessedCommandBuilder.ProcessedOptionProvider() {
            @Override
            public List<ProcessedOption> getOptions() {
                try {
                    List<ProcessedOption> options = new ArrayList<>();
                    ProcessedOption opt1 = new ProcessedOptionBuilder().name("opt1").type(String.class).hasValue(true).create();
                    ProcessedOption opt2 = new ProcessedOptionBuilder().name("opt2").type(String.class).hasValue(true).create();
                    options.add(opt1);
                    options.add(opt2);
                    return options;
                } catch (OptionParserException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }).create();
    }
}
