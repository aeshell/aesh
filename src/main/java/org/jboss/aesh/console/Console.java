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


/**
 * A console reader. Supports ansi terminals
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class Console {

    /*
    private Settings settings;

    private ConsoleCallback consoleCallback;

    private volatile boolean running = false;
    private volatile boolean initiateStop = false;
    private volatile boolean reading = false;
    private volatile int readingInput = -1;
    private volatile boolean processing = false;

    private ByteArrayOutputStream redirectPipeOutBuffer;
    private ByteArrayOutputStream redirectPipeErrBuffer;
    private List<ConsoleOperation> operations;
    private ConsoleOperation currentOperation;
    private AliasManager aliasManager;
    private ExportManager exportManager;
    private ConsoleShell shell;

    private NonBlockingReader reader;
    private BindingReader bindingReader;

    private ArrayBlockingQueue<int[]> cursorQueue;
    private volatile boolean readingCursor = false;

    private ExecutorService executorService;

    private AeshContext context;

    private ProcessManager processManager;

    private ConsoleBuffer consoleBuffer;
    private InputProcessor inputProcessor;
    private CompletionHandler completionHandler;

    private AeshStandardStream standardStream;

    private Terminal terminal;
    private Attributes attributes;
    private PrintStream out;

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
                if (attributes != null && terminal != null) {
                    terminal.setAttributes(attributes);
                }
                if(settings.quitHandler() != null)
                    settings.quitHandler().quit();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected Settings getSettings() {
        return settings;
    }

    private void init() throws IOException {
        if(running)
            throw new RuntimeException("Cant reset an already running Console, must stop if first!");
        //if we already have reset, just return
        if(executorService != null && !executorService.isShutdown()) {
            return;
        }
        if(settings.logging())
            LOGGER.info("RESET");

        executorService = Executors.newFixedThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = Executors.defaultThreadFactory().newThread(runnable);
                thread.setName("Aesh Process Loop " + runnable.hashCode());
                return thread;
            }
        });
        context = settings.aeshContext();

        EditMode editMode = settings.editMode();

        settings = Config.readRuntimeProperties(settings);

        //init terminal
        terminal = TerminalBuilder.builder()
                .streams(settings.stdIn(), settings.stdOut())
                .name("Aesh console")
                .build();
        attributes = terminal.enterRawMode();
        terminal.puts(Capability.keypad_xmit);
        out = new PrintStream(terminal.output());


        cursorQueue = new ArrayBlockingQueue<>(1);

        processManager = new ProcessManager(this, settings.logging());

        operations = new ArrayList<>();
        currentOperation = null;

        standardStream = new AeshStandardStream();

        redirectPipeOutBuffer = new ByteArrayOutputStream();
        redirectPipeErrBuffer = new ByteArrayOutputStream();
        //setPrompt(new Prompt(""));

        shell = new ConsoleShell();

        consoleBuffer = new AeshConsoleBufferBuilder()
                .shell(shell)
                .editMode(editMode)
                .ansi(settings.isAnsiConsole())
                .create();

        completionHandler = new AeshCompletionHandler(context, shell, true);
        //enable completion for redirection
        completionHandler.addCompletion( new RedirectionCompletion());

        //enable aliasing
        if(settings.aliasEnabled()) {
            if(settings.logging())
                LOGGER.info("enable aliasmanager with file: "+settings.aliasFile());
            aliasManager = new AliasManager(settings.aliasFile(), settings.persistAlias(), settings.getName());
            completionHandler.addCompletion(new AliasCompletion(aliasManager));
            completionHandler.setAliasManager(aliasManager);
        }

        //enable export
        if(settings.exportEnabled()) {
            if(settings.logging())
                LOGGER.info("enabling exportManager with file: "+settings.exportFile());
            exportManager = new ExportManager(settings.exportFile(), settings.exportUsesSystemEnvironment());
            completionHandler.addCompletion(new ExportCompletion(exportManager));
        }

        //InterruptHandler for InputProcessor
        InputProcessorInterruptHook interruptHook = action -> {
            if(settings.hasInterruptHook()) {
                settings.interruptHook().handleInterrupt(Console.this, action);
            }
            else {
                if(action.name().equals("ignore-eof")) {
                    displayPrompt();
                }
                //action is interrupt atm so just stop
                else {
                    stop();
                }
            }
        };

        inputProcessor = new AeshInputProcessorBuilder()
                .consoleBuffer(consoleBuffer)
                .completion(completionHandler)
                .settings(settings)
                .interruptHook(interruptHook)
                .create();

        reader = new NonBlockingReader(terminal.getName(),
                                       terminal.reader());
        bindingReader = new BindingReader(reader);
    }

    public Size getTerminalSize() {
        return terminal.getSize();
    }

    public History getHistory() {
        return inputProcessor.getHistory();
    }

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
          out = output;
      }
    }

    public boolean isWaiting(){
        return (!processing && !processManager.hasForegroundProcess() && !hasInput() && readingInput == -1);
    }

    public boolean isWaitingWithoutBackgroundProcess(){
        return (!processing && !processManager.hasProcesses() && !hasInput() && readingInput == -1);
    }

    public void controlled(){
        controlledMode = true;
    }

    public void continuous(){
        controlledMode = false;
    }

    public synchronized void start() {
        if(running)
            throw new IllegalStateException("Not allowed to start the Console without stopping it first");
        if(consoleCallback == null)
            throw new IllegalStateException("Not possible to start the Console without setting ConsoleCallback");
        // bridge to the current way of supporting signals
        terminal.handle(Signal.INT, s -> {
            try {
                inputProcessor.parseOperation(Key.CTRL_C);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        running = true;
        if(settings.executeAtStart() != null)
            pushToInputStream(settings.executeAtStart());
        if(settings.executeFileAtStart() != null) {
            readExecuteFile();
        }

        displayPrompt();
        startExecutor();
    }

    private PrintStream out() {
        //if redirection enabled, put it into a buffer
        if(currentOperation != null && currentOperation.getControlOperator().isRedirectionOut()) {
            return new PrintStream(redirectPipeOutBuffer, true);
        }
        else {
            return out;
        }
    }

    private PrintStream err(){
        //if redirection enabled, put it into a buffer
        if(currentOperation != null && currentOperation.getControlOperator().isRedirectionErr()) {
            return new PrintStream(redirectPipeErrBuffer, true);
        }
        else {
            return settings.stdErr();
        }
    }

    //TODO:
    private AeshStandardStream in() {
        return standardStream;
    }

    public AeshContext aeshContext() {
        return context;
    }

    public CompletionRegistration addCompletion(final Completion completion) {
        completionHandler.addCompletion(completion);
        return new CompletionRegistration() {
            @Override
            public void removeCompletion() {
                completionHandler.removeCompletion(completion);
            }
        };
    }

    public void setCompletionEnabled(boolean completionEnabled){
        completionHandler.setEnabled(completionEnabled);
    }

    public void stop() {
       try {
           if (running) {
               initiateStop = true;
               doStop();
           }
       }
       catch(IOException e) {
           LOGGER.log(Level.WARNING, "Got exception during stop: ", e);
       }
    }

    private synchronized void doStop() throws IOException {
        if(running) {
            running = false;
            if(initiateStop)
                initiateStop = false;

            inputProcessor.getHistory().stop();
            if(aliasManager != null)
                aliasManager.persist();
            if(exportManager != null)
                exportManager.persistVariables();
            processManager.stop();
            executorService.shutdownNow();
            terminal.close();
            reader.close();
            if(settings.logging())
                LOGGER.info("Done stopping services. Terminal is reset");
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void clearBufferAndDisplayPrompt() {
        inputProcessor.clearBufferAndDisplayPrompt();
    }

    protected KeyEvent getInput() throws InterruptedException {
        KeyEvent key = bindingReader.readBinding(Key.getKeyMap());
        if (key != null && key != Key.UNKNOWN) {
            return key;
        }
        else {
            return new KeyEvent() {
                private String input = bindingReader.getLastBinding();
                @Override
                public int getCodePointAt(int index) throws IndexOutOfBoundsException {
                    return input.charAt(index);
                }

                @Override
                public int length() {
                    return input.length();
                }

                @Override
                public String name() {
                    return input;
                }
            };
        }
    }

    protected <T> CmdOperation<T> getInput(KeyMap<T> keyMap) throws InterruptedException {
        T op = bindingReader.readBinding(keyMap);
        if (op != null) {
            return new CmdOperation<>(op, bindingReader.getLastBinding());
        }
        return null;
    }

    protected InputProcessor getInputProcessor() {
        return inputProcessor;
    }

    public void putProcessInBackground(int pid) {
        processManager.putProcessInBackground(pid);
    }

    public void putProcessInForeground(int pid) {
        processManager.putProcessInForeground(pid);
    }

    public void pushToInputStream(String input) {
        bindingReader.runMacro(input);
    }

    public boolean hasInput() {
        try {
            return reader.ready();
        } catch (IOException e) {
            return false;
        }
    }

    public boolean hasRunningProcesses() {
        return processManager.hasForegroundProcess();
    }


    public String getInputLine() throws InterruptedException {
        String result;
        try {
            do {
                result = inputProcessor.parseOperation(getInput());
            }
            while(result == null && running && !initiateStop);

            return result;
        }
        catch(InterruptedException e) {
            if (!initiateStop && running) {
                LOGGER.log(Level.WARNING, "GOT INTERRUPTED: ", e);
            }
           throw e;
        }
        catch(IOException|IOError ioe) {
            if (!initiateStop && running) {
                LOGGER.log(Level.WARNING, "Failure while reading input: ", ioe);
            }
            return null;
        }
    }

    public Shell getShell() {
        return shell;
    }

    public Terminal getTerminal() {
        return terminal;
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

            if(tmpOutput != null && !executorService.isShutdown())
                processManager.startNewProcess(consoleCallback, tmpOutput);

            inputProcessor.clearBufferAndDisplayPrompt();
        }
        else {
            if(running || hasInput()) {
                inputProcessor.resetBuffer();
                displayPrompt();
            }
        }
    }

    */
    /**
     * Get the current console buffer line (no masking)
     *
     * @return current buffer
     */
    /*
    public String getBuffer() {
        if(consoleBuffer.getBuffer() == null)
            return "";
        else
            return consoleBuffer.getBuffer().getLineNoMask();
    }

    private void startExecutor() {
        executorService.execute(this::executeLoop);
    }

    private void executeLoop() {
        try {
            while(!executorService.isShutdown()) {
                if (!processManager.hasForegroundProcess() && hasInput() && !controlledMode) {
                    execute();
                }
                Thread.sleep(10);
            }
        }
        catch (InterruptedException ie) {
            if (!initiateStop && running) {
                LOGGER.log(Level.WARNING, "Exception while executing:", ie);
            }
        }
        finally {
            if(!initiateStop || running)
                stop();
        }
    }

    private void execute() {
        try {
            String line = getInputLine();
            if (line != null) {
                processing = true;
                processOperationResult(line);
            }
            else {
                //not sure if we need this
                stop(); // is that correct ?
            }
        }
        catch (InterruptedException e) {
            if(!initiateStop && settings.logging())
                LOGGER.warning("Execution exception: "+e.getMessage());
        }
        finally {
            processing = false;
        }
    }

    public void execute(String command) throws InterruptedException {
        try {
            int start = 0;
            int end = command.length();
            while (start < end && Character.isWhitespace(command.charAt(start))) {
                start++;
            }
            while (end > start && Character.isWhitespace(command.charAt(end - 1))) {
                end--;
            }
            command = command.substring(start, end);
            ConsoleOperation output = createConsoleOperation(command);
            output = processInternalCommands(output);
            if (output.getBuffer() != null) {
                consoleCallback.execute(output);
            }
        }
        catch (IOException ioe) {
            if (settings.logging())
                LOGGER.severe("Stream failure: " + ioe);
        }
    }

    private void processOperationResult(String result) {
        try {
            //if the input length is 0 we should exit quickly
            //if we are stopping, dont print the prompt
            if(result.length() == 0 && running) {
                inputProcessor.clearBufferAndDisplayPrompt();
                return;
            }
            ConsoleOperation output = createConsoleOperation(result);
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
            if(settings.logging())
                LOGGER.severe("Stream failure: "+ioe);
        }
    }

    private ConsoleOperation createConsoleOperation(String result) throws IOException {
        if(result.startsWith(Parser.SPACE))
            result = Parser.trimInFront(result);

        if(settings.operatorParserEnabled())
            operations = ControlOperatorParser.findAllControlOperators(result);
        else {
            //if we do not parse operators just add ControlOperator.NONE
            operations = new ArrayList<>(1);
            operations.add(new ConsoleOperation(ControlOperator.NONE, result));
        }

        return parseOperations();
    }

    private void displayPrompt() {
        consoleBuffer.displayPrompt();
    }

    */
    /**
     * Clear a ansi terminal
     *
     * @throws IOException stream
     */
    /*
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
            if(settings.logging())
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
    */

    /**
     * Find the next ConsoleOutput based on operations
     *
     * @return next ConsoleOutput
     */
    /*
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
                            aeshContext().getCurrentWorkingDirectory().newInstance(
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
                    if(settings.logging())
                        LOGGER.info(settings.getName()+": syntax error near unexpected token '<'"+Config.getLineSeparator());
                    err().print(settings.getName() + ": syntax error near unexpected token '<'" + Config.getLineSeparator());
                    currentOperation = null;
                    output = new ConsoleOperation(ControlOperator.NONE, "");
                }
            }
            else {
                if(settings.logging())
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
            if(settings.aliasEnabled() &&
                    output.getBuffer().startsWith(InternalCommands.ALIAS.getCommand())) {
                String out = aliasManager.parseAlias(output.getBuffer().trim());
                if(out != null) {
                    out().print(out);
                    out().flush();
                }
                //empty output, will result
                return new ConsoleOperation(ControlOperator.NONE, null);
            }
            else if(settings.aliasEnabled() &&
                    output.getBuffer().startsWith(InternalCommands.UNALIAS.getCommand())) {
                String out = aliasManager.removeAlias(output.getBuffer().trim());
                if(out != null) {
                    out().print(out);
                    out().flush();
                }

                return new ConsoleOperation(ControlOperator.NONE, null);
            }
            else if(settings.exportEnabled() &&
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

        if(settings.exportEnabled()) {
            if(Parser.containsNonEscapedDollar(operation.getBuffer())) {
                operation = new ConsoleOperation(operation.getControlOperator(),
                        exportManager.getValue(operation.getBuffer()));
            }
        }

        if(settings.aliasEnabled()) {
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
            if(settings.logging())
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
            if(settings.logging())
                LOGGER.log(Level.SEVERE, "Saving file "+fileName+" to disk failed: ", e);
            err().println(e.getMessage());
            err().flush();
        }
        redirectPipeOutBuffer = new ByteArrayOutputStream();
        redirectPipeErrBuffer = new ByteArrayOutputStream();
    }

    private void readExecuteFile() {
        if(settings.executeFileAtStart() != null &&
                settings.executeFileAtStart().isLeaf()) {
            try {
                BufferedReader reader = new BufferedReader( new InputStreamReader(settings.executeFileAtStart().read()));
                String line;
                while( ( line = reader.readLine() ) != null ) {
                    if(line.length() > 0) {
                        LOGGER.info("executing: "+line);
                        execute(line);
                    }
                }
            }
            catch(IOException e) {
                LOGGER.log(Level.WARNING, "failed to read resource", e);
            }
            catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "execution got interrupted", e);
            }
        }
    }

    private class ConsoleShell implements org.jboss.aesh.console.Shell {

        private boolean mainBuffer = true;

        @Override
        public Terminal getTerminal() {
            return terminal;
        }

        @Override
        public void clear() {
            terminal.puts(Capability.clear_screen);
        }

        @Override
        public PrintStream out() {
            return Console.this.out();
        }

        @Override
        public PrintStream err() {
            return Console.this.err();
        }

        @Override
        public AeshStandardStream in() {
            return Console.this.in();
        }

        @Override
        public Size getSize() {
            return Console.this.getTerminalSize();
        }

        @Override
        public CursorPosition getCursor() {
            if(settings.isAnsiConsole() && Config.isOSPOSIXCompatible()) {
                try {
                    out().print(ANSI.CURSOR_ROW);
                    out().flush();
                    readingCursor = true;

                    return getActualCursor(cursorQueue.take());
                }
                catch (Exception e) {
                    if(settings.logging())
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
            if (getSize().isPositionWithinSize(position)
                    && terminal.puts(Capability.cursor_address,
                    position.getRow(),
                    position.getColumn())) {
                terminal.flush();
            }
        }

        @Override
        public void moveCursor(int rows, int columns) {
            CursorPosition cp = getCursor();
            cp.move(rows, columns);
            if (getSize().isPositionWithinSize(cp)) {
                setCursor(cp);
            }
        }

        @Override
        public boolean isMainBuffer() {
            return mainBuffer;
        }

        @Override
        public void enableAlternateBuffer() {
            if (mainBuffer && terminal.puts(Capability.enter_ca_mode)) {
                terminal.flush();
                mainBuffer = false;
            }
        }

        @Override
        public void enableMainBuffer() {
            if (!mainBuffer && terminal.puts(Capability.exit_ca_mode)) {
                terminal.flush();
                mainBuffer = true;
            }
        }

    }
    */
}
