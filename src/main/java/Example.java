import org.jboss.jreadline.complete.CompleteOperation;
import org.jboss.jreadline.complete.Completion;
import org.jboss.jreadline.console.Console;
import org.jboss.jreadline.console.settings.Settings;

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
        Console console = new Console();

        PrintWriter out = new PrintWriter(System.out);

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
                else if(co.getBuffer().equals("h")) {
                    commands.add("history");
                    commands.add("help");
                }
                 co.setCompletionCandidates(commands);
            }
        };

        console.addCompletion(completer);

        String line;
        //console.pushToConsole(ANSIColors.GREEN_TEXT());
        while ((line = console.read("> ")) != null) {
            console.pushToConsole("======>\"" + line+"\"\n");

            if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit") ||
                    line.equalsIgnoreCase("reset")) {
                break;
            }
            if(line.equalsIgnoreCase("password")) {
                line = console.read("password: ", Character.valueOf((char) 0));
                console.pushToConsole("password typed:"+line+"\n");

            }
        }
        if(line.equals("reset")) {
            console.stop();
            console = new Console();

            while ((line = console.read("> ")) != null) {
                console.pushToConsole("======>\"" + line+"\"\n");
                if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit") ||
                        line.equalsIgnoreCase("reset")) {
                    break;
                }

            }
        }

        try {
            console.stop();
        } catch (Exception e) {
        }
    }

}
