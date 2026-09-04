/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.builtins.grep;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.completer.OptionCompleter;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.io.Resource;
import org.aesh.readline.completion.CompleteOperation;
import org.aesh.impl.util.FileLister;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@CommandDefinition(name = "grep",
        description = "[OPTION]... PATTERN [FILE]...\n"+
                "Search for PATTERN in each FILE or standard input.\n"+
                "PATTERN is a regular expression.\n" +
                "Example: grep -i 'hello world' menu.h main.c\n")
public class Grep implements Command<CommandInvocation> {

    @Option(shortName = 'H', name = "help", hasValue = false,
            description = "display this help and exit")
    private boolean help;

//    @Option(shortName = 'E', name = "extended-regexp", hasValue = false,
//            description = "PATTERN is an extended regular expression (ERE)")
//    private boolean extendedRegex;
//
//    @Option(shortName = 'F', name = "fixed-strings", hasValue = false,
//            description = "PATTERN is a set of newline-separated fixed strings")
//    private boolean fixedStrings;
//
//    @Option(shortName = 'G', name = "basic-regexp", hasValue = false,
//            description = "PATTERN is a basic regular expression (BRE)")
//    private boolean basicRegexp;
//
//    @Option(shortName = 'P', name = "perl-regexp", hasValue = false,
//            description = "PATTERN is a Perl regular expression")
//    private boolean perlRegexp;
//
//    @Option(shortName = 'e', name = "regexp", argument = "PATTERN",
//            description = "use PATTERN for matching")
//    private String regexp;
//
//    @Option(shortName = 'f', name = "file", argument = "FILE",
//            description = "obtain PATTERN from FILE")
//    private Resource file;

    @Option(shortName = 'i', name = "ignore-case", hasValue = false,
            description = "ignore case distinctions")
    private boolean ignoreCase;

     @Option(shortName = 'n', name = "line-number", hasValue = false,
            description = "Prefix each line of output with the 1-based line number within its input file.")
    private boolean lineNumber;

     @Option(shortName = 'o', name = "only-matching", hasValue = false,
            description = "Print only the matched (non-empty) parts of a matching line, with each such  part  on  a separate output line.")
    private boolean onlyMatching;

     @Option(shortName = 'c', name = "count", hasValue = false,
            description = "Suppress normal output; instead print a count of matching lines for each input file.")
    private boolean count;

    @Arguments(completer = GrepCompletor.class,
            description = "The pattern to grep followed by file paths.")
    private List<String> arguments;

    private Pattern pattern;

    private int numberOfLines;

    private int numberOfMatchedLines;

    public List<String> getArguments() {
        return arguments;
    }

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        //just display help and return
        if(help || arguments == null || arguments.size() == 0) {
            commandInvocation.println(commandInvocation.getHelpInfo("grep"), true);
            return CommandResult.SUCCESS;
        }

        //reset numberOfLines if we've used grep before
        numberOfLines = 0;
        numberOfMatchedLines = 0;

        //first create the pattern
        try {
            if(ignoreCase)
                pattern = Pattern.compile(arguments.remove(0), Pattern.CASE_INSENSITIVE);
            else
                pattern = Pattern.compile(arguments.remove(0));
        }
        catch(PatternSyntaxException pse) {
            commandInvocation.println("grep: invalid pattern.");
            return CommandResult.FAILURE;
        }

        try {
            //do we have data from a pipe/redirect?
            if (commandInvocation.getConfiguration().getPipedData() != null
                    && commandInvocation.getConfiguration().getPipedData().available() > 0) {
                java.util.Scanner s = new java.util.Scanner(commandInvocation.getConfiguration().getPipedData()).useDelimiter("\\A");
                String input = s.hasNext() ? s.next() : "";
                for(String line : input.split("\\R")) {
                    numberOfLines++;
                    doGrep(line, commandInvocation);
                }
                if(count)
                    commandInvocation.println(String.valueOf(numberOfMatchedLines), true);

            } //find argument files and build regex..
            else if (arguments != null && arguments.size() > 0) {
                for (String s : arguments) {
                    doGrep(commandInvocation.getConfiguration().getAeshContext().
                            getCurrentWorkingDirectory().newInstance(s),
                            commandInvocation);
                }
            } //posix starts an interactive shell and read from the input here
            //atm, we'll just quit
            else {
                commandInvocation.println("grep: no file or input given.");
                return CommandResult.SUCCESS;
            }
        } catch (IOException ex) {
            throw new CommandException(ex);
        }

        return null;
    }

    private void doGrep(Resource file, CommandInvocation invocation) throws IOException {
        if (!file.exists()) {
            invocation.println("grep: " + file.toString() + ": No such file or directory");
        }
        else if (file.isLeaf()) {
            try (BufferedReader reader = Files.newBufferedReader(Paths.get(file.getAbsolutePath()))) {
                if (pattern != null) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        numberOfLines++;
                        doGrep(line, invocation);
                    }
                    if(count)
                        invocation.println(String.valueOf(numberOfMatchedLines), true);
                }
                else
                    invocation.println("No pattern given");
            }

        }
    }

    private void doGrep(String line, CommandInvocation invocation) {
        if(pattern != null) {
            Matcher matcher = pattern.matcher(line);
            if(matcher.find()) {
                numberOfMatchedLines++;
                //check if count is set, if so it overrides the output
                if(!count) {
                    if(lineNumber) {
                        if (onlyMatching) {
                            invocation.println(numberOfLines + ": " + line.substring(matcher.start(), matcher.end()), true);
                        }
                        else
                            invocation.println(numberOfLines +": "+line, true);
                    }
                    else {
                        if(onlyMatching)
                            invocation.println(line.substring(matcher.start(), matcher.end()), true);
                        else
                            invocation.println(line, true);
                    }
                }
            }
        }
    }

    /**
     * First argument is the pattern
     * All other arguments should be files
     */
    public static class GrepCompletor implements OptionCompleter<CompleterInvocation> {

        @Override
        public void complete(CompleterInvocation completerData) {
            Grep grep = (Grep) completerData.getCommand();
            //the first argument is the pattern, do not autocomplete
            if (grep.getArguments() != null && grep.getArguments().size() > 0) {
                CompleteOperation completeOperation
                        = new AeshCompleteOperation(completerData.getAeshContext(),
                                completerData.getGivenCompleteValue(), 0);
                List<String> candidates = new ArrayList<>();
                if (completerData.getGivenCompleteValue() == null) {
                    new FileLister("", completerData.getAeshContext().getCurrentWorkingDirectory()).
                            findMatchingDirectories(candidates);
                    completeOperation.addCompletionCandidates(candidates);
                } else {
                    int offset = new FileLister(completerData.getGivenCompleteValue(), completerData.getAeshContext().getCurrentWorkingDirectory()).
                            findMatchingDirectories(candidates);
                    completeOperation.addCompletionCandidates(candidates);
                    completeOperation.setOffset(completerData.getGivenCompleteValue().length() - offset);
                }

                if (completeOperation.getCompletionCandidates().size() > 1) {
                    completeOperation.removeEscapedSpacesFromCompletionCandidates();
                }

                completerData.setCompleterValuesTerminalString(completeOperation.getCompletionCandidates());
                completerData.setOffset(completeOperation.getOffset());
                if (completerData.getGivenCompleteValue() != null && completerData.getCompleterValues().size() == 1) {
                    completerData.setAppendSpace(completeOperation.isAppendSeparator());
                }
            }
        }
    }
}
