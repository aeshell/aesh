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
        ParserBuilder pb = ParserBuilder.init().name("man").usage("[OPTION...]");

        pb.addOption(
                OptionBuilder.init()
                        .name('d')
                        .longName("debug")
                        .description("emit debugging messages")
                        .create());

        pb.addOption(
                OptionBuilder.init()
                        .name('D')
                        .longName("default")
                        .description("reset all options to their default values")
                        .create());

        CommandLineParser clp = pb.generateParser();

        assertEquals("Usage: man [OPTION...]\n"+
                "-d, --debug emit debugging messages\n"+
        "-D, --default reset all options to their default values\n",
                clp.printHelp());
    }
}
