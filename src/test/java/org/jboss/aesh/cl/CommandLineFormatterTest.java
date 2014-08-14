/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import junit.framework.TestCase;
import org.jboss.aesh.cl.internal.ProcessedCommandBuilder;
import org.jboss.aesh.cl.internal.ProcessedOptionBuilder;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.cl.parser.CommandLineParserBuilder;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.util.ANSI;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLineFormatterTest extends TestCase {

    public CommandLineFormatterTest(String name) {
        super(name);
    }

    public void testFormatter() throws CommandLineParserException {
        ProcessedCommandBuilder pb = new ProcessedCommandBuilder().name("man").description("[OPTION...]");

        pb.addOption(
                new ProcessedOptionBuilder()
                        .shortName('d')
                        .name("debug")
                        .description("emit debugging messages")
                        .type(String.class)
                        .create());

        pb.addOption(
                new ProcessedOptionBuilder()
                        .shortName('D')
                        .name("default")
                        .description("reset all options to their default values")
                        .type(String.class)
                        .create());

        CommandLineParser clp = new CommandLineParserBuilder()
                .processedCommand(pb.create())
                .create();

        assertEquals("Usage: man [OPTION...]"+ Config.getLineSeparator()+
                "  -d, --debug    emit debugging messages"+Config.getLineSeparator()+
                "  -D, --default  reset all options to their default values"+Config.getLineSeparator(),
                clp.printHelp());
    }

    public void testFormatter2() throws CommandLineParserException {
        ProcessedCommandBuilder pb = new ProcessedCommandBuilder().name("man").description("[OPTION...]");

        pb.addOption(
                new ProcessedOptionBuilder()
                        .shortName('d')
                        .name("debug")
                        .description("emit debugging messages")
                        .type(String.class)
                        .create());

        pb.addOption(
                new ProcessedOptionBuilder()
                        .shortName('D')
                        .name("default")
                        .required(true)
                        .description("reset all options to their default values")
                        .type(String.class)
                        .create()
        );

        pb.addOption(
                new ProcessedOptionBuilder()
                        .shortName('f')
                        .name("file")
                        .hasValue(true)
                        .argument("filename")
                        .description("set the filename")
                        .type(String.class)
                        .create());


        CommandLineParser clp = new CommandLineParserBuilder().processedCommand(pb.create()).create();

        assertEquals("Usage: man [OPTION...]"+
                        Config.getLineSeparator()+
                        "  -d, --debug            emit debugging messages"+Config.getLineSeparator()+
                        ANSI.getBold()+
                        "  -D, --default"+
                        ANSI.getBoldOff()+
                        "          reset all options to their default values"+Config.getLineSeparator()+
                        "  -f, --file=<filename>  set the filename"+Config.getLineSeparator(),
                clp.printHelp());
    }

}
