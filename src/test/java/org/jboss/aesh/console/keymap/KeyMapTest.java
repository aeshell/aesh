/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jboss.aesh.console.keymap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.aesh.terminal.api.Console;
import org.jboss.aesh.terminal.utils.InfoCmp.Capability;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.jboss.aesh.console.keymap.KeyMap.alt;
import static org.jboss.aesh.console.keymap.KeyMap.del;
import static org.jboss.aesh.console.keymap.KeyMap.key;
import static org.jboss.aesh.console.keymap.KeyMap.esc;
import static org.jboss.aesh.console.keymap.KeyMap.ctrl;
import static org.jboss.aesh.console.keymap.KeyMap.display;
import static org.jboss.aesh.console.keymap.KeyMap.range;
import static org.jboss.aesh.console.keymap.KeyMap.translate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class KeyMapTest {

    @Test
    public void testBound() throws Exception {
        Console console = new DumbConsole(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
        KeyMap map = emacs(console);

        Assert.assertEquals(COMPLETE_WORD, map.getBound("\u001B\u001B"));
        assertEquals(BACKWARD_WORD, map.getBound(alt("b")));

        map.bindIfNotBound(UP_HISTORY, "\033[0A");
        assertEquals(UP_HISTORY, map.getBound("\033[0A"));

        map.bind(DOWN_HISTORY, "\033[0AB");
        assertEquals(UP_HISTORY, map.getBound("\033[0A"));
        assertEquals(DOWN_HISTORY, map.getBound("\033[0AB"));

        int[] remaining = new int[1];
        assertEquals(COMPLETE_WORD, map.getBound("\u001B\u001Ba", remaining));
        assertEquals(1, remaining[0]);

        map.bind("anotherkey", translate("^Uc"));
        assertEquals("anotherkey", map.getBound(translate("^Uc"), remaining));
        assertEquals(0, remaining[0]);
        assertEquals(KILL_WHOLE_LINE, map.getBound(translate("^Ua"), remaining));
        assertEquals(1, remaining[0]);
    }

    @Test
    public void testRemaining() throws Exception {
        KeyMap map = new KeyMap();

        int[] remaining = new int[1];
        assertNull(map.getBound("ab", remaining));
        map.bind(SEND_BREAK, "ab");
        assertNull(map.getBound("a", remaining));
        assertEquals(-1, remaining[0]);
        assertEquals(SEND_BREAK, map.getBound("ab", remaining));
        assertEquals(0, remaining[0]);
        assertEquals(SEND_BREAK, map.getBound("abc", remaining));
        assertEquals(1, remaining[0]);

        map.bind(ACCEPT_LINE, "abc");
        assertNull(map.getBound("a", remaining));
        assertEquals(-1, remaining[0]);
        assertEquals(SEND_BREAK, map.getBound("ab", remaining));
        assertEquals(-1, remaining[0]);
        assertEquals(SEND_BREAK, map.getBound("abd", remaining));
        assertEquals(1, remaining[0]);
        assertEquals(ACCEPT_LINE, map.getBound("abc", remaining));
        assertEquals(0, remaining[0]);

        map.unbind("abc");
        assertNull(map.getBound("a", remaining));
        assertEquals(-1, remaining[0]);
        assertEquals(SEND_BREAK, map.getBound("ab", remaining));
        assertEquals(0, remaining[0]);
        assertEquals(SEND_BREAK, map.getBound("abc", remaining));
        assertEquals(1, remaining[0]);
    }

    @Test
    public void testSort() {
        List<String> strings = new ArrayList<>();
        strings.add("abc");
        strings.add("ab");
        strings.add("ad");
        Collections.sort(strings, KeyMap.KEYSEQ_COMPARATOR);
        assertEquals("ab", strings.get(0));
        assertEquals("ad", strings.get(1));
        assertEquals("abc", strings.get(2));
    }

    @Test
    public void testTranslate() {
        assertEquals("\\\u0007\b\u001b\u001b\f\n\r\t\u000b\u0053\u0045\u2345",
                translate("\\\\\\a\\b\\e\\E\\f\\n\\r\\t\\v\\123\\x45\\u2345"));
        assertEquals("\u0001\u0001\u0002\u0002\u0003\u0003\u007f^",
                translate("\\Ca\\CA\\C-B\\C-b^c^C^?^^"));
        assertEquals("\u001b3", translate("'\\e3'"));
        assertEquals("\u001b3", translate("\"\\e3\""));
    }

    @Test
    public void testDisplay() {
        assertEquals("\"\\\\^G^H^[^L^J^M^I\\u0098\\u2345\"",
                display("\\\u0007\b\u001b\f\n\r\t\u0098\u2345"));
        assertEquals("\"^A^B^C^?\\^\\\\\"",
                display("\u0001\u0002\u0003\u007f^\\"));
    }
    
    @Test
    public void testRange() {
        Collection<String> range = range("a^A-a^D");
        assertEquals(Arrays.asList(translate("a^A"), translate("a^B"), translate("a^C"), translate("a^D")), range);
    }

    private static final String ACCEPT_LINE = "accept-line";
    private static final String BACKWARD_CHAR = "backward-char";
    private static final String BACKWARD_DELETE_CHAR = "backward-delete-char";
    private static final String BACKWARD_KILL_WORD = "backward-kill-word";
    private static final String BACKWARD_WORD = "backward-word";
    private static final String BEGINNING_OF_HISTORY = "beginning-of-history";
    private static final String BEGINNING_OF_LINE = "beginning-of-line";
    private static final String CAPITALIZE_WORD = "capitalize-word";
    private static final String CHARACTER_SEARCH = "character-search";
    private static final String CHARACTER_SEARCH_BACKWARD = "character-search-backward";
    private static final String CLEAR_SCREEN = "clear-screen";
    private static final String COMPLETE_WORD = "complete-word";
    private static final String COPY_PREV_WORD = "copy-prev-word";
    private static final String DELETE_CHAR = "delete-char";
    private static final String DELETE_CHAR_OR_LIST = "delete-char-or-list";
    private static final String DIGIT_ARGUMENT = "digit-argument";
    private static final String DO_LOWERCASE_VERSION = "do-lowercase-version";
    private static final String DOWN_CASE_WORD = "down-case-word";
    private static final String DOWN_HISTORY = "down-history";
    private static final String DOWN_LINE_OR_HISTORY = "down-line-or-history";
    private static final String END_OF_HISTORY = "end-of-history";
    private static final String END_OF_LINE = "end-of-line";
    private static final String EXCHANGE_POINT_AND_MARK = "exchange-point-and-mark";
    private static final String FORWARD_CHAR = "forward-char";
    private static final String FORWARD_WORD = "forward-word";
    private static final String HISTORY_INCREMENTAL_SEARCH_BACKWARD = "history-incremental-search-backward";
    private static final String HISTORY_INCREMENTAL_SEARCH_FORWARD = "history-incremental-search-forward";
    private static final String HISTORY_SEARCH_BACKWARD = "history-search-backward";
    private static final String HISTORY_SEARCH_FORWARD = "history-search-forward";
    private static final String INSERT_CLOSE_CURLY = "insert-close-curly";
    private static final String INSERT_CLOSE_PAREN = "insert-close-paren";
    private static final String INSERT_CLOSE_SQUARE = "insert-close-square";
    private static final String KILL_LINE = "kill-line";
    private static final String KILL_WHOLE_LINE = "kill-whole-line";
    private static final String KILL_WORD = "kill-word";
    private static final String LIST_CHOICES = "list-choices";
    private static final String NEG_ARGUMENT = "neg-argument";
    private static final String OVERWRITE_MODE = "overwrite-mode";
    private static final String QUOTED_INSERT = "quoted-insert";
    private static final String REDO = "redo";
    private static final String SELF_INSERT = "self-insert";
    private static final String SELF_INSERT_UNMETA = "self-insert-unmeta";
    private static final String SEND_BREAK = "abort";
    private static final String SET_MARK_COMMAND = "set-mark-command";
    private static final String TRANSPOSE_CHARS = "transpose-chars";
    private static final String TRANSPOSE_WORDS = "transpose-words";
    private static final String UNDO = "undo";
    private static final String UP_CASE_WORD = "up-case-word";
    private static final String UP_HISTORY = "up-history";
    private static final String UP_LINE_OR_HISTORY = "up-line-or-history";
    private static final String VI_CMD_MODE = "vi-cmd-mode";
    private static final String VI_MATCH_BRACKET = "vi-match-bracket";
    private static final String WHAT_CURSOR_POSITION = "what-cursor-position";
    private static final String YANK = "yank";
    private static final String YANK_POP = "yank-pop";

    public static KeyMap emacs(Console console) {
        KeyMap emacs = new KeyMap();
        emacs.bind(SET_MARK_COMMAND, ctrl('@'));
        emacs.bind(BEGINNING_OF_LINE, ctrl('A'));
        emacs.bind(BACKWARD_CHAR, ctrl('B'));
        emacs.bind(DELETE_CHAR_OR_LIST, ctrl('D'));
        emacs.bind(END_OF_LINE, ctrl('E'));
        emacs.bind(FORWARD_CHAR, ctrl('F'));
        emacs.bind(SEND_BREAK, ctrl('G'));
        emacs.bind(BACKWARD_DELETE_CHAR, ctrl('H'));
        emacs.bind(COMPLETE_WORD, ctrl('I'));
        emacs.bind(ACCEPT_LINE, ctrl('J'));
        emacs.bind(KILL_LINE, ctrl('K'));
        emacs.bind(CLEAR_SCREEN, ctrl('L'));
        emacs.bind(ACCEPT_LINE, ctrl('M'));
        emacs.bind(DOWN_LINE_OR_HISTORY, ctrl('N'));
        emacs.bind(UP_LINE_OR_HISTORY, ctrl('P'));
        emacs.bind(HISTORY_INCREMENTAL_SEARCH_BACKWARD, ctrl('R'));
        emacs.bind(HISTORY_INCREMENTAL_SEARCH_FORWARD, ctrl('S'));
        emacs.bind(TRANSPOSE_CHARS, ctrl('T'));
        emacs.bind(KILL_WHOLE_LINE, ctrl('U'));
        emacs.bind(QUOTED_INSERT, ctrl('V'));
        emacs.bind(BACKWARD_KILL_WORD, ctrl('W'));
        emacs.bind(YANK, ctrl('Y'));
        emacs.bind(CHARACTER_SEARCH, ctrl(']'));
        emacs.bind(UNDO, ctrl('_'));
        emacs.bind(SELF_INSERT, range(" -~"));
        emacs.bind(INSERT_CLOSE_PAREN, ")");
        emacs.bind(INSERT_CLOSE_SQUARE, "]");
        emacs.bind(INSERT_CLOSE_CURLY, "}");
        emacs.bind(BACKWARD_DELETE_CHAR, del());
        emacs.bind(VI_MATCH_BRACKET, translate("^X^B"));
        emacs.bind(SEND_BREAK, translate("^X^G"));
        emacs.bind(OVERWRITE_MODE, translate("^X^O"));
        emacs.bind(REDO, translate("^X^R"));
        emacs.bind(UNDO, translate("^X^U"));
        emacs.bind(VI_CMD_MODE, translate("^X^V"));
        emacs.bind(EXCHANGE_POINT_AND_MARK, translate("^X^X"));
        emacs.bind(DO_LOWERCASE_VERSION, translate("^XA-^XZ"));
        emacs.bind(WHAT_CURSOR_POSITION, translate("^X="));
        emacs.bind(KILL_LINE, translate("^X^?"));
        emacs.bind(SEND_BREAK, alt(ctrl('G')));
        emacs.bind(BACKWARD_KILL_WORD, alt(ctrl('H')));
        emacs.bind(SELF_INSERT_UNMETA, alt(ctrl('M')));
        emacs.bind(COMPLETE_WORD, alt(esc()));
        emacs.bind(CHARACTER_SEARCH_BACKWARD, alt(ctrl(']')));
        emacs.bind(COPY_PREV_WORD, alt(ctrl('_')));
        emacs.bind(SET_MARK_COMMAND, alt(' '));
        emacs.bind(NEG_ARGUMENT, alt('-'));
        emacs.bind(DIGIT_ARGUMENT, range("\\E0-\\E9"));
        emacs.bind(BEGINNING_OF_HISTORY, alt('<'));
        emacs.bind(LIST_CHOICES, alt('='));
        emacs.bind(END_OF_HISTORY, alt('>'));
        emacs.bind(LIST_CHOICES, alt('?'));
        emacs.bind(DO_LOWERCASE_VERSION, range("^[A-^[Z"));
        emacs.bind(BACKWARD_WORD, alt('b'));
        emacs.bind(CAPITALIZE_WORD, alt('c'));
        emacs.bind(KILL_WORD, alt('d'));
        emacs.bind(FORWARD_WORD, alt('f'));
        emacs.bind(DOWN_CASE_WORD, alt('l'));
        emacs.bind(HISTORY_SEARCH_FORWARD, alt('n'));
        emacs.bind(HISTORY_SEARCH_BACKWARD, alt('p'));
        emacs.bind(TRANSPOSE_WORDS, alt('t'));
        emacs.bind(UP_CASE_WORD, alt('u'));
        emacs.bind(YANK_POP, alt('y'));
        emacs.bind(BACKWARD_KILL_WORD, alt(del()));
        emacs.bind(UP_LINE_OR_HISTORY, key(console, Capability.key_up));
        emacs.bind(DOWN_LINE_OR_HISTORY, key(console, Capability.key_down));
        emacs.bind(BACKWARD_CHAR, key(console, Capability.key_left));
        emacs.bind(FORWARD_CHAR, key(console, Capability.key_right));
        emacs.bind(BEGINNING_OF_LINE, key(console, Capability.key_home));
        emacs.bind(END_OF_LINE, key(console, Capability.key_end));
        emacs.bind(DELETE_CHAR, key(console, Capability.key_dc));
        emacs.bind(KILL_WHOLE_LINE, key(console, Capability.key_dl));
        emacs.bind(OVERWRITE_MODE, key(console, Capability.key_ic));
        emacs.bind(FORWARD_WORD, alt(key(console, Capability.key_right)));
        emacs.bind(BACKWARD_WORD, alt(key(console, Capability.key_left)));
        return emacs;
    }

}
