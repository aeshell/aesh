JReadline
=========

JReadline is a Java library for handling console input. JReadline is a fork of jline with the goal of support most GNU Readline features.

Features:
---------
* Line editing
* History
* History search
* Completion
* Support for both Emacs and Vi editing mode

How to build:
-------------
* JReadline uses Gradle (http://gradle.org) as its build tool.*

To get going:
-------------

public class Example {

    public static void main(String[] args) throws java.io.IOException {

        org.jboss.jreadline.console.Console console = new org.jboss.jreadline.console.Console();

        String line;
        while ((line = console.read("> ")) != null) {
            console.pushToConsole("======>\"" + line);

            if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                break;
            }
        }

    }
}


