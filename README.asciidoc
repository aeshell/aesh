== Æsh (Another Extendable SHell)


image:https://travis-ci.org/aeshell/aesh.svg?branch=master["Build Status", link="https://travis-ci.org/aeshell/aesh"]

Æsh is a Java library to easily create commands through a well defined API. Æsh will take care of all the parsing and injection for your commands. Æsh uses the project 'aesh-readline' for it's terminal/readline integration.

IMPORTANT:
---------

We're now heading into the final stages before we'll tag an 1.0-alpha release. 
The master branch is fairly stable atm and the plan is to not change anything big in the upcoming weeks.
We have released a snapshot version of 1.0 if you are eager to test. Just add this to your 'pom.xml':

[source,xml]
----
<dependency>
  <groupId>org.aesh</groupId>
  <artifactId>aesh</artifactId>
  <version>1.7</version>
</dependency>
----

'build.gradle' file.
[source]
----
dependencies {
    compile group: 'org.aesh', name: 'aesh', version: '1.0-SNAPSHOT'
}
----

Features:
---------

Æsh is a library to easily create commands. We recommend using annotations as the default way of
adding metadata for your commands, but we also have a builder API if that is preferred.
Some of our features:

- *Easy to use API to create everything from simple to advanced commands*
- *Supports different types of options (list, group, single) and arguments*
- *Builtin completors for default values, booleans and files*
- *Supports multiple hierarcy of sub commands eg: git rebase/pull/++*
- *All option values and arguments are automatically injected during execution*
- *Possible to add custom validators, activators, completors, converters, 
  renderers and parsers*
- *Automatically generates help/info text based on the metadata provided*
- *Can add and remove commands during runtime*
- *++++*

All the readline functionality included in 'aesh-readline', eg:

- *Line editing*
- *History (search, persistence)*
- *Completion*
- *Masking*
- *Undo and Redo*
- *Paste buffer*
- *Emacs and Vi editing mode*
- *Supports POSIX OS's and Windows*
- *Easy to configure* (history file & buffer size, edit mode, streams, possible to override terminal implementations, etc)
- *Support standard out and standard error*
- *Redirect*
- *Alias*
- *Pipeline*

How to build:
-------------
- Æsh uses Maven (http://maven.apache.org) as its build tool.

Æsh Developer List:
-------------------
https://groups.google.com/forum/#!forum/aesh-dev

To get going:
-------------
[source,java]
----
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.console.settings.Settings;
import org.aesh.console.settings.SettingsBuilder;
import org.aesh.readline.ReadlineConsole;

import java.io.IOException;

public class SimpleExample {
    public static void main(String[] args) throws CommandLineParserException, IOException {
        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(ExitCommand.class)
                .create();
        Settings settings = SettingsBuilder
                .builder()
                .commandRegistry(registry)
                .build();
        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt("[simple@aesh]$ ");
        console.start();
    }

    @CommandDefinition(name = "exit", description = "exit the program", aliases = {"quit"})
    public static class ExitCommand implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.stop();
            return CommandResult.SUCCESS;
        }
    }
}
----
[source,java]


