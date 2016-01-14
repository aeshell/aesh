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
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aesh.console;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.complete.CompletionRegistration;
import org.jboss.aesh.console.alias.Alias;
import org.jboss.aesh.console.alias.AliasCompletion;
import org.jboss.aesh.console.alias.AliasManager;
import org.jboss.aesh.console.command.CommandOperation;
import org.jboss.aesh.console.command.InternalCommands;
import org.jboss.aesh.console.export.ExportCompletion;
import org.jboss.aesh.console.export.ExportManager;
import org.jboss.aesh.console.operator.ControlOperator;
import org.jboss.aesh.console.operator.ControlOperatorParser;
import org.jboss.aesh.console.operator.RedirectionCompletion;
import org.jboss.aesh.console.reader.AeshStandardStream;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.edit.EditMode;
import org.jboss.aesh.edit.actions.Action;
import org.jboss.aesh.history.History;
import org.jboss.aesh.io.Resource;
import org.jboss.aesh.parser.AeshLine;
import org.jboss.aesh.parser.Parser;
import org.jboss.aesh.terminal.CursorPosition;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.terminal.Shell;
import org.jboss.aesh.terminal.Terminal;
import org.jboss.aesh.terminal.TerminalSize;
import org.jboss.aesh.util.ANSI;
import org.jboss.aesh.util.FileUtils;
import org.jboss.aesh.util.LoggerUtil;

