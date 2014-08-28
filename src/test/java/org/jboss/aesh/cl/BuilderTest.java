/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ackage org.jboss.aesh.cl;

import junit.framework.TestCase;
import org.jboss.aesh.cl.internal.ProcessedCommandBuilder;
import org.jboss.aesh.cl.internal.ProcessedOptionBuilder;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.internal.OptionType;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.cl.parser.CommandLineParserBuilder;
import org.jboss.aesh.cl.result.NullResultHandler;
import org.jboss.aesh.cl.validator.NullCommandValidator;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class BuilderTest extends TestCase {

    public BuilderTest(String name) {
        super(name);
    }

    public void testBuilder() throws CommandLineParserException {
        ProcessedCommandBuilder pb = new ProcessedCommandBuilder();
        pb.name("foo").description("foo is bar");
        pb.addOption(
                new ProcessedOptionBuilder().description("filename given").shortName('f').name("filename")
                        .type(String.class).hasValue(true).create());

        CommandLineParser clp = new CommandLineParserBuilder().processedCommand(pb.create()).create();

        CommandLine cl = clp.parse("foo -f test1.txt");
        assertTrue(cl.hasOption('f'));
        assertTrue(cl.hasOption("filename"));
        assertEquals("test1.txt", cl.getOptionValue('f'));
    }

    public void testBuilder2() throws CommandLineParserException {

        ProcessedCommandBuilder pb = new ProcessedCommandBuilder().name("less").description("less is more");
        pb.addOption(
                new ProcessedOptionBuilder().description("version").shortName('V').name("version")
                        .hasValue(false).required(true).type(String.class).create());
        pb.addOption(
                new ProcessedOptionBuilder().description("is verbose").shortName('v').name("verbose")
                        .hasValue(false).type(String.class).create());

        pb.addOption(
                new ProcessedOptionBuilder().description("attributes").shortName('D').name("attributes")
                        .isProperty(true).type(String.class).create());

        pb.addOption(
                new ProcessedOptionBuilder().description("values").name("values").shortName('a')
                        .hasMultipleValues(true).type(String.class).create());

        pb.argument(new ProcessedOptionBuilder().shortName('\u0000').name("").hasMultipleValues(true)
                .optionType(OptionType.ARGUMENT).type(String.class).create());

        CommandLineParser clp = new CommandLineParserBuilder().processedCommand(pb.create()).create();

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
        ProcessedCommandBuilder pb = new ProcessedCommandBuilder().name("less").description("less is more");
        pb.addOption(
                new ProcessedOptionBuilder()
                        .description("version")
                        .name("version")
                        .shortName('v')
                        .hasValue(false)
                        .required(true)
                        .type(String.class)
                        .create());
        pb.addOption(
                new ProcessedOptionBuilder()
                        .description("is verbose")
                        .name("verbose")
                        .hasValue(false)
                        .shortName('e')
                        .type(String.class)
                        .create());

        pb.argument(new ProcessedOptionBuilder().shortName('\u0000').name("").hasMultipleValues(true)
                .optionType(OptionType.ARGUMENT).type(String.class).create());

        CommandLineParser clp = new CommandLineParserBuilder().processedCommand(pb.create()).create();

        assertEquals("version", clp.getProcessedCommand().findOption("v").getName());
        assertEquals("verbose", clp.getProcessedCommand().findOption("e").getName());

        CommandLine cl = clp.parse("less -v -e test1.txt");
        assertTrue(cl.hasOption('v'));
        assertTrue(cl.hasOption('e'));
    }

    public void testParameterInt() throws CommandLineParserException {
        ProcessedCommand processedCommand =
                new ProcessedCommandBuilder()
                        .name("foo")
                        .description("")
                        .validator(NullCommandValidator.class)
                        .resultHandler(NullResultHandler.class)
                        .create();
        processedCommand.addOption(new ProcessedOptionBuilder().name("foo1").shortName('f').type(String.class).create());
        processedCommand.addOption(new ProcessedOptionBuilder().name("foo2").shortName('o').type(String.class).create());
        processedCommand.addOption(new ProcessedOptionBuilder().name("foo3").shortName('3').type(String.class).create());

        assertEquals("f", processedCommand.getOptions().get(0).getShortName());
        assertEquals("o", processedCommand.getOptions().get(1).getShortName());
        assertEquals("3", processedCommand.getOptions().get(2).getShortName());
    }

}
