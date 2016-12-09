package org.jboss.aesh.console.aesh;

import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.readline.ReadlineConsole;
import org.jboss.aesh.tty.TestConnection;
import org.aesh.util.Config;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by joe on 4/14/15.
 */
@Ignore
public class ConsoleNestedCommandTest {

    private volatile boolean firstCallbackStarted = false;
    private volatile boolean firstCallbackFinished = false;
    private volatile boolean nestedCallbackFinished = false;

    ReadlineConsole exampleConsole;

    @Test
    public void testNestedCommand() throws IOException, InterruptedException {
        TestConnection connection = new TestConnection();

        Settings settings = new SettingsBuilder()
                .connection(connection)
                .logging(true)
                .create();

        exampleConsole = new ReadlineConsole(settings);
        //exampleConsole.setConsoleCallback(firstCallback);
        exampleConsole.start();

        connection.read("foo");
        connection.read(Config.getLineSeparator());
        //outputStream.flush();

        connection.read("bar");
        connection.read(Config.getLineSeparator());
        //outputStream.flush();

        while(!firstCallbackFinished){

        }

        Thread.sleep(80);
        exampleConsole.stop();

    }

    /*
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
    */

}