/**
 * A console reader. Supports ansi terminals
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class Console {

    private Settings settings;

    private ConsoleCallback consoleCallback;

    private volatile boolean running = false;
    private volatile boolean initiateStop = false;
    private volatile boolean reading = false;
    private volatile int[] readingInput = null;
    private volatile boolean processing = false;

    private ByteArrayOutputStream redirectPipeOutBuffer;
    private ByteArrayOutputStream redirectPipeErrBuffer;
    private List<ConsoleOperation> operations;
    private ConsoleOperation currentOperation;
    private AliasManager aliasManager;
    private ExportManager exportManager;
    private Shell shell;

    private ArrayBlockingQueue<CommandOperation> inputQueue;

    private ArrayBlockingQueue<int[]> cursorQueue;
    private volatile boolean readingCursor = false;

    private ExecutorService readerService;
    private ExecutorService executorService;

    private AeshContext context;

    private ProcessManager processManager;

    private ConsoleBuffer consoleBuffer;
    private InputProcessor inputProcessor;
    private CompletionHandler completionHandler;

    private AeshStandardStream standardStream;

    private boolean controlledMode = false;

    private static final Logger LOGGER = LoggerUtil.getLogger(Console.class.getName());

    public Console(final Settings settings) {
        this.settings = settings;
        try {
            init();
        }
        catch (IOException e) {
            LOGGER.severe("Æsh failed during setup: " + e.getMessage());
        }

    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        try {
            if(settings != null) {
                if(settings.getTerminal() != null)
                    settings.getTerminal().reset();
                if(settings.getQuitHandler() != null)
                    settings.getQuitHandler().quit();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected Settings getSettings() {
        return settings;
    }

    /**
     * Reset the Console with Settings
     * Can only be called after stop()
     *
     * @throws IOException stream
     */
    private void init() throws IOException {
        if(running)
            throw new RuntimeException("Cant reset an already running Console, must stop if first!");
        //if we already have reset, just return
        if(readerService != null && !readerService.isShutdown()) {
            return;
        }
        if(settings.isLogging())
            LOGGER.info("RESET");

        readerService = Executors.newFixedThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = Executors.defaultThreadFactory().newThread(runnable);
                thread.setName("Aesh Read Loop " + runnable.hashCode());
                thread.setDaemon(true);
                return thread;
            }
        });

        executorService = Executors.newFixedThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = Executors.defaultThreadFactory().newThread(runnable);
                thread.setName("Aesh Process Loop " + runnable.hashCode());
                return thread;
            }
        });
        context = settings.getAeshContext();

        if(settings.doReadInputrc())
            settings = Config.parseInputrc(settings);

        settings = Config.readRuntimeProperties(settings);

        //init terminal
        settings.getTerminal().init(settings);

        EditMode editMode = settings.getEditMode();
        editMode.init(this);

        inputQueue = new ArrayBlockingQueue<>(50000);
        cursorQueue = new ArrayBlockingQueue<>(1);

        processManager = new ProcessManager(this, settings.isLogging());

        operations = new ArrayList<>();
        currentOperation = null;

        standardStream = new AeshStandardStream();

        redirectPipeOutBuffer = new ByteArrayOutputStream();
        redirectPipeErrBuffer = new ByteArrayOutputStream();
        //setPrompt(new Prompt(""));

        shell = new ConsoleShell(getInternalShell(), this);

        consoleBuffer = new AeshConsoleBufferBuilder()
                .shell(shell)
                .editMode(editMode)
                .ansi(settings.isAnsiConsole())
                .create();

        completionHandler = new AeshCompletionHandler(context, consoleBuffer, shell, true);
        //enable completion for redirection
        completionHandler.addCompletion( new RedirectionCompletion());

        //enable aliasing
        if(settings.isAliasEnabled()) {
            if(settings.isLogging())
                LOGGER.info("enable aliasmanager with file: "+settings.getAliasFile());
            aliasManager = new AliasManager(settings.getAliasFile(), settings.doPersistAlias(), settings.getName());
            completionHandler.addCompletion(new AliasCompletion(aliasManager));
            completionHandler.setAliasManager(aliasManager);
        }

        //enable export
        if(settings.isExportEnabled()) {
            if(settings.isLogging())
                LOGGER.info("enabling exportManager with file: "+settings.getExportFile());
            exportManager = new ExportManager(settings.getExportFile(), settings.doExportUsesSystemEnvironment());
            completionHandler.addCompletion(new ExportCompletion(exportManager));
        }

        //InterruptHandler for InputProcessor
        InputProcessorInterruptHook interruptHook = new InputProcessorInterruptHook() {
            @Override
            public void handleInterrupt(Action action) {
                if(settings.hasInterruptHook()) {
                    settings.getInterruptHook().handleInterrupt(Console.this, action);
                }
                else {
                    if(action == Action.IGNOREEOF) {
                        displayPrompt();
                    }
                    else {
                        stop();
                        if(processManager.hasForegroundProcess())
                            stop();
                        else {
                            try {
                                doStop();
                            }
                            catch (IOException e) {
                                LOGGER.warning("Failed to stop aesh! " + e.getMessage());
                            }
                        }
                    }
                }
            }
        };

        inputProcessor = new AeshInputProcessorBuilder()
                .consoleBuffer(consoleBuffer)
                .completion(completionHandler)
                .settings(settings)
                .interruptHook(interruptHook)
                .create();
    }

    /**
     *
     * @return get the terminal size
     */
    public TerminalSize getTerminalSize() {
        return getInternalShell().getSize();
    }

    /**
     * Get the History object
     *
     * @return history
     */
    public History getHistory() {
        return inputProcessor.getHistory();
    }

    /**
     * Set the current prompt.
     *
     * @param prompt prompt
     */
    public void setPrompt(Prompt prompt) {
        consoleBuffer.setPrompt(prompt);
    }

    public Prompt getPrompt() {
        return consoleBuffer.getBuffer().getPrompt();
    }

    public ExportManager getExportManager() {
        return exportManager;
    }

    public void setConsoleCallback(ConsoleCallback consoleCallback) {
        this.consoleCallback = consoleCallback;
    }

    public ConsoleCallback getConsoleCallback() {
        return consoleCallback;
    }

    public void changeOutputStream(PrintStream output) {
      if(output != null) {
          consoleBuffer.changeOutputBuffer(output);
          getTerminal().changeOutputStream(output);
      }
    }

    /**
     * Returns true if the console is waiting for input and no foreground process is executing.
     *
     * @return
     */
    public boolean isWaiting(){
        return (!processing && !processManager.hasForegroundProcess() && !getTerminal().hasInput() && readingInput == null && !hasInput());
    }

    /**
     * Returns true if the console is waiting for input and no process, foreground or background, is executing.
     *
     * @return
     */
    public boolean isWaitingWithoutBackgroundProcess(){
        return (!processing && !processManager.hasProcesses() && !getTerminal().hasInput() && readingInput == null && !hasInput());
    }

    public void controlled() {
        controlledMode = true;
    }

     public void continuous() {
         controlledMode = false;
     }

    public synchronized void start() {
        if(running)
            throw new IllegalStateException("Not allowed to start the Console without stopping it first");
        if(consoleCallback == null)
            throw new IllegalStateException("Not possible to start the Console without setting ConsoleCallback");
        running = true;
        startReader();
        startExecutor();
        consoleBuffer.setPrompted(false);
        if(settings.getExecuteAtStart() != null)
            pushToInputStream(settings.getExecuteAtStart());
        if(settings.getExecuteFileAtStart() != null) {
            readExecuteFile();
        }
    }

    private PrintStream out() {
        //if redirection enabled, put it into a buffer
        if(currentOperation != null && currentOperation.getControlOperator().isRedirectionOut()) {
            return new PrintStream(redirectPipeOutBuffer, true);
        }
        else {
            return getInternalShell().out();
        }
    }

    private PrintStream err(){
        //if redirection enabled, put it into a buffer
        if(currentOperation != null && currentOperation.getControlOperator().isRedirectionErr()) {
            return new PrintStream(redirectPipeErrBuffer, true);
        }
        else {
            return getInternalShell().err();
        }
    }

    //TODO:
    private AeshStandardStream in() {
        return standardStream;
    }

    public AeshContext getAeshContext() {
        return context;
    }

    /**
     * Add a Completion to the completion list
     *
     * @param completion comp
     */
    public CompletionRegistration addCompletion(final Completion completion) {
        completionHandler.addCompletion(completion);
        return new CompletionRegistration() {
            @Override
            public void removeCompletion() {
                completionHandler.removeCompletion(completion);
            }
        };
    }

    /**
     * Runtime enable/disable of completion capabilities
     *
     * @param completionEnabled
     */
    public void setCompletionEnabled(boolean completionEnabled){
        completionHandler.setEnabled(completionEnabled);
    }

    public void stop() {
       initiateStop = true;
       try {
           doStop();
       }
       catch(IOException e) {
           LOGGER.log(Level.WARNING, "Got exception during stop: ", e);
       }
    }

    /**
     * Stop the Console, close streams, and reset terminals.
     * WARNING: After this is called the Console object must be reset
     * before its used.
     * @throws IOException stream
     */
    private synchronized void doStop() throws IOException {
        if(running) {
            running = false;
            if(initiateStop)
                initiateStop = false;

            //we need to make sure that we finish the data we
            // have already parsed and put into the queue before we quit
            // - to prevent a deadlock we add a counter
            int counter = 0;
            while(!inputQueue.isEmpty() && counter < 10) {
                try {
                    Thread.sleep(10);
                    counter++;
                }
                catch (InterruptedException e) {
                    LOGGER.log(Level.WARNING, "Exception while waiting on inputqueue to flush: ", e);
                }
            }
            if(counter == 10) {
                LOGGER.log(Level.WARNING, "InputQueue still contains items after stop: "+inputQueue.toString());
            }

            inputProcessor.getHistory().stop();
            if(aliasManager != null)
                aliasManager.persist();
            if(exportManager != null)
                exportManager.persistVariables();
            processManager.stop();
            readerService.shutdown();
            executorService.shutdown();
            if(settings.isLogging())
                LOGGER.info("Done stopping services. Terminal is reset");
        }

        getTerminal().close();
        getTerminal().reset();
    }

    /**
     *
     * @return true if Console is set up and streams are open
     */
    public boolean isRunning() {
        return running;
    }

    public void clearBufferAndDisplayPrompt() {
        inputProcessor.clearBufferAndDisplayPrompt();
    }

    protected CommandOperation getInput() throws InterruptedException {
        return inputQueue.take();
    }

    protected InputProcessor getInputProcessor() {
        return inputProcessor;
    }

    /**
     * Put the current process in the background
     */
    public void putProcessInBackground(int pid) {
        processManager.putProcessInBackground(pid);
    }

    /**
     * Put the current process in the foreground
     */
    public void putProcessInForeground(int pid) {
        processManager.putProcessInForeground(pid);
    }

    public void pushToInputStream(String input) {
        getTerminal().writeToInputStream(input);
    }

    public boolean hasInput() {
        return inputQueue.size() > 0;
    }

    public boolean hasRunningProcesses() {
        return processManager.hasForegroundProcess();
    }

    /**
     * Read from the input stream and return when user have pressed enter.
     * This method will block until enter is pressed or because of interruption.
     *
     * @return input
     * @throws InterruptedException
     */
    public String getInputLine() throws InterruptedException {
        String result;
        try {
            do {
                result = inputProcessor.parseOperation(getInput());
            }
            while(result == null);

            return result;
        }
        catch(InterruptedException e) {
           LOGGER.log(Level.WARNING, "GOT INTERRUPTED: ",e);
           throw e;
        }
        catch(IOException ioe) {
            LOGGER.log(Level.WARNING, "Failure while reading input: ",ioe);
            return null;
        }
    }

    /**
     * @return get the current shell
     */
    private Shell getInternalShell() {
        return settings.getTerminal().getShell();
    }

    /**
     * @return get the current shell
     */
    public Shell getShell() {
        return shell;
    }

    public void currentProcessFinished(Process process) {
        //piped/redirect commands
        if(currentOperation != null) {
            ConsoleOperation tmpOutput = null;
            try {
                tmpOutput = parseCurrentOperation();
            }
            catch (IOException e) {
              e.printStackTrace();
            }

            if(tmpOutput != null && !readerService.isShutdown())
                processManager.startNewProcess(consoleCallback, tmpOutput);

            inputProcessor.clearBufferAndDisplayPrompt();
        }
        else {
            if(running || !inputQueue.isEmpty()) {
                inputProcessor.resetBuffer();
                if (!consoleBuffer.isPrompted()) {
                    displayPrompt();
                }
            }
        }
        consoleBuffer.setPrompted(false);
    }

    private Terminal getTerminal() {
        return settings.getTerminal();
    }

    /**
     * Get the current console buffer line (no masking)
     *
     * @return current buffer
     */
    public String getBuffer() {
        if(consoleBuffer.getBuffer() == null)
            return "";
        else
            return consoleBuffer.getBuffer().getLineNoMask();
    }

    /**
     * Read from the input stream, perform action according to mapped
     * operations/completions/etc
     */
    private void startReader() {
        reading = true;
        Runnable reader = new Runnable() {
            @Override
            public void run() {
                try {
                    while(read()) { }
                }
                finally {
                    reading = false;
                }
            }
        };
        readerService.execute(reader);
    }

    private void startExecutor() {
        Runnable reader = new Runnable() {
            @Override
            public void run() {
                try {
                    while(!executorService.isShutdown()) {
                        execute();
                        Thread.sleep(10);
                    }
                }
                catch (InterruptedException ie) {
                    LOGGER.log(Level.WARNING, "Exception while executing:", ie);
                }
                finally {
                    if(!initiateStop || running)
                        stop();
                }
            }
        };
        executorService.execute(reader);
    }

    private boolean read() {
        try {
            readingInput = getTerminal().read();
            if(settings.isLogging()) {
                LOGGER.info("GOT: " + Arrays.toString(readingInput));
            }
            if(readingCursor) {
                if(readingInput.length > 4) {
                    cursorQueue.add(readingInput);
                    readingCursor = false;
                    return true;
                }
            }
            //close thread, exit
            if(readingInput.length == 0 || readingInput[0] == -1 || initiateStop) {
                LOGGER.info("Received null input or -1, or stop() has been called, stop reading");
                return false;
            }

            parseInput(readingInput);
            return true;
        }
        catch (IOException ioe) {
            if(settings.isLogging())
                LOGGER.log(Level.SEVERE, "Stream failure, stopping Aesh: ",ioe);
            //if we get an ioexception/interrupted exp its either input or output failure
            //lets just stop while we can...
            stop();
            return false;
        }
        catch (InterruptedException e) {
            if(settings.isLogging())
                LOGGER.log(Level.SEVERE, "Stream failure, stopping Aesh: ",e);
            return false;
        }finally {
            readingInput = null;
        }
    }

    private void parseInput(int[] input) throws InterruptedException {
        boolean parsing = true;
        //use a position instead of changing the array
        int position = 0;
        //if we get a paste or have input lag this should parse it correctly...
        while(parsing) {
            Key inc = Key.findStartKey(input, position);
            if(input.length > inc.getKeyValues().length+position) {
                position += inc.getKeyValues().length;
            }
            else {
                parsing = false;
            }
            //if we get ctrl-c/d while a process is running we'll try to kill
            if((inc == Key.CTRL_C || inc == Key.CTRL_D) &&
                    processManager.hasForegroundProcess()) {
                //try to kill running process
                try {
                    if(settings.isLogging())
                        LOGGER.info("killing process: "+processManager.getCurrentProcess().getPID());
                    processManager.getCurrentProcess().interrupt();
                }
                catch(InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
            else {
                inputQueue.put(new CommandOperation(inc, input, position));
            }
        }
    }

    private void execute() {
        while(!processManager.hasForegroundProcess() && hasInput() && !controlledMode) {
            processing = true;
            try {
                processInternalOperation(getInput());
            }
            catch (IOException | InterruptedException e) {
                if(settings.isLogging())
                    LOGGER.warning("Execution exception: "+e.getMessage());
            }finally {
                processing = false;
            }
        }

        if(!processManager.hasProcesses() && !hasInput() && !reading ){
            consoleBuffer.out().print(Config.getLineSeparator());
            stop();
        }
    }

    private void processInternalOperation(CommandOperation commandOperation) throws IOException {
        String result = inputProcessor.parseOperation(commandOperation);
        if(result != null)
            processOperationResult(result);
    }

    private void processOperationResult(String result) {
        try {
            //if the input length is 0 we should exit quickly
            //if we are stopping, dont print the prompt
            if(result.length() == 0 && running) {
                inputProcessor.clearBufferAndDisplayPrompt();
                return;
            }
            if(result.startsWith(Parser.SPACE))
                result = Parser.trimInFront(result);

            if(settings.isOperatorParserEnabled())
                operations = ControlOperatorParser.findAllControlOperators(result);
            else {
                //if we do not parse operators just add ControlOperator.NONE
                operations = new ArrayList<>(1);
                operations.add(new ConsoleOperation(ControlOperator.NONE, result));
            }

            ConsoleOperation output = parseOperations();
            output = processInternalCommands(output);
            if(output.getBuffer() != null) {
                processManager.startNewProcess(consoleCallback, output);
                //abort if the user have initiated stop
                //if(readerService.isShutdown())
                //    return;
            }
            else {
                inputProcessor.clearBufferAndDisplayPrompt();
            }
        }
        catch (IOException ioe) {
            if(settings.isLogging())
                LOGGER.severe("Stream failure: "+ioe);
        }
    }

    private void displayPrompt() {
        consoleBuffer.displayPrompt();
    }

    /**
     * Clear a ansi terminal
     *
     * @throws IOException stream
     */
    public void clear() throws IOException {
        consoleBuffer.clear(false);
    }

    private ConsoleOperation parseCurrentOperation() throws IOException {
        if(currentOperation.getControlOperator() == ControlOperator.OVERWRITE_OUT
                || currentOperation.getControlOperator() == ControlOperator.OVERWRITE_ERR
                || currentOperation.getControlOperator() == ControlOperator.APPEND_OUT
                || currentOperation.getControlOperator() == ControlOperator.APPEND_ERR
                || currentOperation.getControlOperator() == ControlOperator.OVERWRITE_OUT_AND_ERR) {

            ConsoleOperation nextOperation = operations.remove(0);
            persistRedirection(nextOperation.getBuffer(), currentOperation.getControlOperator());
            if(nextOperation.getControlOperator() == ControlOperator.NONE) {
                redirectPipeErrBuffer = new ByteArrayOutputStream();
                redirectPipeOutBuffer = new ByteArrayOutputStream();
                currentOperation = null;
                return null;
            }
            else {
                redirectPipeErrBuffer = new ByteArrayOutputStream();
                redirectPipeOutBuffer = new ByteArrayOutputStream();
                currentOperation = nextOperation;
                return parseCurrentOperation();
            }
        }
        else if(currentOperation.getControlOperator() == ControlOperator.PIPE
                || currentOperation.getControlOperator() == ControlOperator.PIPE_OUT_AND_ERR) {
            return parseOperations();
        }
        else if(currentOperation.getControlOperator() == ControlOperator.END) {
            if(operations.size() > 0) {
                currentOperation = operations.remove(0);
                return currentOperation;
            }
            else {
                currentOperation = null;
                return null;
            }
        }
        //this should never happen (all overwrite_in should be parsed in parseOperations())
        else if(currentOperation.getControlOperator() == ControlOperator.OVERWRITE_IN) {
            if(settings.isLogging())
                LOGGER.info(settings.getName()+": syntax error while reading token: \'<\'");
            err().print(settings.getName() + ": syntax error while reading token: \'<\'");
            return null;
        }
        //ControlOperator.NONE
        else {
            //do nothing
            currentOperation = null;
            return null;
        }
    }

    /**
     * Find the next ConsoleOutput based on operations
     *
     * @return next ConsoleOutput
     */
    private ConsoleOperation parseOperations() throws IOException {

        ConsoleOperation output = null;
        ConsoleOperation op = operations.remove(0);

        if(op.getControlOperator() == ControlOperator.OVERWRITE_OUT
                || op.getControlOperator() == ControlOperator.OVERWRITE_ERR
                || op.getControlOperator() == ControlOperator.APPEND_OUT
                || op.getControlOperator() == ControlOperator.APPEND_ERR
                || op.getControlOperator() == ControlOperator.OVERWRITE_OUT_AND_ERR
                || op.getControlOperator() == ControlOperator.PIPE_OUT_AND_ERR
                || op.getControlOperator() == ControlOperator.PIPE ) {
            if(operations.size() == 0) {
                //throw some sort of exception
            }
            else {
                currentOperation = op;
                if(op.getControlOperator().isRedirectionOut()) {
                    standardStream.setStdIn(new BufferedInputStream(
                            new ByteArrayInputStream(redirectPipeOutBuffer.toString().getBytes())));
                }
                if(op.getControlOperator().isRedirectionErr()) {
                    standardStream.setStdError(new BufferedInputStream(
                            new ByteArrayInputStream(redirectPipeErrBuffer.toString().getBytes())));
                }

                //output = new ConsoleOutput(op, null, null);
                output = op;
            }
        }
        else if(op.getControlOperator() == ControlOperator.OVERWRITE_IN) {
            //1. we need to find next operation
            //2. use the buffer from the next operation to read file to buffer
            //3. switch redirection operation with next one
            if(operations.size() > 0) {
                ConsoleOperation nextOperation = operations.remove(0);
                if( nextOperation.getBuffer().length() > 0) {
                    AeshLine line = Parser.findAllWords(nextOperation.getBuffer());
                    currentOperation = new ConsoleOperation(nextOperation.getControlOperator(), op.getBuffer());

                    Resource fileRelativePath =
                            getAeshContext().getCurrentWorkingDirectory().newInstance(
                                    Parser.switchEscapedSpacesToSpacesInWord(line.getWords().get(0)));

                    Resource readFile = fileRelativePath.resolve( context.getCurrentWorkingDirectory()).get(0);
                    if(readFile.isLeaf()) {
                        standardStream.setStdIn(new BufferedInputStream( readFile.read()));
                        output = new ConsoleOperation(nextOperation.getControlOperator(),op.getBuffer());
                    }
                    else {
                        err().println(settings.getName() + ": " + readFile.toString()+ " no such file.");
                        currentOperation = null;
                        output = new ConsoleOperation(ControlOperator.NONE, op.getBuffer());
                    }
                }
                else {
                    if(settings.isLogging())
                        LOGGER.info(settings.getName()+": syntax error near unexpected token '<'"+Config.getLineSeparator());
                    err().print(settings.getName() + ": syntax error near unexpected token '<'" + Config.getLineSeparator());
                    currentOperation = null;
                    output = new ConsoleOperation(ControlOperator.NONE, "");
                }
            }
            else {
                if(settings.isLogging())
                    LOGGER.info(settings.getName()+": syntax error near unexpected token 'newline'"+Config.getLineSeparator());
                err().print(settings.getName() + ": syntax error near unexpected token 'newline'" + Config.getLineSeparator());
                currentOperation = null;
                output = new ConsoleOperation(ControlOperator.NONE, "");
            }
        }
        else if(op.getControlOperator() == ControlOperator.END) {
            currentOperation = op;
            output = op;
        }
        else {
            currentOperation = null;
            standardStream.setStdIn(new BufferedInputStream(
                    new ByteArrayInputStream(redirectPipeOutBuffer.toString().getBytes())));
            standardStream.setStdError(new BufferedInputStream(
                    new ByteArrayInputStream(redirectPipeErrBuffer.toString().getBytes())));
            output = op;
            //redirectPipeOutBuffer.toString(), redirectPipeErrBuffer.toString());
        }

        if(redirectPipeOutBuffer.toString().length() > 0)
            redirectPipeOutBuffer = new ByteArrayOutputStream();
        if(redirectPipeErrBuffer.toString().length() > 0)
            redirectPipeErrBuffer = new ByteArrayOutputStream();

        //todo: check if this flush is needed
        out().flush();
        if(output != null)
            return findAliases(output);
        else
            return new ConsoleOperation(ControlOperator.NONE, "");
    }

    private ConsoleOperation processInternalCommands(ConsoleOperation output) throws IOException {
        if(output.getBuffer() != null) {
            if(settings.isAliasEnabled() &&
                    output.getBuffer().startsWith(InternalCommands.ALIAS.getCommand())) {
                String out = aliasManager.parseAlias(output.getBuffer().trim());
                if(out != null) {
                    out().print(out);
                    out().flush();
                }
                //empty output, will result
                return new ConsoleOperation(ControlOperator.NONE, null);
            }
            else if(settings.isAliasEnabled() &&
                    output.getBuffer().startsWith(InternalCommands.UNALIAS.getCommand())) {
                String out = aliasManager.removeAlias(output.getBuffer().trim());
                if(out != null) {
                    out().print(out);
                    out().flush();
                }

                return new ConsoleOperation(ControlOperator.NONE, null);
            }
            else if(settings.isExportEnabled() &&
                    output.getBuffer().startsWith(InternalCommands.EXPORT.getCommand())) {
                if(output.getBuffer().trim().equals(InternalCommands.EXPORT.getCommand()))
                    out().print(exportManager.listAllVariables());
                else {
                    String out = exportManager.addVariable(output.getBuffer());
                    if(out != null) {
                        out().println(out);
                        out().flush();
                    }
                }
                return new ConsoleOperation(ControlOperator.NONE, null);
            }
        }
        return output;
    }

    private ConsoleOperation findAliases(ConsoleOperation operation) {

        if(settings.isExportEnabled()) {
            if(Parser.containsNonEscapedDollar(operation.getBuffer())) {
                operation = new ConsoleOperation(operation.getControlOperator(),
                        exportManager.getValue(operation.getBuffer()));
            }
        }

        if(settings.isAliasEnabled()) {
            String command = Parser.findFirstWord(operation.getBuffer());
            Alias alias = aliasManager.getAlias(command);

            if(alias != null) {
                operation = new ConsoleOperation(operation.getControlOperator(),
                        alias.getValue() + operation.getBuffer().substring(command.length()));
            }
        }
        return operation;
    }

    private void persistRedirection(String fileName, ControlOperator redirection) throws IOException {
        AeshLine line = Parser.findAllWords(fileName);
        if(line.getWords().size() > 1) {
            if(settings.isLogging())
                LOGGER.info(settings.getName()+": can't redirect to more than one file."+Config.getLineSeparator());
            err().print(settings.getName() + ": can't redirect to more than one file." + Config.getLineSeparator());
            return;
        }
        //this is safe since we check that buffer do contain text earlier
        else {
            fileName = line.getWords().get(0);
            if(fileName.startsWith("~/")) {
                fileName = Config.getHomeDir()+fileName.substring(1);
            }
        }

        try {
            if(redirection == ControlOperator.OVERWRITE_OUT)
                FileUtils.saveFile( context.getCurrentWorkingDirectory().newInstance(
                        Parser.switchEscapedSpacesToSpacesInWord(fileName)).resolve(
                        context.getCurrentWorkingDirectory()).get(0),  redirectPipeOutBuffer.toString(), false);
            else if(redirection == ControlOperator.OVERWRITE_ERR)
                FileUtils.saveFile( context.getCurrentWorkingDirectory().newInstance(
                                Parser.switchEscapedSpacesToSpacesInWord(fileName)).resolve(
                                context.getCurrentWorkingDirectory()).get(0),
                        redirectPipeErrBuffer.toString(), false);
            else if(redirection == ControlOperator.APPEND_OUT)
                FileUtils.saveFile( context.getCurrentWorkingDirectory().newInstance(
                                Parser.switchEscapedSpacesToSpacesInWord(fileName)).resolve(
                                context.getCurrentWorkingDirectory()).get(0),
                        redirectPipeOutBuffer.toString(), true);
            else if(redirection == ControlOperator.APPEND_ERR)
                FileUtils.saveFile( context.getCurrentWorkingDirectory().newInstance(
                                Parser.switchEscapedSpacesToSpacesInWord(fileName)).resolve(
                                context.getCurrentWorkingDirectory()).get(0),
                        redirectPipeErrBuffer.toString(), true);
        }
        catch (IOException e) {
            if(settings.isLogging())
                LOGGER.log(Level.SEVERE, "Saving file "+fileName+" to disk failed: ", e);
            getInternalShell().err().println(e.getMessage());
            err().flush();
        }
        redirectPipeOutBuffer = new ByteArrayOutputStream();
        redirectPipeErrBuffer = new ByteArrayOutputStream();
    }

    private void readExecuteFile() {
        if(settings.getExecuteFileAtStart() != null && settings.getExecuteFileAtStart().isLeaf()) {
            LOGGER.info("reading file");
            try {
                BufferedReader reader = new BufferedReader( new InputStreamReader(settings.getExecuteFileAtStart().read()));
                String line;
                while( ( line = reader.readLine() ) != null ) {
                    if(line.length() > 0) {
                        LOGGER.info("pushing: "+line);
                        pushToInputStream(line + Config.getLineSeparator());
                    }
                }
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class ConsoleShell implements Shell {
        private final Console console;
        private final Shell shell;

        ConsoleShell(Shell shell, Console console) {
            this.shell = shell;
            this.console = console;
        }

        @Override
        public void clear() throws IOException {
            shell.clear();
        }

        @Override
        public PrintStream out() {
            return console.out();
        }

        @Override
        public PrintStream err() {
            return console.err();
        }

        @Override
        public AeshStandardStream in() {
            return console.in();
        }

        @Override
        public TerminalSize getSize() {
            return console.getTerminalSize();
        }

        @Override
        public CursorPosition getCursor() {
            if(console.settings.isAnsiConsole() && Config.isOSPOSIXCompatible()) {
                try {
                    out().print(ANSI.CURSOR_ROW);
                    out().flush();
                    console.readingCursor = true;

                    return getActualCursor(console.cursorQueue.take());
                }
                catch (Exception e) {
                    if(console.settings.isLogging())
                        LOGGER.log(Level.SEVERE, "Failed to find current row with ansi code: ",e);
                    return new CursorPosition(-1,-1);
                }
            }
            return new CursorPosition(-1,-1);
        }

        private CursorPosition getActualCursor(int[] input) {
            boolean started = false;
            boolean gotSep = false;
            int col = 0;
            int row = 0;

            //read until we get a 'R'
            for(int i=0; i < input.length-1; i++) {
                if(started) {
                    if(input[i] == 82)
                        break;
                    else if(input[i] == 59) // we got a ';' which is the separator
                        gotSep = true;
                    else {
                        if(gotSep) {
                            //col.append((char) input[i]);
                            char c = (char) input[i];
                            col *= 10;
                            col += ((int)c & 0xF);
                        }
                        else {
                            char c = (char) input[i];
                            row *= 10;
                            row += ((int)c & 0xF);
                        }
                        //row.append((char) input[i]);
                    }
                }
                //search for the beginning which starts with esc,[
                else if(input[i] == Key.ESC.getFirstValue() && i < input.length-1 &&
                        input[i+1] == Key.LEFT_SQUARE_BRACKET.getFirstValue()) {
                    started = true;
                    i++;
                }
            }

            return new CursorPosition(row, col);
        }

        @Override
        public void setCursor(CursorPosition position) {
            shell.setCursor(position);
        }

        @Override
        public void moveCursor(int rows, int columns) {
            shell.moveCursor(rows, columns);
        }

        @Override
        public boolean isMainBuffer() {
            return shell.isMainBuffer();
        }

        @Override
        public void enableAlternateBuffer() {
            shell.enableAlternateBuffer();
        }

        @Override
        public void enableMainBuffer() {
            shell.enableMainBuffer();
        }
    }

}
