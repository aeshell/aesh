/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
    private ByteArrayOutputStream redirectPipeOutBuffer;
    private ByteArrayOutputStream redirectPipeErrBuffer;
    private List<ConsoleOperation> operations;
    private ConsoleOperation currentOperation;
    private AliasManager aliasManager;
    private ExportManager exportManager;
    private Shell shell;

    private ArrayBlockingQueue<CommandOperation> inputQueue;

    private ArrayBlockingQueue<int[]> cursorQueue;
    private transient boolean readingCursor = false;

    private ExecutorService readerService;
    private ExecutorService executorService;

    private AeshContext context;

    private ProcessManager processManager;

    private ConsoleBuffer consoleBuffer;
    private InputProcessor inputProcessor;
    private CompletionHandler completionHandler;

    private AeshStandardStream standardStream;
    private transient boolean initiateStop = false;

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
                        //.buffer() buffer = new Buffer(settings.isAnsiConsole(), null);
                .editMode(editMode)
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
            exportManager = new ExportManager(settings.getExportFile());
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
                    if(action != Action.IGNOREEOF) {
                        if(processManager.hasRunningProcess())
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

    public synchronized void start() {
        if(running)
            throw new IllegalStateException("Not allowed to start the Console without stopping it first");
        if(consoleCallback == null)
            throw new IllegalStateException("Not possible to start the Console without setting ConsoleCallback");
        running = true;
        displayPrompt();
        startReader();
        startExecutor();
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

    public void stop() {
        initiateStop = true;
        try {
            getTerminal().close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stop the Console, close streams, and reset terminals.
     * WARNING: After this is called the Console object must be reset
     * before its used.
     * @throws IOException stream
     */
    private void doStop() throws IOException {
        if(running) {
            try {
                running = false;
                getTerminal().close();
                getTerminal().reset();
                inputProcessor.getHistory().stop();
                if(aliasManager != null)
                    aliasManager.persist();
                if(exportManager != null)
                    exportManager.persistVariables();
                if(settings.isLogging())
                    LOGGER.info("Done stopping reading thread. Terminal is reset");
                processManager.stop();
                readerService.shutdown();
                executorService.shutdown();
            }
            finally {
                settings.getInputStream().close();
                settings.getStdErr().close();
                settings.getStdOut().close();
                if(settings.isLogging())
                    LOGGER.info("Streams are closed");
            }
        }
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

    private boolean hasInput() {
        return inputQueue.size() > 0;
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
        if(currentOperation != null) {
            ConsoleOperation tmpOutput = null;
            try {
                tmpOutput = parseCurrentOperation();
            }
            catch (IOException e) { e.printStackTrace(); }

            if(tmpOutput != null && !readerService.isShutdown())
                processManager.startNewProcess(consoleCallback, tmpOutput);

            inputProcessor.clearBufferAndDisplayPrompt();
        }
        else {
            inputProcessor.resetBuffer();
            if(initiateStop) {
                try {
                    doStop();
                    initiateStop = false;
                }
                catch (IOException e) {
                    LOGGER.warning("Stop failed: " + e.getCause());
                }
            }
            else
                displayPrompt();
        }
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
        Runnable reader = new Runnable() {
            @Override
            public void run() {
                try {
                    while(read()) { }
                }
                finally {
                    try {
                        doStop();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }

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
                    ie.printStackTrace();
                }
                finally {
                    try {
                        doStop();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        };
        executorService.execute(reader);
    }

    private boolean read() {
        try {
            int[] input = getTerminal().read(settings.isReadAhead());
            if(settings.isLogging()) {
                LOGGER.info("GOT: " + Arrays.toString(input));
            }
            if(readingCursor) {
                if(input.length > 4) {
                    cursorQueue.add(input);
                    readingCursor = false;
                    return true;
                }
            }
            //close thread, exit
            if(input.length == 0 || input[0] == -1) {
                //dont have to initiate it twice
                if(!initiateStop)
                    stop();
                return false;
            }

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
                        processManager.hasRunningProcess()) {
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
            return true;
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            if(settings.isLogging())
                LOGGER.severe("Stream failure, stopping Aesh: "+ioe);
            try {
                //if we get an ioexception/interrupted exp its either input or output failure
                //lets just stop while we can...
                doStop();
                return false;
            }
            catch (IOException ignored) {
                ignored.printStackTrace();
                return false;
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void execute() {
        while(!processManager.hasRunningProcess() && hasInput()) {
            try {
                processInternalOperation(getInput());
            }
            catch (IOException | InterruptedException e) {
                if(settings.isLogging())
                    LOGGER.warning("Execution exception: "+e.getMessage());
            }
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
            if(result.length() == 0) {
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

                    File readFile = new File(Parser.switchEscapedSpacesToSpacesInWord(line.getWords().get(0)));
                    if(readFile.isFile()) {
                        standardStream.setStdIn(new BufferedInputStream(
                                new FileInputStream(readFile)));
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
                FileUtils.saveFile(new File(Parser.switchEscapedSpacesToSpacesInWord( fileName)), redirectPipeOutBuffer.toString(), false);
            else if(redirection == ControlOperator.OVERWRITE_ERR)
                FileUtils.saveFile(new File(Parser.switchEscapedSpacesToSpacesInWord( fileName)), redirectPipeErrBuffer.toString(), false);
            else if(redirection == ControlOperator.APPEND_OUT)
                FileUtils.saveFile(new File(Parser.switchEscapedSpacesToSpacesInWord( fileName)), redirectPipeOutBuffer.toString(), true);
            else if(redirection == ControlOperator.APPEND_ERR)
                FileUtils.saveFile(new File(Parser.switchEscapedSpacesToSpacesInWord( fileName)), redirectPipeErrBuffer.toString(), true);
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
                    out().print(ANSI.getCurrentCursorPos());
                    out().flush();
                    console.readingCursor = true;

                    return getActualCursor(console.cursorQueue.take());
                }
                catch (Exception e) {
                    if(console.settings.isLogging())
                        console.LOGGER.log(Level.SEVERE, "Failed to find current row with ansi code: ",e);
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
