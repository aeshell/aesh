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
                if(line.length() < 1 || line.startsWith("f") || line.startsWith("fo"))
                    commands.add("foo");

                return commands;
            }
        };

        console.addCompletion(completer);

        String line;
        while ((line = console.read("> ")) != null) {
            console.pushToConsole("======>\"" + line + "\n");

            if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                break;
            }
        }

    }
}
