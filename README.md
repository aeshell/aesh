JReadline
=========

JReadline is a Java library for handling console input. JReadline is a fork of jline with the goal to support most GNU Readline features.

Features:
---------
* Line editing
* History (search, persistence)
* Completion
* Emacs and Vi editing mode
* Supports POSIX OS's and Windows
* Easy to configure (history file & buffer size, edit mode, streams, possible to override terminal impls, etc)

How to build:
-------------
* JReadline uses Gradle (http://gradle.org) as its build tool.

To get going:
-------------

public class Example {

    public static void main(String[] args) throws java.io.IOException {

        org.jboss.jreadline.console.Console console = new org.jboss.jreadline.console.Console();

        String line;
        while ((line = console.read("> ")) != null) {
            console.pushToConsole("======>\"" +line+ "\n");

            if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                break;
            }
        }

    }
}


