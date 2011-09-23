import org.jboss.jreadline.complete.Completion;
import org.jboss.jreadline.console.Console;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class Example {

    public static void main(String[] args) throws IOException {

        //Console console = new Console(System.in, new OutputStreamWriter(System.out));
        Console console = new Console();

        PrintWriter out = new PrintWriter(System.out);

        Completion completer = new Completion() {
            @Override
            public List<String> complete(String line, int cursor) {
                // very simple completor
                List<String> commands = new ArrayList<String>();
                if(line.equals("fo")) {
                    commands.add("foo");
                    commands.add("foobaa");
                    commands.add("foobar");
                    commands.add("foobaxxxxxx");
                    commands.add("foobbx");
                    commands.add("foobcx");
                    commands.add("foobdx");
                }
                else if(line.equals("fooba")) {
                    commands.add("foobaa");
                    commands.add("foobar");
                    commands.add("foobaxxxxxx");
                }
                else if(line.equals("foobar")) {
                    commands.add("foobar");
                }
                 return commands;
            }
        };

        console.addCompletion(completer);

        String line;
        while ((line = console.read("> ")) != null) {
            console.pushToConsole("======>\"" + line);

            if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                break;
            }
        }

    }
}
