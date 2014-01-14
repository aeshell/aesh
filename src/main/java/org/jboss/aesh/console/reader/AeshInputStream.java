/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.reader;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.terminal.Key;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshInputStream extends InputStream {

    private final ArrayBlockingQueue<String> blockingQueue;
    private String b;
    private int c;
    private transient boolean stopped = false;

    public AeshInputStream(ArrayBlockingQueue<String> blockingQueue) {
        this.blockingQueue = blockingQueue;
    }

    @Override
    public int read() throws IOException {
        if(stopped && blockingQueue.size() == 0)
            return -1;
        try {
            if (b == null || c == b.length()) {
                b = blockingQueue.poll(365, TimeUnit.DAYS);
                c = 0;
            }

            if (b != null && !(b.length() == 0)) {
                return b.charAt(c++);
            }
        } catch (InterruptedException e) {
            //
        }
        return -1;
    }

    public int[] readAll() {
        if(stopped && blockingQueue.size() == 0)
            return new int[] {-1};
        try {
            if(Config.isOSPOSIXCompatible()) {
                String out = blockingQueue.poll(356, TimeUnit.DAYS);
                int[] input = new int[out.length()];
                for(int i=0; i < out.length(); i++)
                    input[i] = out.charAt(i);
                return input;
            }
            else {
                String out = blockingQueue.poll(356, TimeUnit.DAYS);
                //hack to make multi-value input work (arrows ++)
                if(out.charAt(0) == Key.WINDOWS_ESC.getAsChar()) {
                    int[] input = new int[2];
                    input[0] = out.charAt(0);
                    String out2 = blockingQueue.poll(356, TimeUnit.DAYS);
                    input[1] = out2.charAt(0);

                    return input;
                }
                else {
                    int[] input = new int[out.length()];
                    for(int i=0; i < out.length(); i++)
                        input[i] = out.charAt(i);
                    return input;
                }
            }

        }
        catch (InterruptedException e) {
            return new int[] {-1};
        }
    }

    @Override
    public int available() {
        if (b != null)
            return b.length();
        else
            return 0;
    }

    @Override
    public void close() throws IOException {
        stopped = true;
        blockingQueue.add("");
    }

}
