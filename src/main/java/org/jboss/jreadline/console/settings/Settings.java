/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
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
package org.jboss.jreadline.console.settings;

import org.jboss.jreadline.console.Config;
import org.jboss.jreadline.edit.*;
import org.jboss.jreadline.terminal.POSIXTerminal;
import org.jboss.jreadline.terminal.Terminal;
import org.jboss.jreadline.terminal.WindowsTerminal;

import java.io.*;

/**
 * Settings object that is parsed when Console is initialized.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class Settings {

    private Mode editMode = Mode.EMACS;
    private File historyFile;
    private int historySize = 500; //default is 500
    private String bellStyle;
    private boolean ansiConsole = true; //TODO: need to implement this correctly
    private InputStream inputStream;
    private OutputStream outputStream;
    private Terminal terminal;
    private boolean readInputrc = true;
    private File inputrc;
    private boolean isLogging = true;
    private String logFile;
    private boolean disableCompletion = true;

    private static final Settings INSTANCE = new Settings();

    private Settings() {
    }

    public static Settings getInstance() {
        return INSTANCE;
    }
    /**
     * Either EMACS or VI mode.
     * EMACS is default if not set
     *
     * @return editing mode
     */
    public Mode getEditMode() {
        return editMode;
    }

    public void setEditMode(Mode editMode) {
        this.editMode = editMode;
    }

    public EditMode getFullEditMode() {
        if(Config.isOSPOSIXCompatible()) {
            if(getEditMode() == Mode.EMACS)
                return new EmacsEditMode(KeyOperationManager.generatePOSIXEmacsMode());
            else
                return new ViEditMode(KeyOperationManager.generatePOSIXViMode());
        }
        else {
            if(getEditMode() == Mode.EMACS)
                return new EmacsEditMode(KeyOperationManager.generateWindowsEmacsMode());
            else
                return new ViEditMode(KeyOperationManager.generateWindowsViMode());
        }
    }

    /**
     * If not set the history file will be:
     * $HOME/.jreadline_history
     *
     * @return history file
     */
    public File getHistoryFile() {
        if(historyFile == null) {
            return new File(System.getProperty("user.home")+
                    Config.getPathSeparator()+".jreadline_history");
        }
        else
            return historyFile;
    }

    public void setHistoryFile(File historyFile) {
        this.historyFile = historyFile;
    }

    /**
     * By default history size is 500
     *
     * @return size
     */
    public int getHistorySize() {
        return historySize;
    }

    public void setHistorySize(int historySize) {
        this.historySize = historySize;
    }

    /**
     * By default, bell style is noisy
     * NOTE: Not implemented yet
     *
     * @return bell style
     */
    public String getBellStyle() {
        return bellStyle;
    }

    public void setBellStyle(String bellStyle) {
        this.bellStyle = bellStyle;
    }

    /**
     * @return true if its an ansi console
     */
    public boolean isAnsiConsole() {
        return ansiConsole;
    }

    /**
     * Possible to override the type of console
     * NOTE: atm jreadline will always presume it uses and ansi console
     *
     * @param ansiConsole is it an ansi compatible console?
     */
    public void setAnsiConsole(boolean ansiConsole) {
        this.ansiConsole = ansiConsole;
    }

    /**
     * If not set, FileDescriptor.in will be used
     *
     * @return input
     */
    public InputStream getInputStream() {
        if(inputStream == null)
            return new FileInputStream(FileDescriptor.in);
        else
            return inputStream;
    }

    /**
     * Set where input is coming from
     *
     * @param inputStream input
     */
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * If not set System.out is used
     * @return out
     */
    public OutputStream getOutputStream() {
        if(outputStream == null)
            return System.out;
        else
            return outputStream;
    }

    /**
     * Set where output should go to
     * @param outputStream output
     */
    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    /**
     * Use the specified terminal implementation
     * If not set, jreadline will try to use the best suited one
     * @return terminal
     */
    public Terminal getTerminal() {
        if(terminal == null) {
            if(Config.isOSPOSIXCompatible())
                return new POSIXTerminal();
            else
                return new WindowsTerminal();
        }
        else
            return terminal;
    }

    /**
     * Specify which terminal impl to use
     * @param terminal term
     */
    public void setTerminal(Terminal terminal) {
        this.terminal = terminal;
    }

    public File getInputrc() {
        if(inputrc == null) {
            inputrc = new File(System.getProperty("user.home")+Config.getPathSeparator()+".inputrc");
        }
        return inputrc;
    }

    public void setInputrc(File inputrc) {
        this.inputrc = inputrc;
    }

    public boolean isLogging() {
        return isLogging;
    }

    public void setLogging(boolean logging) {
        isLogging = logging;
    }

    public boolean isDisableCompletion() {
        return disableCompletion;
    }

    public void setDisableCompletion(boolean disableCompletion) {
        this.disableCompletion = disableCompletion;
    }

    /**
     * Get log file
     *
     * @return log file
     */
    public String getLogFile() {
        if(logFile == null)
            logFile = "jreadline.log";
        return logFile;
    }

    /**
     * Specify a log file
     *
     * @param logFile file
     */
    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }

    /**
     * Should we read config from inputrc
     * Set to true by default
     *
     * @return do we?
     */
    public boolean doReadInputrc() {
        return readInputrc;
    }

    /**
     * Specify if we should read config from inputrc
     * Set to true by default
     *
     * @param readInputrc specify
     */
    public void setReadInputrc(boolean readInputrc) {
        this.readInputrc = readInputrc;
    }
}
