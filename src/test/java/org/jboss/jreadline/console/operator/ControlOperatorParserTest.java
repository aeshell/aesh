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
package org.jboss.jreadline.console.operator;

import junit.framework.TestCase;
import org.jboss.jreadline.console.ConsoleOperation;

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

         ops = ControlOperatorParser.findAllControlOperators("bas & foo; foo2 && foo3; bar");
        assertEquals(new ConsoleOperation(ControlOperator.AMP, "bas "), ops.get(0));
        assertEquals(new ConsoleOperation(ControlOperator.END, " foo"), ops.get(1));
        assertEquals(new ConsoleOperation(ControlOperator.AND, " foo2 "), ops.get(2));
        assertEquals(new ConsoleOperation(ControlOperator.END, " foo3"), ops.get(3));
        assertEquals(new ConsoleOperation(ControlOperator.NONE, " bar"), ops.get(4));



    }

    public void testFindLastRedirectionBeforeCursor() {
        assertEquals(0, ControlOperatorParser.findLastRedirectionPositionBeforeCursor(" foo", 5));
        assertEquals(2, ControlOperatorParser.findLastRedirectionPositionBeforeCursor(" > foo", 5));
        assertEquals(4, ControlOperatorParser.findLastRedirectionPositionBeforeCursor("ls > bah > foo", 6));
        assertEquals(10, ControlOperatorParser.findLastRedirectionPositionBeforeCursor("ls > bah > foo", 12));
        assertEquals(13, ControlOperatorParser.findLastRedirectionPositionBeforeCursor("ls > bah 2>&1 foo", 15));

    }
}
