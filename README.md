Æsh (Another Extendable SHell)
=========

[![Build Status](https://travis-ci.org/aeshell/aesh.png?branch=master)](https://travis-ci.org/aeshell/aesh)

Æsh is a Java library for handling console input with the goal to support most GNU Readline features.

Features:
---------
* Line editing
* History (search, persistence)
* Completion
* Masking
* Undo and Redo
* Paste buffer
* Emacs and Vi editing mode
* Supports POSIX OS's and Windows
* Easy to configure (history file & buffer size, edit mode, streams, possible to override terminal implementations, etc)
* Support standard out and standard error
* Redirect
* Alias
* Pipeline

How to build:
-------------
* Æsh uses Gradle (http://gradle.org) as its build tool.

To get going:
-------------
```java
import java.io.IOException;

import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleCallback;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.settings.SettingsBuilder;

public class Example {

    public static void main(String[] args) throws IOException {

        final Console console = new Console(new SettingsBuilder().create());
        console.setPrompt(new Prompt("[aesh]$ "));

        console.setConsoleCallback(new ConsoleCallback() {
            @Override
            public int execute(ConsoleOperation output) {
                console.getShell().out().println("======>\"" + output.getBuffer());
                if (output.getBuffer().equals("quit")) {
                    try {
                        console.stop();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return 0;
            }
        });
        console.start();
    }
}
```

Keys that are mapped by default in Æsh
--------------------------------------------
Note: C equals Control and M is Meta/Alt

EMACS Mode
----------
* Move back one char : C-b or left arrow
* Move forward one char : C-f or right arrow
* Delete the character left of cursor : backspace
* Delete the character on cursor : C-d
* Undo : C-_ or C-x C-u
* Move to the start of the line : C-a or home
* Move to the end of the line : C-e or end
* Move forward a word, where a word is composed of letters and digits : M-f
* Move backward a word : M-b
* Previous line : up arrow
* Next line : down arrow
* Clear the screen, reprinting the current line at the top : C-l
* Delete next word to the right of cursor : M-d
* Complete : tab
* Kill the text from the current cursor position to the end of the line : C-k
* Kill from the cursor to the end of the current word, or, if between words, to the end of the next word : M-d
* Kill from the cursor to the previous whitespace : C-w
* Yank the most recently killed text back into the buffer at the cursor : C-y
* Search backward in the history for a particular string : C-r
* Search forward in the history for a particular string : C-s
* Switch to VI editing mode: M-C-j


VI Mode
----------
In command mode: About every vi command is supported, here's a few:

* Move back one char : h
* Move forward one char : l
* Delete the character left of cursor : X
* Delete the character on cursor : x
* Undo : u
* Move to the start of the line : 0
* Move to the end of the line : $
* Move forward a word, where a word is composed of letters and digits : w
* Move backward a word : b
* Previous line : k
* Next line : n
* Clear the screen, reprinting the current line at the top : C-l
* Delete next word to the right of cursor : dw 
* Kill the text from the current cursor position to the end of the line : D and d$
* Kill from the cursor to the end of the current word, or, if between words, to the end of the next word : db
* Kill from the cursor to the previous whitespace : dB
* Yank the most recently killed text back into the buffer at the cursor : p (after cursor), P (before cursor)
* Add text into yank buffer : y + movement action
* Enable change mode : c
* Repeat previous action : .
* +++ (read a vi manual)

In edit mode:

* Search backward in the history for a particular string : C-r
* Search forward in the history for a particular string : C-s
* Delete the character left of cursor : backspace

Supported runtime properties:
-------------
* aesh.terminal : specify Terminal object
* aesh.editmode : specify either VI or EMACS edit mode
* aesh.readinputrc : specify if Æsh should read settings from inputrc
* aesh.inputrc : specify the inputrc file (must exist)
* aesh.historyfile : specify the history file (must exist)
* aesh.historypersistent : specify if Æsh should persist history file on exit
* aesh.historydisabled : specify if history should be disabled
* aesh.historysize : specify the maximum size of the history file
* aesh.logging : specify if logging should be enabled
* aesh.logfile : specify the log file
* aesh.disablecompletion : specify if completion should be disabled
