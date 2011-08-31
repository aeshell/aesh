JReadline
=========

JReadline is a fork of jline with the goal of support most GNU Readline features.

To get going:

import org.jboss.jreadline.console.Console;

import java.io.IOException;

public class Example {

    public static void main(String[] args) throws IOException {

        Console console = new Console();

        String line;
        while ((line = console.read("> ")) != null) {
            System.out.println("======>\"" + line + "\"");
            System.out.flush();

            if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                break;
            }
        }

    }
}


