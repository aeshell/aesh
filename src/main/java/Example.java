import org.jboss.jreadline.console.Console;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class Example {

    public static void main(String[] args) throws IOException {

        //Console console = new Console(System.in, new OutputStreamWriter(System.out));
        Console console = new Console();

        PrintWriter out = new PrintWriter(System.out);

        String line;
        while ((line = console.read("> ")) != null) {
            console.pushToConsole("======>\"" + line + "\n");

            if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                break;
            }
        }

    }
}
