/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import junit.framework.TestCase;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.exception.OptionParserException;
import org.jboss.aesh.console.Config;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLineFormatterTest extends TestCase {

    public CommandLineFormatterTest(String name) {
        super(name);
    }

    public void testFormatter() throws CommandLineParserException {
        ParameterBuilder pb = new ParameterBuilder().name("man").usage("[OPTION...]");

        pb.addOption(
                new OptionBuilder()
                        .name('d')
                        .longName("debug")
                        .description("emit debugging messages")
                        .create());

        pb.addOption(
                new OptionBuilder()
                        .name('D')
                        .longName("default")
                        .description("reset all options to their default values")
                        .create());

        CommandLineParser clp = new ParserBuilder(pb.generateParameter()).generateParser();

        assertEquals("Usage: man [OPTION...]"+ Config.getLineSeparator()+
                "  -d, --debug    emit debugging messages"+Config.getLineSeparator()+
                "  -D, --default  reset all options to their default values"+Config.getLineSeparator(),
                clp.printHelp());
    }

    public void testFormatter2() throws CommandLineParserException {
        ParameterBuilder pb = new ParameterBuilder().name("man").usage("[OPTION...]");

        pb.addOption(
                new OptionBuilder()
                        .name('d')
                        .longName("debug")
                        .description("emit debugging messages")
                        .create());

        pb.addOption(
                new OptionBuilder()
                        .name('D')
                        .longName("default")
                        .description("reset all options to their default values")
                        .create());

        pb.addOption(
                new OptionBuilder()
                        .name('f')
                        .longName("file")
                        .hasValue(true)
                        .argument("filename")
                        .description("set the filename")
                        .create());


        CommandLineParser clp = new ParserBuilder(pb.generateParameter()).generateParser();

        assertEquals("Usage: man [OPTION...]"+Config.getLineSeparator()+
                "  -d, --debug            emit debugging messages"+Config.getLineSeparator()+
                "  -D, --default          reset all options to their default values"+Config.getLineSeparator()+
                "  -f, --file=<filename>  set the filename"+Config.getLineSeparator(),
                clp.printHelp());
    }

}
