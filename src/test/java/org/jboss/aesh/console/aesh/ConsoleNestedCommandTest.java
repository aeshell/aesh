package org.jboss.aesh.console.aesh;

import org.jboss.aesh.console.AeshConsoleCallback;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleCallback;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by joe on 4/14/15.
 */
public class ConsoleNestedCommandTest {

    private volatile boolean firstCallbackStarted = false;
    private volatile boolean firstCallbackFinished = false;
    private volatile boolean nestedCallbackFinished = false;

    Console exampleConsole;

    @Test
    public void testNestedCommand() throws IOException, InterruptedException {
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .inputStream(pipedInputStream)
                .outputStream(new PrintStream(byteArrayOutputStream))
                .logging(true)
                .create();

        exampleConsole = new Console(settings);
        exampleConsole.setConsoleCallback(firstCallback);
        exampleConsole.start();

        outputStream.write("foo".getBytes());
        outputStream.write(Config.getLineSeparator().getBytes());
        outputStream.flush();

        outputStream.write("bar".getBytes());
        outputStream.write(Config.getLineSeparator().getBytes());
        outputStream.flush();

        while(!firstCallbackFinished){

        }

        Thread.sleep(80);
        exampleConsole.stop();

    }

    ConsoleCallback firstCallback = new AeshConsoleCallback() {

        @Override
        public int execute(ConsoleOperation output) throws InterruptedException {
            try {
                firstCallbackStarted = true;
                exampleConsole.setConsoleCallback(nestedCallback);

                exampleConsole.putProcessInBackground(output.getPid());

                while (!nestedCallbackFinished) {

                }

                exampleConsole.putProcessInForeground(output.getPid());
                return 0;
            }finally {
                firstCallbackFinished = true;
            }
        }
    };

    ConsoleCallback nestedCallback = new AeshConsoleCallback() {
        private boolean hasUsername = false;

        @Override
        public int execute(ConsoleOperation output) throws InterruptedException {
            if(firstCallbackStarted && !firstCallbackFinished){
                assertTrue(true);
            }else{
                assertFalse("Not executed inside first callback.", false);
            }

            exampleConsole.setConsoleCallback(firstCallback);
            nestedCallbackFinished = true;
            return 0;
        }
    };

}
