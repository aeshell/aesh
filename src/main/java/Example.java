import org.jboss.jreadline.console.ConsoleCommand;
import org.jboss.jreadline.complete.CompleteOperation;
import org.jboss.jreadline.complete.Completion;
import org.jboss.jreadline.console.Console;
import org.jboss.jreadline.console.settings.Settings;
import org.jboss.jreadline.edit.actions.Operation;
import org.jboss.jreadline.util.ANSI;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class Example {

    public static void main(String[] args) throws IOException {

        //Settings.getInstance().setAnsiConsole(false);
        Settings.getInstance().setReadInputrc(false);
       //Settings.getInstance().setHistoryDisabled(true);
        //Settings.getInstance().setHistoryPersistent(false);
        Console exampleConsole = new Console();

        PrintWriter out = new PrintWriter(System.out);

        ConsoleCommand test = new ConsoleCommand(exampleConsole) {

            @Override
            protected void afterAttach() throws IOException {
				console.pushToConsole(ANSI.getAlternateBufferScreen());
				//console.pushToConsole("blablablablablabal");
                readFromFile();
            }

            @Override
            protected void afterDetach() throws IOException {
				console.pushToConsole(ANSI.getMainBufferScreen());
            }

            private void readFromFile() throws IOException {
                //console.clear();
                console.pushToConsole("here should we present some text... press 'q' to quit");
                /*
                Path file = FileSystems.getDefault().getPath("/tmp", "README.md");
                Charset charset = Charset.defaultCharset();
                BufferedReader reader = Files.newBufferedReader(file, charset);
                String line = null;
                while ((line = reader.readLine()) != null) {
                    console.pushToConsole(line);
                    console.pushToConsole(Config.getLineSeparator());
                }
                */
            }

            @Override
            public String processOperation(Operation operation) throws IOException {
                //console.pushToConsole("blablablablablabal");
                if(operation.getInput()[0] == 'q') {
					detach();
					return "";
				}
                else if(operation.getInput()[0] == 'a') {
                    readFromFile();
                    return null;
                }
				else
					return null;
            }
        };

        Completion completer = new Completion() {
            @Override
            public void complete(CompleteOperation co) {
                // very simple completor
                List<String> commands = new ArrayList<String>();
                if(co.getBuffer().equals("fo") || co.getBuffer().equals("foo")) {
                    commands.add("foo");
                    commands.add("foobaa");
                    commands.add("foobar");
                    commands.add("foobaxxxxxx");
                    commands.add("foobbx");
                    commands.add("foobcx");
                    commands.add("foobdx");
                }
                else if(co.getBuffer().equals("fooba")) {
                    commands.add("foobaa");
                    commands.add("foobar");
                    commands.add("foobaxxxxxx");
                }
                else if(co.getBuffer().equals("foobar")) {
                    commands.add("foobar");
                }
                else if(co.getBuffer().equals("bar")) {
                    commands.add("bar/");
                }
                else if(co.getBuffer().equals("h")) {
                    commands.add("help.history");
                    commands.add("help");
                }
                else if(co.getBuffer().equals("help")) {
                    commands.add("help.history");
                    commands.add("help");
                }
                else if(co.getBuffer().equals("help.")) {
                    commands.add("help.history");
                }
                else if(co.getBuffer().equals("deploy")) {
                    commands.add("deploy /home/blabla/foo/bar/alkdfe/en/to/tre");
                }
                 co.setCompletionCandidates(commands);
            }
        };

        exampleConsole.addCompletion(completer);

        String line;
        //console.pushToConsole(ANSI.GREEN_TEXT());
        while ((line = exampleConsole.read("[test@foo.bar]~> ")) != null) {
            exampleConsole.pushToConsole("======>\"" + line + "\"\n");

            if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit") ||
                    line.equalsIgnoreCase("reset")) {
                break;
            }
            if(line.equalsIgnoreCase("password")) {
                line = exampleConsole.read("password: ", Character.valueOf((char) 0));
                exampleConsole.pushToConsole("password typed:" + line + "\n");

            }
            if(line.equals("clear"))
                exampleConsole.clear();
            if(line.equals("man")) {
                //exampleConsole.attachProcess(test);
                test.attach();
            }
        }
        if(line.equals("reset")) {
            exampleConsole.stop();
            exampleConsole = new Console();

            while ((line = exampleConsole.read("> ")) != null) {
                exampleConsole.pushToConsole("======>\"" + line + "\"\n");
                if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit") ||
                        line.equalsIgnoreCase("reset")) {
                    break;
                }

            }
        }

        try {
            exampleConsole.stop();
        } catch (Exception e) {
        }
    }

}
