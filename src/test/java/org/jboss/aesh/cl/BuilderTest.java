/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import junit.framework.TestCase;
import org.jboss.aesh.cl.builder.CommandBuilder;
import org.jboss.aesh.cl.builder.OptionBuilder;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.internal.OptionType;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class BuilderTest extends TestCase {

    public BuilderTest(String name) {
        super(name);
    }

    public void testBuilder() throws CommandLineParserException {
        CommandBuilder pb = new CommandBuilder();
        pb.name("foo").description("foo is bar");
        pb.addOption(
                new OptionBuilder().description("filename given").shortName('f').name("filename")
                        .hasValue(true).create());

        CommandLineParser clp = new ParserBuilder(pb.generateParameter()).generateParser();

        CommandLine cl = clp.parse("foo -f test1.txt");
        assertTrue(cl.hasOption('f'));
        assertTrue(cl.hasOption("filename"));
        assertEquals("test1.txt", cl.getOptionValue('f'));
    }

    public void testBuilder2() throws CommandLineParserException {

        CommandBuilder pb = new CommandBuilder().name("less").description("less is more");
        pb.addOption(
                new OptionBuilder().description("version").shortName('V').name("version")
                        .hasValue(false).required(true).create());
        pb.addOption(
                new OptionBuilder().description("is verbose").shortName('v').name("verbose")
                        .hasValue(false).create());

        pb.addOption(
                new OptionBuilder().description("attributes").shortName('D').name("attributes")
                        .isProperty(true).create());

        pb.addOption(
                new OptionBuilder().description("values").name("values").shortName('a')
                        .hasMultipleValues(true).create());

        pb.argument(new OptionBuilder().shortName('\u0000').name("").hasMultipleValues(true)
                .optionType(OptionType.ARGUMENT).type(String.class).create());

        CommandLineParser clp = new ParserBuilder(pb.generateParameter()).generateParser();

        CommandLine cl = clp.parse("less -V test1.txt");
        assertTrue(cl.hasOption('V'));
        assertEquals("true", cl.getOptionValue('V'));
        assertFalse(cl.hasOption('v'));
        assertEquals("test1.txt", cl.getArgument().getValues().get(0));

        cl = clp.parse("less -V -Dfoo1=bar1 -Dfoo2=bar2 test1.txt");
        assertTrue(cl.hasOption('D'));
        assertEquals("bar2", cl.getOptionProperties("D").get("foo2"));

        cl = clp.parse("less -V -Dfoo1=bar1 -Dfoo2=bar2 --values f1,f2,f3 test1.txt");
        assertTrue(cl.hasOption("values"));
        assertEquals("f2", cl.getOptionValues("values").get(1));
        assertEquals("test1.txt", cl.getArgument().getValues().get(0));
    }

    public void testBuilder3() throws CommandLineParserException {
        CommandBuilder pb = new CommandBuilder().name("less").description("less is more");
        pb.addOption(
                new OptionBuilder().description("version").name("version").hasValue(false).required(true).create());
        pb.addOption(
                new OptionBuilder().description("is verbose").name("verbose").hasValue(false).shortName('e').create());

        pb.argument(new OptionBuilder().shortName('\u0000').name("").hasMultipleValues(true)
                .optionType(OptionType.ARGUMENT).type(String.class).create());

        CommandLineParser clp = new ParserBuilder(pb.generateParameter()).generateParser();

        assertEquals("version", clp.getParameter().findOption("v").getName());
        assertEquals("verbose", clp.getParameter().findOption("e").getName());

        CommandLine cl = clp.parse("less -v -e test1.txt");
        assertTrue(cl.hasOption('v'));
        assertTrue(cl.hasOption('e'));
    }

}
