package org.jboss.aesh.console.reader;

import org.jboss.aesh.terminal.TerminalCharacter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshPrintWriter extends PrintWriter {

    public AeshPrintWriter(OutputStream out, boolean autoFlush) {
        super(out,autoFlush);
    }

    public AeshPrintWriter(OutputStreamWriter out, boolean autoFlush) {
        super(out, autoFlush);
    }

    public AeshPrintWriter(StringWriter out, boolean autoFlush) {
        super(out, autoFlush);
    }

    /**
     * Do noting
     */
    @Override
    public void close() {
    }

    public void print(List<TerminalCharacter> chars) throws IOException {
        StringBuilder builder = new StringBuilder();
        TerminalCharacter prev = null;
        for(TerminalCharacter c : chars) {
            if(prev == null)
                builder.append(c.toString());
            else
                builder.append(c.toString(prev));
            prev = c;
        }
        print(builder.toString());
    }

    public void println(List<TerminalCharacter> chars) throws IOException {
        print(chars);
        println();
    }

}
