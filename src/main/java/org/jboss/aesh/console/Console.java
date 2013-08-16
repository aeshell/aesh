/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.console.alias.Alias;
import org.jboss.aesh.console.alias.AliasCompletion;
import org.jboss.aesh.console.alias.AliasManager;
import org.jboss.aesh.console.helper.InterruptHandler;
import org.jboss.aesh.console.helper.Search;
import org.jboss.aesh.console.operator.ControlOperator;
import org.jboss.aesh.console.operator.ControlOperatorParser;
import org.jboss.aesh.console.operator.RedirectionCompletion;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.edit.EditMode;
import org.jboss.aesh.edit.Mode;
import org.jboss.aesh.edit.PasteManager;
import org.jboss.aesh.edit.ViEditMode;
import org.jboss.aesh.edit.actions.Action;
import org.jboss.aesh.edit.actions.EditAction;
import org.jboss.aesh.edit.actions.EditActionManager;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.edit.actions.PrevWordAction;
import org.jboss.aesh.edit.actions.Movement;
import org.jboss.aesh.history.FileHistory;
import org.jboss.aesh.history.History;
import org.jboss.aesh.history.InMemoryHistory;
import org.jboss.aesh.history.SearchDirection;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.terminal.Terminal;
import org.jboss.aesh.terminal.TerminalCharacter;
import org.jboss.aesh.terminal.TerminalSize;
import org.jboss.aesh.terminal.TerminalString;
import org.jboss.aesh.undo.UndoAction;
import org.jboss.aesh.undo.UndoManager;
import org.jboss.aesh.util.ANSI;
import org.jboss.aesh.util.FileUtils;
import org.jboss.aesh.util.LoggerUtil;
import org.jboss.aesh.util.Parser;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * A console reader.
 * Supports ansi terminals
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class Console {

    private Buffer buffer;

    private UndoManager undoManager;
    private PasteManager pasteManager;
    private EditMode editMode;
    private History history;
    private List<Completion> completionList;
    private Settings settings;
    private Search search;

    private Action prevAction = Action.EDIT;

    private ConsoleCommand command;
    private ConsoleCallback consoleCallback;

    private boolean displayCompletion = false;
    private boolean askDisplayCompletion = false;
    private volatile boolean running = false;
    private StringWriter redirectPipeOutBuffer;
    private StringWriter redirectPipeErrBuffer;
    private List<ConsoleOperation> operations;
    private ConsoleOperation currentOperation;
    private AliasManager aliasManager;

    private Logger logger = LoggerUtil.getLogger(getClass().getName());

    private Pattern endsWithBackslashPattern = Pattern.compile(".*\\s\\\\$");

    private ExecutorService executorService;

    //used to optimize text deletion
    private char[] resetLineAndSetCursorToStart =
            (ANSI.saveCursor()+ANSI.getStart()+"0G"+ANSI.getStart()+"2K").toCharArray();

    public Console(final Settings settings) {
        this.settings = settings;
        try {
            init();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //init a interrupt hook if its defined (by default its null)
        if(settings.hasInterruptHook()) {
            try {
                if(Class.forName("sun.misc.Signal") != null)
                    new InterruptHandler(this, settings.getInterruptHook()).initInterrupt();
            }
            catch(ClassNotFoundException e) {
                if(settings.isLogging())
                    logger.log(Level.WARNING,
                            "Class sun.misc.Signal was not found. No interrupt handling enabled.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void start() {
                try {
                    settings.getTerminal().reset();
                    if(settings.getQuitHandler() != null)
                        settings.getQuitHandler().quit();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

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
        if(executorService != null && !executorService.isShutdown()) {
            return;
        }
        if(settings.isLogging())
            logger.info("RESET");

        executorService = Executors.newSingleThreadExecutor();

         if(settings.doReadInputrc())
            settings = Config.parseInputrc(settings);

        settings = Config.readRuntimeProperties(settings);

        //init terminal
        settings.getTerminal().init(settings.getInputStream(), settings.getStdOut(), settings.getStdErr());

        editMode = settings.getEditMode();

        undoManager = new UndoManager();
        pasteManager = new PasteManager();
        buffer = new Buffer(settings.isAnsiConsole(), null);
        if(settings.isHistoryPersistent())
            history = new FileHistory(settings.getHistoryFile().getAbsolutePath(),
                    settings.getHistorySize());
        else
            history = new InMemoryHistory(settings.getHistorySize());


        completionList = new ArrayList<Completion>();
        //enable completion for redirection
        completionList.add(new RedirectionCompletion());

        //enable aliasing
        if(settings.isAliasEnabled()) {
            logger.info("enable aliasmanager with file: "+settings.getAliasFile());
            aliasManager = new AliasManager(settings.getAliasFile(), settings.doPersistAlias(), settings.getName());
            completionList.add(new AliasCompletion(aliasManager));
        }

        operations = new ArrayList<ConsoleOperation>();
        currentOperation = null;

        redirectPipeOutBuffer = new StringWriter();
        redirectPipeErrBuffer = new StringWriter();
        setPrompt(new Prompt(""));
    }

    /**
     *
     * @return get the terminal size
     */
    public TerminalSize getTerminalSize() {
        return getTerminal().getSize();
    }

    /**
     * Get the History object
     *
     * @return history
     */
    public History getHistory() {
        return history;
    }

    /**
     * Set the current prompt.
     *
     * @param prompt prompt
     * @throws IOException stream
     */
    public void setPrompt(Prompt prompt) throws IOException {
        if(!buffer.getPrompt().equals(prompt)) {
            buffer.updatePrompt(prompt);
            //only update the prompt if Console is running
            //set cursor position line.length
            if(running) {
                displayPrompt(prompt);
                if(buffer.getLine().length() > 0) {
                    pushToStdOut(buffer.getLine());
                    buffer.setCursor(buffer.getLine().length());
                }
            }
        }
    }

    public void setConsoleCallback(ConsoleCallback consoleCallback) {
        this.consoleCallback = consoleCallback;
    }

    public synchronized void start() throws IOException {
        if(running)
            throw new IllegalStateException("Not allowed to start the Console without stopping it first");
        if(consoleCallback == null)
            throw new IllegalStateException("Not possible to start the Console without setting ConsoleCallback");
        running = true;
        displayPrompt();
        startReader();
    }

    /**
     * Push text to the console, note that this will not update the internal
     * cursor position.
     *
     * @param input text
     * @throws IOException stream
     */
    public void pushToStdOut(String input) throws IOException {
        if(input != null && input.length() > 0) {
            //if redirection enabled, put it into a buffer
            if(currentOperation != null &&
                    ControlOperator.isRedirectionOut(currentOperation.getControlOperator()))
                redirectPipeOutBuffer.append(input);
            else
                getTerminal().writeToStdOut(input);
        }
    }

    /**
     * @see #pushToStdOut
     *
     * @param input chars
     * @throws IOException stream
     */
    public void pushToStdOut(char[] input) throws IOException {
        if(input != null && input.length > 0) {
            //if redirection enabled, put it into a buffer
            if(currentOperation != null &&
                    ControlOperator.isRedirectionOut(currentOperation.getControlOperator()))
                redirectPipeOutBuffer.write(input);
            else
                getTerminal().writeToStdOut(input);
        }
    }

    public void pushToStdOut(List<TerminalCharacter> termChars) throws IOException {
        if(termChars != null && termChars.size() > 0) {
            if(currentOperation != null &&
                    ControlOperator.isRedirectionOut(currentOperation.getControlOperator())) {
                StringBuilder builder = new StringBuilder();
                TerminalCharacter prev = null;
                for(TerminalCharacter c : termChars) {
                    if(prev == null)
                        builder.append(c.getAsString());
                    else
                        builder.append(c.getAsString(prev));
                    prev = c;
                }
                redirectPipeOutBuffer.append(builder.toString());
            }
            else
                getTerminal().writeToStdOut(termChars);
        }
    }

    public void pushToStdOut(TerminalString terminalString) throws IOException {
        if(terminalString != null) {
            if(currentOperation != null &&
                    ControlOperator.isRedirectionOut(currentOperation.getControlOperator())) {
                redirectPipeOutBuffer.append(terminalString.getAsString());
            }
            else
                getTerminal().writeStdOut(terminalString);
        }
    }

    public void pushToStdErr(String input) throws IOException {
        if(input != null && input.length() > 0) {
            if(currentOperation != null &&
                    ControlOperator.isRedirectionErr(currentOperation.getControlOperator()))
                redirectPipeErrBuffer.append(input);
            else
                getTerminal().writeToStdErr(input);
        }
    }

    public void pushToStdErr(char[] input) throws IOException {
        if(input != null && input.length > 0) {
            if(currentOperation != null &&
                    ControlOperator.isRedirectionErr(currentOperation.getControlOperator()))
                redirectPipeErrBuffer.write(input);
            else
                getTerminal().writeToStdErr(input);
        }
    }

    /**
     * Add a Completion to the completion list
     *
     * @param completion comp
     */
    public void addCompletion(Completion completion) {
        completionList.add(completion);
    }

    /**
     * Add a list of completions to the completion list
     *
     * @param completionList comps
     */
    public void addCompletions(List<Completion> completionList) {
        this.completionList.addAll(completionList);
    }

    /**
     * Stop the Console, close streams, and reset terminals.
     * WARNING: After this is called the Console object must be reset
     * before its used.
     * @throws IOException stream
     */
    public synchronized void stop() throws IOException {
        try {
            running = false;
            executorService.shutdown();
            //setting it to null to prevent uncertain state
            //settings.setInputStream(null);
            getTerminal().reset();
            //terminal = null;
            history.stop();
            if(aliasManager != null)
                aliasManager.persist();
            if(settings.isLogging())
                logger.info("Done stopping reading thread. Terminal is reset");
        }
        finally {
            settings.getInputStream().close();
            settings.getStdErr().close();
            settings.getStdOut().close();
            if(settings.isLogging())
                logger.info("Streams are closed");
        }
    }

    /**
     *
     * @return true if Console is set up and streams are open
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Used by ConsoleCommand to attach itself to the Console
     *
     * @param cc command
     * @throws IOException stream
     */
    protected void attachProcess(ConsoleCommand cc) throws IOException {
        command = cc;
    }

    /**
     * Remove the current running command from Console
     *
     * @throws IOException stream
     */
    private void detachProcess() throws IOException {
        command = null;
        displayPrompt();
    }

    /**
     * @return get the specified terminal
     */
    public Terminal getTerminal() {
        return settings.getTerminal();
    }

    /**
     * Read from the input stream, perform action according to mapped
     * operations/completions/etc
     */
    private void startReader() {
        Runnable reader = new Runnable() {
            @Override
            public void run() {
                while(!executorService.isShutdown()) {
                    read();
                }
            }
        };
        executorService.execute(reader);
    }

    private void read() {
        try {
            if(command != null && !command.isAttached()) {
                detachProcess();
            }

            int[] in = getTerminal().read(settings.isReadAhead());
            if(settings.isLogging()) {
                logger.info("GOT: "+ Arrays.toString(in));
            }
            //close thread, exit
            if (in[0] == -1) {
                executorService.shutdown();
                running = false;
                return;
            }

            //we have a paste that contain enter
            if(in.length > 1 && Key.ENTER.containKey(in) &&
                    //dont want ctrl chars in a paste
                    !Key.ESC.containKey(in)) {
                //split it up based on enter, atm we assuming its only chars
                // not special chars like delete, arrow up, etc
                List<String> lines = new ArrayList<String>();
                StringBuilder current = new StringBuilder();
                for(int i : in) {
                    if(Key.ENTER.getFirstValue() == i) {
                        lines.add(current.toString());
                        current = new StringBuilder();
                    }
                    else
                        current.append((char) i);
                }
                for(String line : lines) {
                    //if we're masking the cursor is always at first pos.
                    if(buffer.isMasking()) {
                       StringBuilder builder = new StringBuilder(buffer.getLineNoMask());
                        builder.append(line);
                        buffer.setLine(builder.toString());
                    }
                    else {
                        buffer.write(line);
                        pushToStdOut(line);
                        addToHistory(buffer.getLine());
                    }
                    printNewline();
                    processOperationResult(buffer.getLineNoMask());
                }
                //if we have some chars after the last enter, paste that is as well
                if(current.length() > 0) {
                    if(buffer.isMasking()) {
                        StringBuilder builder = new StringBuilder(buffer.getLineNoMask());
                        builder.append(current);
                        buffer.setLine(builder.toString());
                    }
                    else {
                        buffer.write(current.toString());
                        pushToStdOut(buffer.getLine());
                    }
                }
            }
            //a "normal" line, no paste etc...
            else {
                Operation operation = editMode.parseInput(in, buffer.getLine());
                operation.setInput(in);

                //if we have a command hooked in the input goes there
                if(command != null)
                    command.processOperation(operation);
                    //the input is parsed by æsh
                else {
                    String result = parseOperation(operation, buffer.getPrompt().getMask());
                    if(result != null)
                        processOperationResult(result);
                }
            }
        }
        catch (IOException ioe) {
            if(settings.isLogging())
                logger.severe("Stream failure: "+ioe);
        }
    }

    private void processOperationResult(String result) {
        try {
            // if the line ends with: \ we create a new line
            if(!buffer.getPrompt().isMasking() && endsWithBackslashPattern.matcher(result).find()) {
                buffer.setMultiLine(true);
                buffer.updateMultiLineBuffer();
                displayPrompt();
            }
            //normal line
            else {
                if(result.startsWith(" "))
                    result = Parser.trimInFront(result);

                if(settings.isOperatorParserEnabled())
                    operations = ControlOperatorParser.findAllControlOperators(result);
                else {
                    //if we do not parse operators just add ControlOperator.NONE
                    operations = new ArrayList<ConsoleOperation>(1);
                    operations.add(new ConsoleOperation(ControlOperator.NONE, result));
                }

                ConsoleOutput output = parseOperations();
                output = processInternalCommands(output);
                if(output.getBuffer() != null) {
                    //return output;
                    consoleCallback.readConsoleOutput(output);
                    //abort if the user have initiated stop
                    if(executorService.isShutdown())
                        return;

                    while(currentOperation != null) {
                        ConsoleOutput tmpOutput = parseCurrentOperation();
                        if(tmpOutput != null && !executorService.isShutdown())
                            consoleCallback.readConsoleOutput(tmpOutput);
                    }
                    search = null;
                    buffer.reset();
                    if(command == null) {
                        displayPrompt();
                    }
                }
                else {
                    buffer.reset();
                    displayPrompt();
                    search = null;
                }
            }
        }
        catch (IOException ioe) {
            if(settings.isLogging())
                logger.severe("Stream failure: "+ioe);
        }
    }

    /**
     * Parse the current operation
     *
     * @param operation operation
     * @param mask if set typed chars will be masked with this specified char
     * @return out
     * @throws IOException stream
     */
    private String parseOperation(Operation operation, Character mask) throws IOException {

        Action action = operation.getAction();

        if(askDisplayCompletion) {
            askDisplayCompletion = false;
            if('y' == (char) operation.getInput()[0]) {
                displayCompletion = true;
                complete();
            }
            //do not display complete, but make sure that the previous line
            // is restored correctly
            else {
                getTerminal().writeToStdOut(Config.getLineSeparator());
                displayPrompt();
                getTerminal().writeToStdOut(buffer.getLine());
                syncCursor();
            }
        }
        else if (action == Action.EDIT) {
            writeChars(operation.getInput(), mask);
        }
        //make sure that every action except delete is ignored when masking is enabled
        else if(mask != null) {
            if(action == Action.DELETE) {
                if(mask == 0)
                    deleteWithMaskEnabled();
                else
                    performAction(EditActionManager.parseAction(operation, buffer.getCursor(), buffer.length()));
            }
        }
        // For search movement is used a bit differently.
        // It only triggers what kind of search action thats performed
        else if(action == Action.SEARCH && !settings.isHistoryDisabled()) {
            if(search == null)
                search = new Search(operation, operation.getInput()[0]);
            else {
                search.setOperation(operation);
                search.setInput(operation.getInput()[0]);
            }
            doSearch(search);
            if(search.isFinished())
                return search.getResult();
        }
        else if(action == Action.MOVE || action == Action.DELETE ||
                action == Action.CHANGE || action == Action.YANK) {
            performAction(EditActionManager.parseAction(operation, buffer.getCursor(), buffer.length()));
        }
        else if(action == Action.ABORT) {
        }
        else if(action == Action.CASE) {
            addActionToUndoStack();
            changeCase();
        }
        else if(action == Action.COMPLETE) {
            complete();
        }
        else if(action == Action.EXIT) {
            if(settings.hasInterruptHook())
                settings.getInterruptHook().handleInterrupt(this);
            stop();
            System.exit(0);
        }
        else if(action == Action.HISTORY) {
            if(operation.getMovement() == Movement.NEXT)
                getHistoryElement(true);
            else if(operation.getMovement() == Movement.PREV)
                getHistoryElement(false);
        }

        else if(action == Action.UNDO) {
            undo();
        }
        else if(action == Action.PASTE_FROM_CLIPBOARD) {
            addActionToUndoStack();
            //paste();
        }
        else if(action == Action.PASTE) {
            if(operation.getMovement() == Movement.NEXT)
                doPaste(0, true);
            else
                doPaste(0, false);
        }
        else if(action == Action.CHANGE_EDITMODE) {
            changeEditMode(operation.getMovement());
        }
        else if(action == Action.CLEAR) {
            clear(true);
        }
        else if(action == Action.REPLACE) {
            replace(operation.getInput()[0]);
        }
        else if(action == Action.NO_ACTION) {
            //atm do nothing
        }

        //a hack to get history working
        if(action == Action.HISTORY && !settings.isHistoryDisabled())
            prevAction = action;

        //in the end we check for a newline
        if(action == Action.NEWLINE) {
            // clear the undo stack for each new line
            clearUndoStack();
            if(mask == null) {// dont push to history if masking
                //dont push lines that end with \ to history
                if(!endsWithBackslashPattern.matcher(buffer.getLine()).find()) {
                    if(buffer.isMultiLine())
                        addToHistory(buffer.getMultiLineBuffer()+buffer.getLine());
                    else
                        addToHistory(buffer.getLine());
                }
            }
            prevAction = Action.NEWLINE;
            //moveToEnd();
            printNewline(); // output newline
            if(buffer.isMultiLine())
                return buffer.getMultiLineBuffer() + buffer.getLineNoMask();
            else
                return buffer.getLineNoMask();
        }

        return null;

    }

    /**
     * Parse the Search object
     *
     * @param search search
     * @throws IOException stream
     */
    private void doSearch(Search search) throws IOException {

        switch (search.getOperation().getMovement()) {
            //init a previous doSearch
            case PREV:
                history.setSearchDirection(SearchDirection.REVERSE);
                search.setSearchTerm( new StringBuilder(buffer.getLine()));
                if (search.getSearchTerm().length() > 0) {
                    search.setResult( history.search(search.getSearchTerm().toString()));
                }
                break;

            case NEXT:
                history.setSearchDirection(SearchDirection.FORWARD);
                search.setSearchTerm(new StringBuilder(buffer.getLine()));
                if (search.getSearchTerm().length() > 0) {
                    search.setResult( history.search(search.getSearchTerm().toString()));
                }
                break;

            case PREV_WORD:
                history.setSearchDirection(SearchDirection.REVERSE);
                if (search.getSearchTerm().length() > 0)
                    search.setResult( history.search(search.getSearchTerm().toString()));
                break;

            case NEXT_WORD:
                history.setSearchDirection(SearchDirection.FORWARD);
                if(search.getSearchTerm().length() > 0)
                    search.setResult(history.search(search.getSearchTerm().toString()));
                break;

            case PREV_BIG_WORD:
                if (search.getSearchTerm().length() > 0)
                    search.getSearchTerm().deleteCharAt(search.getSearchTerm().length() - 1);
                break;
            // new doSearch input, append to doSearch
            case ALL:
                search.getSearchTerm().appendCodePoint(search.getInput());
                //check if the new searchTerm will find anything
                String tmpResult = history.search(search.getSearchTerm().toString());
                if(tmpResult == null) {
                    search.getSearchTerm().deleteCharAt(search.getSearchTerm().length()-1);
                }
                else {
                    search.setResult(tmpResult);
                }
                break;
            // pressed enter, ending the doSearch
            case END:
                // Set buffer to the found string.
                if (search.getResult() != null) {
                    moveCursor(-buffer.getCursor());
                    setBufferLine(search.getResult());
                    redrawLine();
                    printNewline();
                    search.setResult( buffer.getLineNoMask());
                    search.setFinished(true);
                    return;
                }
                else {
                    moveCursor(-buffer.getCursor());
                    setBufferLine("");
                    redrawLine();
                }
                break;

            //exiting doSearch (with esc)
            case NEXT_BIG_WORD:
                if(search.getResult() != null) {
                    moveCursor(-buffer.getCursor());
                    setBufferLine(search.getResult());
                    search.setResult(null);
                }
                else {
                    moveCursor(-buffer.getCursor());
                    setBufferLine("");
                }
                //redrawLine();
                break;
        }
        // if we're still in doSearch mode, print the doSearch status
        if (editMode.getCurrentAction() == Action.SEARCH) {
            if (search.getSearchTerm().length() == 0) {
                if(search.getResult() != null)
                    printSearch("", search.getResult());
                else
                    printSearch("", "");
            }
            else {
                if (search.getResult() == null) {
                    //beep();
                }
                else {
                    printSearch(search.getSearchTerm().toString(),
                            search.getResult());
                }
            }
        }
        // otherwise, restore the line
        else {
            redrawLine();
            getTerminal().writeToStdOut(Buffer.printAnsi((buffer.getPrompt().getLength() + 1) + "G"));
        }
    }

    /**
     * If movement == PREV setting VI mode
     * if movement == NEXT setting EMACS mode
     *
     * @param movement specifying vi/emacs mode
     */
    private void changeEditMode(Movement movement) {
        if(editMode.getMode() == Mode.EMACS && movement == Movement.PREV) {
            settings.switchMode();
            settings.resetEditMode();
        }
        else if(editMode.getMode() == Mode.VI && movement == Movement.NEXT) {
            settings.switchMode();
            settings.resetEditMode();
        }
        editMode = settings.getEditMode();
    }

    private void getHistoryElement(boolean first) throws IOException {
        if(settings.isHistoryDisabled())
            return;
        // first add current line to history
        if(prevAction == Action.NEWLINE) {
            history.setCurrent(buffer.getLine());
        }
        //get next
        String fromHistory;
        if(first)
            fromHistory = history.getNextFetch();
        // get previous
        else
           fromHistory = history.getPreviousFetch();

        if(fromHistory != null) {
            setBufferLine(fromHistory);
            moveCursor(-buffer.getCursor()+buffer.length());
            redrawLine();
        }
        prevAction = Action.HISTORY;
    }

    private void setBufferLine(String newLine) throws IOException {
        //must make sure that there are enough space for the
        // line thats about to be injected
        if((newLine.length()+buffer.getPrompt().getLength()) >= getTerminal().getSize().getWidth() &&
                newLine.length() >= buffer.getLine().length()) {
            int currentRow = getTerminal().getCursor().getRow();
            if(currentRow > -1) {
                int cursorRow = buffer.getCursorWithPrompt() / getTerminal().getSize().getWidth();
                if(currentRow + (newLine.length() / getTerminal().getSize().getWidth()) - cursorRow >= getTerminal().getSize().getHeight()) {
                    int numNewRows = currentRow + ((newLine.length()+buffer.getPrompt().getLength()) / getTerminal().getSize().getWidth()) - cursorRow - getTerminal().getSize().getHeight();
                    //if the line is exactly equal to termWidth we need to add another row
                    if((newLine.length()+buffer.getPrompt().getLength()) % getTerminal().getSize().getWidth() == 0)
                        numNewRows++;
                    if(numNewRows > 0) {
                        if(settings.isLogging()) {
                            int totalRows = (newLine.length()+buffer.getPrompt().getLength()) / getTerminal().getSize().getWidth() +1;
                            logger.info("ADDING "+numNewRows+", totalRows:"+totalRows+
                                    ", currentRow:"+currentRow+", cursorRow:"+cursorRow);
                        }
                        getTerminal().writeToStdOut(Buffer.printAnsi(numNewRows + "S"));
                        getTerminal().writeToStdOut(Buffer.printAnsi(numNewRows + "A"));
                    }
                }
            }
        }
        buffer.setLine(newLine);
    }

    private void insertBufferLine(String insert, int position) throws IOException {
        if((insert.length()+buffer.totalLength()) >= getTerminal().getSize().getWidth()) { //&&
                //(insert.length()+buffer.totalLength()) > buffer.getLine().length()) {
            int currentRow = getTerminal().getCursor().getRow();
            if(currentRow > -1) {
                int newLine = insert.length()+buffer.totalLength();
                int cursorRow = buffer.getCursorWithPrompt() / getTerminal().getSize().getWidth();
                if(currentRow + (newLine / getTerminal().getSize().getWidth()) - cursorRow >= getTerminal().getSize().getHeight()) {
                    int numNewRows = currentRow + (newLine / getTerminal().getSize().getWidth()) - cursorRow - getTerminal().getSize().getHeight();
                    //if the line is exactly equal to termWidth we need to add another row
                    if((insert.length()+buffer.totalLength()) % getTerminal().getSize().getWidth() == 0)
                        numNewRows++;
                    if(numNewRows > 0) {
                        getTerminal().writeToStdOut(Buffer.printAnsi(numNewRows + "S"));
                        getTerminal().writeToStdOut(Buffer.printAnsi(numNewRows + "A"));
                    }
                }
            }
        }
        buffer.insert(position, insert);
    }

    private void addToHistory(String line) {
        if(!settings.isHistoryDisabled())
            history.push(line);
    }

    private void displayPrompt() throws IOException {
        displayPrompt(buffer.getPrompt());
    }

    private void displayPrompt(Prompt prompt) throws IOException {
        if(prompt.hasANSI()) {
            getTerminal().writeToStdOut(ANSI.getStart()+"0G"+ANSI.getStart()+"2K");
            getTerminal().writeToStdOut(prompt.getANSI());
        }
        else
            getTerminal().writeToStdOut(ANSI.getStart()+"0G"+ANSI.getStart()+"2K"+prompt.getPromptAsString());
    }

    private void writeChars(int[] chars, Character mask) throws IOException {
        for(int c : chars)
            writeChar(c,mask);
    }

    private void writeChar(int c, Character mask) throws IOException {

        buffer.write((char) c);
        //if mask is set and not set to 0 (nullvalue) we write out
        //the masked char. if masked is set to 0 we write nothing
        if(mask != null) {
            if(mask != 0)
                getTerminal().writeToStdOut(mask);
        }
        else {
            getTerminal().writeToStdOut((char) c);
        }

        // add a 'fake' new line when inserting at the edge of terminal
        if(buffer.getCursorWithPrompt() > getTerminal().getSize().getWidth() &&
                buffer.getCursorWithPrompt() % getTerminal().getSize().getWidth() == 1) {
            getTerminal().writeToStdOut((char) 32);
            getTerminal().writeToStdOut((char) 13);
        }

        // if we insert somewhere other than the end of the line we need to redraw from cursor
        if(buffer.getCursor() < buffer.length()) {
            //check if we just started a new line, if we did we need to make sure that we add one
            if(buffer.totalLength() > getTerminal().getSize().getWidth() &&
                    (buffer.totalLength()-1) % getTerminal().getSize().getWidth() == 1) {
                int ansiCurrentRow = getTerminal().getCursor().getRow();
                int currentRow = (buffer.getCursorWithPrompt() / getTerminal().getSize().getWidth());
                if(currentRow > 0 && buffer.getCursorWithPrompt() % getTerminal().getSize().getWidth() == 0)
                    currentRow--;

                int totalRows = buffer.totalLength() / getTerminal().getSize().getWidth();
                if(totalRows > 0 && buffer.totalLength() % getTerminal().getSize().getWidth() == 0)
                    totalRows--;

                if(ansiCurrentRow+(totalRows-currentRow) > getTerminal().getSize().getHeight()) {
                    getTerminal().writeToStdOut(Buffer.printAnsi("1S")); //adding a line
                    getTerminal().writeToStdOut(Buffer.printAnsi("1A")); // moving up a line
                }
            }
            redrawLine();
        }
    }

    /**
     * A simple hack to ensure that delete works when masking is enabled and
     * the mask character is set to null (empty).
     * The only operation that will work when the mask character is set to 0 is
     * delete.
     *
     * @throws IOException
     */
    private void deleteWithMaskEnabled() throws IOException {
        if(buffer.getLineNoMask().length() > 0)
            buffer.delete(buffer.getLineNoMask().length()-1, buffer.getLineNoMask().length());
    }

    /**
     * Perform the designated action created by an event
     *
     * @param action console action
     * @return true if nothing goes wrong
     * @throws IOException stream
     */
    private boolean performAction(EditAction action) throws IOException {
        action.doAction(buffer.getLine());
        if(action.getAction() == Action.MOVE) {
            moveCursor((action.getEnd() - action.getStart()));
            return true;
        }
        else if(action.getAction() == Action.DELETE || action.getAction() == Action.CHANGE) {
            //first trigger undo action
            addActionToUndoStack();

            if(action.getEnd() > action.getStart()) {
                // only if start != cursor we need to move it
                if(action.getStart() != buffer.getCursor()) {
                    moveCursor(action.getStart() - buffer.getCursor());
                }
                addToPaste(buffer.getLine().substring(action.getStart(), action.getEnd()));
                buffer.delete(action.getStart(), action.getEnd());
            }
            else {
                addToPaste(buffer.getLine().substring(action.getEnd(), action.getStart()));
                buffer.delete(action.getEnd(), action.getStart());
                moveCursor((action.getEnd() - action.getStart()));
            }

            if(editMode.getMode() == Mode.VI && buffer.getCursor() == buffer.length()) {
                if(!((ViEditMode) editMode).isInEditMode())
                    moveCursor(-1);
            }
            redrawLine();
        }
        else if(action.getAction() == Action.YANK) {
            if(action.getEnd() > action.getStart()) {
                addToPaste(buffer.getLine().substring(action.getStart(), action.getEnd()));
            }
            else {
                addToPaste(buffer.getLine().substring(action.getEnd(), action.getStart()));
            }
        }

        return true;
    }

    /**
     * Add current text and cursor position to the undo stack
     *
     * @throws IOException if getCursorPosition() fails
     */
    private void addActionToUndoStack() throws IOException {
        UndoAction ua = new UndoAction(buffer.getCursor(), buffer.getLine());
        undoManager.addUndo(ua);
    }


    private void clearUndoStack() {
        undoManager.clear();
    }

    private void addToPaste(String buffer) {
        pasteManager.addText(new StringBuilder(buffer));
    }

    /**
     * Paste previous yanked word/char either before or on the cursor position
     *
     * @param index which yank index
     * @param before cursor
     * @return true if everything went as expected
     * @throws IOException if redraw failed
     */
    private boolean doPaste(int index, boolean before) throws IOException {
        StringBuilder pasteBuffer = pasteManager.get(index);
        if(pasteBuffer == null)
            return false;

        addActionToUndoStack();
        if(before || buffer.getCursor() >= buffer.getLine().length()) {
            insertBufferLine(pasteBuffer.toString(), buffer.getCursor());
            redrawLine();
        }
        else {
            insertBufferLine(pasteBuffer.toString(), buffer.getCursor()+1);
            redrawLine();
            //move cursor one char
            moveCursor(1);
        }
        return true;
    }

    public final void moveCursor(final int where) throws IOException {
        if(editMode.getMode() == Mode.VI &&
                (editMode.getCurrentAction() == Action.MOVE ||
                        editMode.getCurrentAction() == Action.DELETE)) {

            getTerminal().writeToStdOut(buffer.move(where, getTerminal().getSize().getWidth(), true));
        }
        else {
            getTerminal().writeToStdOut(buffer.move(where, getTerminal().getSize().getWidth()));
        }
    }

    private void redrawLine() throws IOException {
        drawLine(buffer.getPrompt().getPromptAsString() + buffer.getLine());
    }

    private void drawLine(String line) throws IOException {
       //need to clear more than one line
        if(line.length() > getTerminal().getSize().getWidth() ||
                (line.length()+ Math.abs(buffer.getDelta()) > getTerminal().getSize().getWidth())) {

            int currentRow = 0;
            if(buffer.getCursorWithPrompt() > 0)
                currentRow = buffer.getCursorWithPrompt() / getTerminal().getSize().getWidth();
            if(currentRow > 0 && buffer.getCursorWithPrompt() % getTerminal().getSize().getWidth() == 0)
                currentRow--;

            if(settings.isLogging()) {
                logger.info("actual position: "+getTerminal().getCursor());
                logger.info("currentRow:"+currentRow+", cursorWithPrompt:"+buffer.getCursorWithPrompt()
                        +", width:"+getTerminal().getSize().getWidth()+", height:"+getTerminal().getSize().getHeight()
                        +", delta:"+buffer.getDelta() +", buffer:"+buffer.getLine());
            }

            getTerminal().writeToStdOut(ANSI.saveCursor()); //save cursor

            if(currentRow > 0)
                for(int i=0; i<currentRow; i++)
                    getTerminal().writeToStdOut(Buffer.printAnsi("A")); //move to top

            getTerminal().writeToStdOut(Buffer.printAnsi("0G")); //clear

            //getTerminal().writeToStdOut(line);
            if(!buffer.isPromptDisabled())
                displayPrompt();
            getTerminal().writeToStdOut(buffer.getLine());
            //if the current line.length < compared to previous we add spaces to the end
            // to overwrite the old chars (wtb a better way of doing this)
            if(buffer.getDelta() < 0) {
                StringBuilder sb = new StringBuilder();
                for(int i=0; i > buffer.getDelta(); i--)
                    sb.append(' ');
                getTerminal().writeToStdOut(sb.toString());
            }

            // move cursor to saved pos
            getTerminal().writeToStdOut(ANSI.restoreCursor());
        }
        // only clear the current line
        else {
            //most deletions are backspace from the end of the line so we've
            //optimize that like this.
            //NOTE: this doesnt work with history, need to find a better solution
            /*
            if(buffer.getDelta() == -1 && buffer.getCursor() >= buffer.length()) {
                getTerminal().writeToStdOut(' ' + ANSI.getStart() + "1D"); //move cursor to left
            }
            */
            //save cursor, move the cursor to the beginning, reset line
            getTerminal().writeToStdOut(resetLineAndSetCursorToStart);
            if(!buffer.isPromptDisabled())
                displayPrompt();
            //write line and restore cursor
            getTerminal().writeToStdOut(buffer.getLine()+ANSI.restoreCursor());
        }
    }

    private void printSearch(String searchTerm, String result) throws IOException {
        //cursor should be placed at the index of searchTerm
        int cursor = result.indexOf(searchTerm);

        StringBuilder out;
        if(history.getSearchDirection() == SearchDirection.REVERSE)
            out = new StringBuilder("(reverse-i-search) `");
        else
            out = new StringBuilder("(forward-i-search) `");
        out.append(searchTerm).append("': ");
        cursor += out.length();
        out.append(result);
        buffer.disablePrompt(true);
        moveCursor(-buffer.getCursor());
        getTerminal().writeToStdOut(ANSI.moveCursorToBeginningOfLine());
        getTerminal().writeToStdOut(ANSI.getStart() + "2K");
        setBufferLine(out.toString());
        moveCursor(cursor);
        drawLine(buffer.getLine());
        buffer.disablePrompt(false);
    }

    /**
     * Insert a newline
     *
     * @throws java.io.IOException stream
     */
    private void printNewline() throws IOException {
        moveCursor(buffer.totalLength());
        getTerminal().writeToStdOut(Config.getLineSeparator());
    }

      /**
     * Switch case if the character is a letter
     *
     * @throws java.io.IOException stream
     */
    private void changeCase() throws IOException {
        if(buffer.changeCase()) {
           moveCursor(1);
            redrawLine();
        }
    }

    /**
     * Perform an undo
     *
     * @throws IOException if redraw fails
     */
    private void undo() throws IOException {
        UndoAction ua = undoManager.getNext();
        if(ua != null) {
            setBufferLine(ua.getBuffer());
            redrawLine();
            moveCursor(ua.getCursorPosition() - buffer.getCursor());
        }
    }

    /**
     * Display possible completions.
     * 1. Find all possible completions
     * 2. If we find only one, display it.
     * 3. If we find more than one, display them,
     *    but not more than 100 at once
     *
     * @throws IOException stream
     */
    private void complete() throws IOException {
        if(completionList.size() < 1)
            return;

        List<CompleteOperation> possibleCompletions = new ArrayList<CompleteOperation>();
        int pipeLinePos = 0;
        boolean redirect = false;
        if(ControlOperatorParser.doStringContainPipeline(buffer.getLine())) {
            pipeLinePos =  ControlOperatorParser.findLastPipelinePositionBeforeCursor(buffer.getLine(), buffer.getCursor());
        }
        if(ControlOperatorParser.findLastRedirectionPositionBeforeCursor(buffer.getLine(), buffer.getCursor()) > pipeLinePos) {
            pipeLinePos = 0;
            redirect = true;

        }

        for(Completion completion : completionList) {
            if(redirect && !completion.getClass().equals(RedirectionCompletion.class)) {
                break;
            }
            CompleteOperation co;
            if(pipeLinePos > 0) {
                co = findAliases(buffer.getLine().substring(pipeLinePos, buffer.getCursor()), buffer.getCursor() - pipeLinePos);
            }
            else {
                co = findAliases(buffer.getLine(), buffer.getCursor());
            }

            completion.complete(co);

            if(co.getCompletionCandidates() != null && co.getCompletionCandidates().size() > 0)
                possibleCompletions.add(co);
        }

        if(settings.isLogging())
            logger.info("Found completions: "+possibleCompletions);

        // not hits, just return (perhaps we should beep?)
        if(possibleCompletions.size() < 1) {
            //do nothing atm
        }
        // only one hit, do a completion
        else if(possibleCompletions.size() == 1 &&
                possibleCompletions.get(0).getCompletionCandidates().size() == 1) {
            //some formatted completions might not be valid and shouldnt be displayed
            displayCompletion(possibleCompletions.get(0).getCompletionCandidates().get(0),
                    possibleCompletions.get(0).getFormattedCompletionCandidates().get(0),
                    possibleCompletions.get(0).hasAppendSeparator(),
                    possibleCompletions.get(0).getSeparator());
        }
        // more than one hit...
        else {

            String startsWith = Parser.findStartsWithOperation(possibleCompletions);

            if(startsWith.length() > 0)
                displayCompletion("", startsWith, false, possibleCompletions.get(0).getSeparator());
                // display all
                // check size
            else {
                List<String> completions = new ArrayList<String>();
                for(CompleteOperation co : possibleCompletions)
                    completions.addAll(co.getCompletionCandidates());

                if(completions.size() > 100) {
                    if(displayCompletion) {
                        displayCompletions(completions);
                        displayCompletion = false;
                    }
                    else {
                        askDisplayCompletion = true;
                        getTerminal().writeToStdOut(Config.getLineSeparator() + "Display all " + completions.size() + " possibilities? (y or n)");
                    }
                }
                // display all
                else {
                    displayCompletions(completions);
                }
            }
        }
    }

    /**
     * Display the completion string in the terminal.
     * If !completion.startsWith(buffer.getLine()) the completion will be added to the line,
     * else it will replace whats at the buffer line.
     *
     * @param fullCompletion the while completion
     * @param completion partial completion
     * @param appendSpace if its an actual complete
     * @throws java.io.IOException stream
     */
    private void displayCompletion(String fullCompletion, String completion,
                                   boolean appendSpace, char separator) throws IOException {
        if(completion.startsWith(buffer.getLine())) {
            performAction(new PrevWordAction(buffer.getCursor(), Action.DELETE));
            buffer.write(completion);
            getTerminal().writeToStdOut(completion);

            //only append space if its an actual complete, not a partial
        }
        else {
            buffer.write(completion);
            getTerminal().writeToStdOut(completion);
        }
        if(appendSpace) { // && fullCompletion.startsWith(buffer.getLine())) {
            buffer.write(separator);
            getTerminal().writeToStdOut(separator);
        }

        redrawLine();
    }

    /**
     * Display all possible completions
     *
     * @param completions all completion items
     * @throws IOException stream
     */
    private void displayCompletions(List<String> completions) throws IOException {
        //printNewline reset cursor pos, so we need to store it
        int oldCursorPos = buffer.getCursor();
        printNewline();
        buffer.setCursor(oldCursorPos);
        getTerminal().writeToStdOut(Parser.formatDisplayList(completions,
                getTerminal().getSize().getHeight(), getTerminal().getSize().getWidth()));
        displayPrompt();
        getTerminal().writeToStdOut(buffer.getLine());
        //if we do a complete and the cursor is not at the end of the
        //buffer we need to move it to the correct place
        syncCursor();
    }

    private void syncCursor() throws IOException {
        if(buffer.getCursor() != buffer.getLine().length())
            getTerminal().writeToStdOut(Buffer.printAnsi((
                    Math.abs(buffer.getCursor() -
                            buffer.getLine().length()) + "D")));

    }

    private void replace(int rChar) throws IOException {
        addActionToUndoStack();
        buffer.replaceChar((char) rChar);
        redrawLine();
    }

    /**
     * Clear a ansi terminal
     *
     * @throws IOException stream
     */
    public void clear() throws IOException {
       clear(false);
    }

    /**
     * Clear an ansi terminal.
     * Set includeBuffer to true if the current buffer should be
     * printed again after clear.
     *
     * @param includeBuffer if true include the current buffer line
     * @throws IOException stream
     */
    public void clear(boolean includeBuffer) throws IOException {
        //(windows fix)
        if(!Config.isOSPOSIXCompatible())
            printNewline();
        //first clear console
        getTerminal().writeToStdOut(ANSI.clearScreen());
        //move cursor to correct position
        getTerminal().writeToStdOut(Buffer.printAnsi("1;1H"));
        //then writeToStdOut prompt
        if(includeBuffer) {
            displayPrompt();
            getTerminal().writeToStdOut(buffer.getLine());
        }
    }

    private ConsoleOutput parseCurrentOperation() throws IOException {
        if(currentOperation.getControlOperator() == ControlOperator.OVERWRITE_OUT
                || currentOperation.getControlOperator() == ControlOperator.OVERWRITE_ERR
                || currentOperation.getControlOperator() == ControlOperator.APPEND_OUT
                || currentOperation.getControlOperator() == ControlOperator.APPEND_ERR
                || currentOperation.getControlOperator() == ControlOperator.OVERWRITE_OUT_AND_ERR) {

            ConsoleOperation nextOperation = operations.remove(0);
            persistRedirection(nextOperation.getBuffer(), currentOperation.getControlOperator());
            if(nextOperation.getControlOperator() == ControlOperator.NONE) {
                redirectPipeErrBuffer = new StringWriter();
                redirectPipeOutBuffer = new StringWriter();
                currentOperation = null;
                return null;
            }
            else {
                redirectPipeErrBuffer = new StringWriter();
                redirectPipeOutBuffer = new StringWriter();
                currentOperation = nextOperation;
                return parseCurrentOperation();
            }
        }
        else if(currentOperation.getControlOperator() == ControlOperator.PIPE
                || currentOperation.getControlOperator() == ControlOperator.PIPE_OUT_AND_ERR) {
            return parseOperations();
        }
        //this should never happen (all overwrite_in should be parsed in parseOperations())
        else if(currentOperation.getControlOperator() == ControlOperator.OVERWRITE_IN) {
            if(settings.isLogging())
                logger.info(settings.getName()+": syntax error while reading token: \'<\'");
            pushToStdErr(settings.getName()+": syntax error while reading token: \'<\'");
            return null;
        }
        //ControlOperator.NONE
        else {
            //do nothing
            return null;
        }
    }

    /**
     * Find the next ConsoleOutput based on operations
     *
     * @return next ConsoleOutput
     */
    private ConsoleOutput parseOperations() throws IOException {

        ConsoleOutput output = null;
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
                output = new ConsoleOutput(op,
                        redirectPipeOutBuffer.toString(), redirectPipeErrBuffer.toString());
            }
        }
        else if(op.getControlOperator() == ControlOperator.OVERWRITE_IN) {
            //1. we need to find next operation
            //2. use the buffer from the next operation to read file to buffer
            //3. switch redirection operation with next one
            if(operations.size() > 0) {
                ConsoleOperation nextOperation = operations.remove(0);
                if( nextOperation.getBuffer().length() > 0) {
                    List<String> files = Parser.findAllWords(nextOperation.getBuffer());
                    currentOperation = new ConsoleOperation(nextOperation.getControlOperator(), op.getBuffer());
                    try {
                        output = new ConsoleOutput(new ConsoleOperation(nextOperation.getControlOperator(),op.getBuffer()),
                                FileUtils.readFile(new File(Parser.switchEscapedSpacesToSpacesInWord(files.get(0)))),
                                redirectPipeErrBuffer.toString());
                    }
                    //if we get any io error reading the file:
                    catch (IOException ioe) {
                        pushToStdErr(settings.getName()+": "+ioe.getMessage()+Config.getLineSeparator());
                        currentOperation = null;
                        output = new ConsoleOutput(new ConsoleOperation(ControlOperator.NONE, ""));
                    }
                }
                else {
                    if(settings.isLogging())
                        logger.info(settings.getName()+": syntax error near unexpected token '<'"+Config.getLineSeparator());
                    pushToStdErr(settings.getName()+": syntax error near unexpected token '<'"+Config.getLineSeparator());
                    currentOperation = null;
                    output = new ConsoleOutput(new ConsoleOperation(ControlOperator.NONE, ""));
                }
            }
            else {
                if(settings.isLogging())
                    logger.info(settings.getName()+": syntax error near unexpected token 'newline'"+Config.getLineSeparator());
                pushToStdErr(settings.getName()+": syntax error near unexpected token 'newline'"+Config.getLineSeparator());
                currentOperation = null;
                output = new ConsoleOutput(new ConsoleOperation(ControlOperator.NONE, ""));
            }
        }
        else {
            currentOperation = null;
            output = new ConsoleOutput(op,
                    redirectPipeOutBuffer.toString(), redirectPipeErrBuffer.toString());
        }

        if(redirectPipeOutBuffer.toString().length() > 0)
            redirectPipeOutBuffer = new StringWriter();
        if(redirectPipeErrBuffer.toString().length() > 0)
            redirectPipeErrBuffer = new StringWriter();

        return findAliases(output);
    }

    private ConsoleOutput processInternalCommands(ConsoleOutput output) throws IOException {
        if(output.getBuffer() != null) {
            if(settings.isAliasEnabled() &&
                    output.getBuffer().startsWith(InternalCommands.ALIAS.getCommand())) {
                String out = aliasManager.parseAlias(output.getBuffer().trim());
                if(out != null) {
                    pushToStdOut(out);
                }
                //empty output, will result
                return new ConsoleOutput(new ConsoleOperation(ControlOperator.NONE, null));
            }
            else if(settings.isAliasEnabled() &&
                    output.getBuffer().startsWith(InternalCommands.UNALIAS.getCommand())) {
                String out = aliasManager.removeAlias(output.getBuffer().trim());
                if(out != null)
                    pushToStdOut(out);

                return new ConsoleOutput(new ConsoleOperation(ControlOperator.NONE, null));
            }
        }
        return output;
    }

    private ConsoleOutput findAliases(ConsoleOutput operation) {
        if(settings.isAliasEnabled()) {
            String command = Parser.findFirstWord(operation.getBuffer());
            Alias alias = aliasManager.getAlias(command);

            if(alias != null) {
                operation.setConsoleOperation( new ConsoleOperation(operation.getControlOperator(),
                        alias.getValue() + operation.getBuffer().substring(command.length())));
            }
        }
        return operation;
    }

    private CompleteOperation findAliases(String buffer, int cursor) {
        if(settings.isAliasEnabled()) {
            String command = Parser.findFirstWord(buffer);
            Alias alias = aliasManager.getAlias(command);
            if(alias != null) {
                return new CompleteOperation( alias.getValue()+buffer.substring(command.length()),
                        cursor+(alias.getValue().length()-command.length()));
            }
        }

        return new CompleteOperation(buffer, cursor);
    }

    private void persistRedirection(String fileName, ControlOperator redirection) throws IOException {
        List<String> fileNames = Parser.findAllWords(fileName);
        if(fileNames.size() > 1) {
            if(settings.isLogging())
                logger.info(settings.getName()+": can't redirect to more than one file."+Config.getLineSeparator());
            pushToStdErr(settings.getName()+": can't redirect to more than one file."+Config.getLineSeparator());
            return;
        }
        //this is safe since we check that buffer do contain text earlier
        else {
            fileName = fileNames.get(0);
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
                logger.log(Level.SEVERE, "Saving file "+fileName+" to disk failed: ", e);
            pushToStdErr(e.getMessage());
        }
        redirectPipeOutBuffer = new StringWriter();
        redirectPipeErrBuffer = new StringWriter();
    }
    
    public PrintWriter getStdOut() {
        //if redirection enabled, put it into a buffer
        if(currentOperation != null &&
                ControlOperator.isRedirectionOut(currentOperation.getControlOperator())) {
        	return new PrintWriter(redirectPipeOutBuffer, true);
        } else {
        	return getTerminal().getStdOut();
        }
    }
    
    public PrintWriter getStdErr(){ 
        //if redirection enabled, put it into a buffer
        if(currentOperation != null &&
                ControlOperator.isRedirectionOut(currentOperation.getControlOperator())) {
        	return new PrintWriter(redirectPipeErrBuffer, true);
        } else {
        	return getTerminal().getStdErr();
        }
    }

}
