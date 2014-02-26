/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.operator;

import junit.framework.TestCase;
import org.jboss.aesh.console.ConsoleOperation;

import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ControlOperatorParserTest extends TestCase {

    public ControlOperatorParserTest(String name) {
        super(name);
    }

    public void testRedirectionOperation() {
        assertEquals(new ConsoleOperation(ControlOperator.NONE, "ls foo.txt"),
                ControlOperatorParser.findAllControlOperators("ls foo.txt").get(0));

        assertEquals(new ConsoleOperation(ControlOperator.OVERWRITE_OUT, "ls . "),
                ControlOperatorParser.findAllControlOperators("ls . > foo.txt").get(0));
        assertEquals(new ConsoleOperation(ControlOperator.NONE, " foo.txt"),
                ControlOperatorParser.findAllControlOperators("ls . > foo.txt").get(1));

        assertEquals(new ConsoleOperation(ControlOperator.OVERWRITE_OUT, "bas "),
                ControlOperatorParser.findAllControlOperators("bas > foo.txt 2>&1 ").get(0));
        assertEquals(new ConsoleOperation(ControlOperator.OVERWRITE_OUT_AND_ERR, " foo.txt "),
                ControlOperatorParser.findAllControlOperators("bas > foo.txt 2>&1 ").get(1));

        List<ConsoleOperation> ops =
                ControlOperatorParser.findAllControlOperators("bas | foo.txt 2>&1 foo");

        assertEquals(new ConsoleOperation(ControlOperator.PIPE, "bas "), ops.get(0));
        assertEquals(new ConsoleOperation(ControlOperator.OVERWRITE_OUT_AND_ERR, " foo.txt "), ops.get(1));
        assertEquals(new ConsoleOperation(ControlOperator.NONE, " foo"), ops.get(2));

        ops = ControlOperatorParser.findAllControlOperators("bas | foo");
        assertEquals(new ConsoleOperation(ControlOperator.PIPE, "bas "), ops.get(0));
        assertEquals(new ConsoleOperation(ControlOperator.NONE, " foo"), ops.get(1));

        ops = ControlOperatorParser.findAllControlOperators("bas 2> foo");
        assertEquals(new ConsoleOperation(ControlOperator.OVERWRITE_ERR, "bas "), ops.get(0));
        assertEquals(new ConsoleOperation(ControlOperator.NONE, " foo"), ops.get(1));

        ops = ControlOperatorParser.findAllControlOperators("bas < foo");
        assertEquals(new ConsoleOperation(ControlOperator.OVERWRITE_IN, "bas "), ops.get(0));
        assertEquals(new ConsoleOperation(ControlOperator.NONE, " foo"), ops.get(1));

        ops = ControlOperatorParser.findAllControlOperators("bas > foo > foo2");
        assertEquals(new ConsoleOperation(ControlOperator.OVERWRITE_OUT, "bas "), ops.get(0));
        assertEquals(new ConsoleOperation(ControlOperator.OVERWRITE_OUT, " foo "), ops.get(1));
        assertEquals(new ConsoleOperation(ControlOperator.NONE, " foo2"), ops.get(2));

        ops = ControlOperatorParser.findAllControlOperators("bas > foo; foo2");
        assertEquals(new ConsoleOperation(ControlOperator.OVERWRITE_OUT, "bas "), ops.get(0));
        assertEquals(new ConsoleOperation(ControlOperator.END, " foo"), ops.get(1));
        assertEquals(new ConsoleOperation(ControlOperator.NONE, " foo2"), ops.get(2));

         ops = ControlOperatorParser.findAllControlOperators("bas & foo; foo2 && foo3; bar || foo4");
        assertEquals(new ConsoleOperation(ControlOperator.AMP, "bas "), ops.get(0));
        assertEquals(new ConsoleOperation(ControlOperator.END, " foo"), ops.get(1));
        assertEquals(new ConsoleOperation(ControlOperator.AND, " foo2 "), ops.get(2));
        assertEquals(new ConsoleOperation(ControlOperator.END, " foo3"), ops.get(3));
        assertEquals(new ConsoleOperation(ControlOperator.OR, " bar "), ops.get(4));
        assertEquals(new ConsoleOperation(ControlOperator.NONE, " foo4"), ops.get(5));

        //escaped pipes
        assertEquals(new ConsoleOperation(ControlOperator.NONE, "ls \\>foo.txt"),
                ControlOperatorParser.findAllControlOperators("ls \\>foo.txt").get(0));

        assertEquals(new ConsoleOperation(ControlOperator.NONE, "ls \\<foo.txt"),
                ControlOperatorParser.findAllControlOperators("ls \\<foo.txt").get(0));

        assertEquals(new ConsoleOperation(ControlOperator.NONE, "ls \\|foo.txt"),
                ControlOperatorParser.findAllControlOperators("ls \\|foo.txt").get(0));

        assertEquals(new ConsoleOperation(ControlOperator.NONE, "ls \\&foo.txt"),
                ControlOperatorParser.findAllControlOperators("ls \\&foo.txt").get(0));

        assertEquals(new ConsoleOperation(ControlOperator.NONE, "ls \\<foo.txt\\>"),
                ControlOperatorParser.findAllControlOperators("ls \\<foo.txt\\>").get(0));

        assertEquals(new ConsoleOperation(ControlOperator.OVERWRITE_OUT, "ls \\<foo.txt\\> "),
                ControlOperatorParser.findAllControlOperators("ls \\<foo.txt\\> > test.txt").get(0));

        assertEquals(new ConsoleOperation(ControlOperator.NONE, "ls bar=>foo"),
                ControlOperatorParser.findAllControlOperators("ls bar=>foo").get(0));

        assertEquals(new ConsoleOperation(ControlOperator.NONE, "ls bar=<foo"),
                ControlOperatorParser.findAllControlOperators("ls bar=<foo").get(0));

        assertEquals(new ConsoleOperation(ControlOperator.NONE, "ls bar>=foo"),
                ControlOperatorParser.findAllControlOperators("ls bar>=foo").get(0));

        assertEquals(new ConsoleOperation(ControlOperator.NONE, "ls bar<=foo"),
                ControlOperatorParser.findAllControlOperators("ls bar<=foo").get(0));

    }

    public void testFindLastRedirectionBeforeCursor() {
        assertEquals(0, ControlOperatorParser.findLastRedirectionPositionBeforeCursor(" foo", 5));
        assertEquals(2, ControlOperatorParser.findLastRedirectionPositionBeforeCursor(" > foo", 5));
        assertEquals(4, ControlOperatorParser.findLastRedirectionPositionBeforeCursor("ls > bah > foo", 6));
        assertEquals(10, ControlOperatorParser.findLastRedirectionPositionBeforeCursor("ls > bah > foo", 12));
        assertEquals(13, ControlOperatorParser.findLastRedirectionPositionBeforeCursor("ls > bah 2>&1 foo", 15));

    }
}
