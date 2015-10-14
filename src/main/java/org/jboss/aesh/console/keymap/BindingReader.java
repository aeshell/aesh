/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jboss.aesh.console.keymap;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * The BindingReader will transform incoming chars into
 * key bindings
 */
public class BindingReader implements IntConsumer {

    public static final long DEFAULT_AMBIGUOUS_TIMEOUT = 1000l;

    protected final Consumer<Object> bindingConsumer;
    protected ScheduledExecutorService timer;
    protected final Object unicode;
    protected final Object nomatch;
    protected final long ambiguousTimeout;
    protected final StringBuilder opBuffer = new StringBuilder();
    protected String lastBinding;
    protected KeyMap keys;
    protected KeyMap local;
    protected int highChar = -1;

    protected AtomicReference<ScheduledFuture<?>> future = new AtomicReference<>();

    /**
     * Build a new BindingReader.
     *
     * @param bindingConsumer the consumer of key bindings
     * @param timer a scheduler to use when
     * @param unicode
     * @param nomatch
     */
    public BindingReader(Consumer<Object> bindingConsumer, ScheduledExecutorService timer, Object unicode, Object nomatch) {
        this(bindingConsumer, timer, unicode, nomatch, DEFAULT_AMBIGUOUS_TIMEOUT);
    }

    public BindingReader(Consumer<Object> bindingConsumer, ScheduledExecutorService timer, Object unicode, Object nomatch, long ambiguousTimeout) {
        this.bindingConsumer = bindingConsumer;
        this.timer = timer;
        this.unicode = unicode;
        this.nomatch = nomatch;
        this.ambiguousTimeout = ambiguousTimeout;
    }

    public void setKeyMaps(KeyMap keys) {
        setKeyMaps(keys, null);
    }

    public synchronized void setKeyMaps(KeyMap keys, KeyMap local) {
        this.keys = keys;
        this.local = local;
    }

    @Override
    public void accept(int value) {
        if (highChar == -1) {
            if (Character.isHighSurrogate((char) value)) {
                highChar = value;
            } else {
                opBuffer.appendCodePoint(value);
                decode(false);
            }
        } else {
            opBuffer.appendCodePoint(Character.toCodePoint((char) highChar, (char) value));
            highChar = -1;
            decode(false);
        }
    }

    protected synchronized void decode(boolean timeout) {
        // Cancel timeout
        ScheduledFuture<?> f = future.getAndSet(null);
        if (f != null) {
            f.cancel(false);
        }
        // Decode
        while (opBuffer.length() > 0) {
            Object o = null;
            int[] remaining = new int[1];
            if (local != null) {
                o = local.getBound(opBuffer, remaining);
            }
            if (o == null && (local == null || remaining[0] >= 0)) {
                o = keys.getBound(opBuffer, remaining);
            }
            if (remaining[0] > 0) {
                // We have a binding and additional chars
                if (o != null) {
                    String rem = opBuffer.substring(opBuffer.length() - remaining[0]);
                    lastBinding = opBuffer.substring(0, opBuffer.length() - remaining[0]);
                    opBuffer.setLength(0);
                    opBuffer.append(rem);
                }
                // We don't match anything
                else {
                    int cp = opBuffer.codePointAt(0);
                    String rem = opBuffer.substring(Character.charCount(cp));
                    lastBinding = opBuffer.substring(0, Character.charCount(cp));
                    // Unicode character
                    if (cp >= KeyMap.KEYMAP_LENGTH && unicode != null) {
                        o = unicode;
                    } else {
                        o = nomatch;
                    }
                    opBuffer.setLength(0);
                    opBuffer.append(rem);
                }
            } else if (remaining[0] < 0 && o != null) {
                if (!timeout) {
                    startTimer();
                    break;
                }
                lastBinding = opBuffer.toString();
                opBuffer.setLength(0);
            } else if (remaining[0] == 0 && o != null) {
                lastBinding = opBuffer.toString();
                opBuffer.setLength(0);
            }
            if (o != null) {
                bindingConsumer.accept(o);
            } else {
                break;
            }
        }
    }

    private void startTimer() {
        if (timer != null) {
            future.set(timer.schedule(this::onTimeout, ambiguousTimeout, TimeUnit.MILLISECONDS));
        }
    }

    private void onTimeout() {
        decode(true);
    }


    public synchronized void runMacro(String macro) {
        opBuffer.append(macro);
    }

    public synchronized String getCurrentBuffer() {
        return opBuffer.toString();
    }

    public synchronized String getLastBinding() {
        return lastBinding;
    }

}
