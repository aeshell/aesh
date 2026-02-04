package org.aesh.command.impl.shell;

import org.aesh.command.impl.operator.OutputDelegate;
import org.aesh.command.shell.Shell;
import org.aesh.readline.Prompt;
import org.aesh.terminal.Key;
import org.aesh.terminal.tty.Size;
import org.aesh.terminal.utils.Parser;
import org.aesh.terminal.utils.Config;

import java.util.concurrent.TimeUnit;

/**
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class ShellOutputDelegate implements Shell {

    private final Shell delegate;
    private final OutputDelegate output;

    public ShellOutputDelegate(Shell delegate, OutputDelegate output) {
        this.delegate = delegate;
        this.output = output;
    }

    private void doWrite(String out) {
        output.write(out);
    }

    @Override
    public void write(String out, boolean paging) {
        doWrite(out);
    }

    @Override
    public void writeln(String out, boolean paging) {
        doWrite(out+ Config.getLineSeparator());
    }

    @Override
    public void write(int[] out) {
        doWrite(Parser.fromCodePoints(out));
    }

    @Override
    public void write(char out) {
        output.write(String.valueOf(out));
    }

    @Override
    public String readLine() throws InterruptedException {
        return delegate.readLine();
    }

    @Override
    public String readLine(Prompt prompt) throws InterruptedException {
        return delegate.readLine(prompt);
    }

    @Override
    public Key read() throws InterruptedException {
        return delegate.read();
    }

    @Override
    public Key read(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.read(timeout,unit);
    }

    @Override
    public Key read(Prompt prompt) throws InterruptedException {
        return delegate.read(prompt);
    }

    @Override
    public boolean enableAlternateBuffer() {
        //do nothing when we're redirection output
        return false;
    }

    @Override
    public boolean enableMainBuffer() {
        //do nothing when we're redirection output
        return false;
    }

    @Override
    public Size size() {
        return delegate.size();
    }

    @Override
    public void clear() {
        //do nothing when we're redirection output
    }
}
