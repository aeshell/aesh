package org.jboss.aesh.console.reader;

import java.io.BufferedInputStream;

/**
 * Pipes the standard input and standard error
 * to the current running command
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshStandardStream {

    private BufferedInputStream stdIn;
    private BufferedInputStream stdError;

    public AeshStandardStream() {
        stdIn = new BufferedInputStream(null);
        stdError = new BufferedInputStream(null);
    }

    public AeshStandardStream(BufferedInputStream stdIn) {
        this.stdIn = stdIn;
        stdError = new BufferedInputStream(null);
    }

    public AeshStandardStream(BufferedInputStream stdIn, BufferedInputStream stdError) {
        this.stdIn = stdIn;
        this.stdError = stdError;
    }

    public BufferedInputStream getStdIn() {
        return stdIn;
    }

    public void setStdIn(BufferedInputStream stdIn) {
        this.stdIn = stdIn;
    }

    public BufferedInputStream getStdError() {
        return stdError;
    }

    public void setStdError(BufferedInputStream stdError) {
        this.stdError = stdError;
    }
}
