package org.aesh.tamboui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.aesh.terminal.Attributes;
import org.aesh.terminal.Connection;
import org.aesh.terminal.Device;
import org.aesh.terminal.tty.Capability;
import org.aesh.terminal.tty.Signal;
import org.aesh.terminal.tty.Size;
import org.junit.Test;

/**
 * Tests for {@link NonClosingConnection} — verifies that close() and
 * openBlocking()/openNonBlocking() are no-ops while other methods delegate.
 */
public class NonClosingConnectionTest {

    @Test
    public void testCloseIsNoOp() {
        AtomicBoolean delegateClosed = new AtomicBoolean(false);
        Connection delegate = new StubConnection() {
            @Override
            public void close() {
                delegateClosed.set(true);
            }
        };
        NonClosingConnection wrapper = new NonClosingConnection(delegate);

        wrapper.close();
        assertFalse("Delegate should NOT be closed", delegateClosed.get());
    }

    @Test
    public void testOpenBlockingIsNoOp() {
        AtomicBoolean delegateOpened = new AtomicBoolean(false);
        Connection delegate = new StubConnection() {
            @Override
            public void openBlocking() {
                delegateOpened.set(true);
            }
        };
        NonClosingConnection wrapper = new NonClosingConnection(delegate);

        wrapper.openBlocking();
        assertFalse("Delegate openBlocking should NOT be called", delegateOpened.get());
    }

    @Test
    public void testOpenNonBlockingIsNoOp() {
        AtomicBoolean delegateOpened = new AtomicBoolean(false);
        Connection delegate = new StubConnection() {
            @Override
            public void openNonBlocking() {
                delegateOpened.set(true);
            }
        };
        NonClosingConnection wrapper = new NonClosingConnection(delegate);

        wrapper.openNonBlocking();
        assertFalse("Delegate openNonBlocking should NOT be called", delegateOpened.get());
    }

    @Test
    public void testSizeDelegates() {
        Size expected = new Size(120, 40);
        Connection delegate = new StubConnection() {
            @Override
            public Size size() {
                return expected;
            }
        };
        NonClosingConnection wrapper = new NonClosingConnection(delegate);

        assertEquals(expected, wrapper.size());
    }

    @Test
    public void testStdoutHandlerDelegates() {
        Consumer<int[]> handler = ints -> {
        };
        Connection delegate = new StubConnection() {
            @Override
            public Consumer<int[]> stdoutHandler() {
                return handler;
            }
        };
        NonClosingConnection wrapper = new NonClosingConnection(delegate);

        assertEquals(handler, wrapper.stdoutHandler());
    }

    @Test
    public void testTuiMixinResolverFindsField() {
        TestCommandWithMixin cmd = new TestCommandWithMixin();
        TuiMixin mixin = TuiMixinResolver.findMixin(cmd);
        assertNotNull("Should find TuiMixin field", mixin);
    }

    @Test
    public void testTuiMixinResolverReturnsNullWithoutField() {
        TestCommandWithoutMixin cmd = new TestCommandWithoutMixin();
        TuiMixin mixin = TuiMixinResolver.findMixin(cmd);
        assertNull("Should return null when no TuiMixin field", mixin);
    }

    // Test helpers

    static class TestCommandWithMixin {
        TuiMixin tuiOptions = new TuiMixin();
    }

    static class TestCommandWithoutMixin {
        String name = "test";
    }

    /**
     * Minimal stub implementing Connection for testing.
     */
    static class StubConnection implements Connection {
        @Override
        public Device device() {
            return null;
        }

        @Override
        public Size size() {
            return new Size(80, 24);
        }

        @Override
        public Consumer<Size> sizeHandler() {
            return null;
        }

        @Override
        public void setSizeHandler(Consumer<Size> handler) {
        }

        @Override
        public Consumer<Signal> signalHandler() {
            return null;
        }

        @Override
        public void setSignalHandler(Consumer<Signal> handler) {
        }

        @Override
        public Consumer<int[]> stdinHandler() {
            return null;
        }

        @Override
        public void setStdinHandler(Consumer<int[]> handler) {
        }

        @Override
        public Consumer<int[]> stdoutHandler() {
            return null;
        }

        @Override
        public void setCloseHandler(Consumer<Void> closeHandler) {
        }

        @Override
        public Consumer<Void> closeHandler() {
            return null;
        }

        @Override
        public void close() {
        }

        @Override
        public void openBlocking() {
        }

        @Override
        public void openNonBlocking() {
        }

        @Override
        public boolean put(Capability capability, Object... params) {
            return false;
        }

        @Override
        public Attributes attributes() {
            return new Attributes();
        }

        @Override
        public void setAttributes(Attributes attr) {
        }

        @Override
        public Charset inputEncoding() {
            return Charset.defaultCharset();
        }

        @Override
        public Charset outputEncoding() {
            return Charset.defaultCharset();
        }

        @Override
        public boolean supportsAnsi() {
            return false;
        }
    }
}
