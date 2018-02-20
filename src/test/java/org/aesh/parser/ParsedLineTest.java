package org.aesh.parser;

import org.aesh.command.operator.OperatorType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Marek Marusic <mmarusic@redhat.com> on 2/19/18.
 */
public class ParsedLineTest {
    @Test
    public void firstWordFromEmptyLine() throws Exception {
        List<ParsedWord> words = new ArrayList<>();
        ParsedLine pl = new ParsedLine("", words, -1,
                0, 0, ParserStatus.OK, "", OperatorType.NONE);
        assertEquals(pl.firstWord().word(), "");
    }

    @Test
    public void firstWordFromLineWithWords() throws Exception {
        List<ParsedWord> words = new ArrayList<>();
        words.add(new ParsedWord("command", 0));
        words.add(new ParsedWord("line", 1));
        words.add(new ParsedWord("text", 2));

        ParsedLine pl = new ParsedLine("command line text", words, -1,
                0, 0, ParserStatus.OK, "", OperatorType.NONE);
        assertEquals(pl.firstWord().word(), "command");
    }

}