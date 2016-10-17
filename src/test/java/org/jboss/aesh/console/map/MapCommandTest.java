/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.aesh.console.map;

import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.List;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.cl.parser.AeshCommandLineParser;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.AeshConsoleImpl;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.container.AeshCommandContainer;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.MutableCommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.edit.KeyOperation;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author jdenise@redhat.com
 */
public class MapCommandTest {

    private final KeyOperation completeChar = new KeyOperation(Key.CTRL_I, Operation.COMPLETE);
    private final KeyOperation backSpace = new KeyOperation(Key.BACKSPACE, Operation.DELETE_PREV_CHAR);

    /**
     * Test that during completion and execution. Option values are not lost.
     *
     * @throws Exception
     */
    @Test
    public void testMapCommand() throws Exception {

        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .inputStream(pipedInputStream)
                .outputStream(new PrintStream(byteArrayOutputStream))
                .setPersistExport(false)
                .logging(true)
                .create();

        MutableCommandRegistry registry = (MutableCommandRegistry) new AeshCommandRegistryBuilder().create();
        registry.addCommand(new AeshCommandContainer(
                new AeshCommandLineParser<>(
                        new MyMapCommand().getProcessedCommand())));
        AeshConsoleBuilder consoleBuilder = new AeshConsoleBuilder()
                .settings(settings)
                .commandRegistry(registry)
                .prompt(new Prompt(""));

        AeshConsole aeshConsole = consoleBuilder.create();
        aeshConsole.start();
        try {
            outputStream.write("dyn-".getBytes());
            outputStream.write(completeChar.getFirstValue());
            outputStream.flush();
            Thread.sleep(100);
            assertEquals("dyn-cmd ", ((AeshConsoleImpl) aeshConsole).getBuffer());

            outputStream.write("--opt1 o1 ".getBytes());
            outputStream.write(completeChar.getFirstValue());
            outputStream.flush();
            Thread.sleep(100);
            assertEquals(byteArrayOutputStream.toString(),
                    "dyn-cmd --opt1 o1 --opt2 ", ((AeshConsoleImpl) aeshConsole).getBuffer());

            {
                // Check that value is o1.
                List<ProcessedOption> opts
                        = registry.getCommand("dyn-cmd", null).getParser().getProcessedCommand().getOptions();
                boolean found = false;
                for (ProcessedOption opt : opts) {
                    if (opt.getName().equals("opt1")) {
                        assertEquals(opt.getValue(), "o1");
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new Exception("Option not found");
                }
            }

            outputStream.write("o2 ".getBytes());
            outputStream.write(completeChar.getFirstValue());
            outputStream.flush();
            Thread.sleep(100);
            assertEquals(byteArrayOutputStream.toString(),
                    "dyn-cmd --opt1 o1 --opt2 o2 ", ((AeshConsoleImpl) aeshConsole).getBuffer());

            {
                // Check that value is o1.
                List<ProcessedOption> opts
                        = registry.getCommand("dyn-cmd", null).getParser().getProcessedCommand().getOptions();
                // Check that value is o1.
                int found = 0;
                for (ProcessedOption opt : opts) {
                    if (opt.getName().equals("opt1")) {
                        assertEquals(opt.getValue(), "o1");
                        found += 1;
                    }
                    if (opt.getName().equals("opt2")) {
                        assertEquals(opt.getValue(), "o2");
                        found += 1;
                    }
                }
                if (found != 2) {
                    throw new Exception("Options not found");
                }
            }

            outputStream.write(completeChar.getFirstValue());
            outputStream.flush();
            Thread.sleep(100);
            assertEquals(byteArrayOutputStream.toString(),
                    "dyn-cmd --opt1 o1 --opt2 o2 ", ((AeshConsoleImpl) aeshConsole).getBuffer());

            {
                // Check that value is o1.
                List<ProcessedOption> opts
                        = registry.getCommand("dyn-cmd", null).getParser().getProcessedCommand().getOptions();
                // Check that value is o1.
                int found = 0;
                for (ProcessedOption opt : opts) {
                    if (opt.getName().equals("opt1")) {
                        assertEquals(opt.getValue(), "o1");
                        found += 1;
                    }
                    if (opt.getName().equals("opt2")) {
                        assertEquals(opt.getValue(), "o2");
                        found += 1;
                    }
                }
                if (found != 2) {
                    throw new Exception("Options not found");
                }
            }

            outputStream.write(Config.getLineSeparator().getBytes());
            outputStream.flush();
            Thread.sleep(100);
            assertTrue(byteArrayOutputStream.toString().contains("opt1=o1,opt2=o2"));
        } finally {
            aeshConsole.stop();
        }
    }
}
