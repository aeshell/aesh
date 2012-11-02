/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLineFormatterTest extends TestCase {

    public CommandLineFormatterTest(String name) {
        super(name);
    }

    public void testFormatter() {
        ParserBuilder pb = new ParserBuilder().name("man").usage("[OPTION...]");

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

        CommandLineParser clp = pb.generateParser();

        assertEquals("Usage: man [OPTION...]\n"+
                "  -d, --debug    emit debugging messages\n"+
                "  -D, --default  reset all options to their default values\n",
                clp.printHelp());
    }

    public void testFormatter2() {
        ParserBuilder pb = new ParserBuilder().name("man").usage("[OPTION...]");

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


        CommandLineParser clp = pb.generateParser();

        assertEquals("Usage: man [OPTION...]\n"+
                "  -d, --debug            emit debugging messages\n"+
                "  -D, --default          reset all options to their default values\n"+
                "  -f, --file=<filename>  set the filename\n",
                clp.printHelp());
    }

}
