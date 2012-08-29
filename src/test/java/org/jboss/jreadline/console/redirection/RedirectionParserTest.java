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
package org.jboss.jreadline.console.redirection;

import junit.framework.TestCase;

import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class RedirectionParserTest extends TestCase {

    public RedirectionParserTest(String name) {
        super(name);
    }

    public void testRedirectionOperation() {
        assertEquals(new RedirectionOperation(Redirection.NONE, "ls foo.txt"),
                RedirectionParser.findAllRedirections("ls foo.txt").get(0));

        assertEquals(new RedirectionOperation(Redirection.OVERWRITE_OUT, "ls . "),
                RedirectionParser.findAllRedirections("ls . > foo.txt").get(0));
        assertEquals(new RedirectionOperation(Redirection.NONE, " foo.txt"),
                RedirectionParser.findAllRedirections("ls . > foo.txt").get(1));

        assertEquals(new RedirectionOperation(Redirection.OVERWRITE_OUT, "bas "),
                RedirectionParser.findAllRedirections("bas > foo.txt 2>&1 ").get(0));
        assertEquals(new RedirectionOperation(Redirection.OVERWRITE_OUT_AND_ERR, " foo.txt "),
                RedirectionParser.findAllRedirections("bas > foo.txt 2>&1 ").get(1));

        List<RedirectionOperation> ops =
                RedirectionParser.findAllRedirections("bas | foo.txt 2>&1 foo");

        assertEquals(new RedirectionOperation(Redirection.PIPE, "bas "), ops.get(0));
        assertEquals(new RedirectionOperation(Redirection.OVERWRITE_OUT_AND_ERR, " foo.txt "), ops.get(1));
        assertEquals(new RedirectionOperation(Redirection.NONE, " foo"), ops.get(2));

        ops = RedirectionParser.findAllRedirections("bas | foo");
        assertEquals(new RedirectionOperation(Redirection.PIPE, "bas "), ops.get(0));
        assertEquals(new RedirectionOperation(Redirection.NONE, " foo"), ops.get(1));

        ops = RedirectionParser.findAllRedirections("bas 2> foo");
        assertEquals(new RedirectionOperation(Redirection.OVERWRITE_ERR, "bas "), ops.get(0));
        assertEquals(new RedirectionOperation(Redirection.NONE, " foo"), ops.get(1));

        ops = RedirectionParser.findAllRedirections("bas < foo");
        assertEquals(new RedirectionOperation(Redirection.OVERWRITE_IN, "bas "), ops.get(0));
        assertEquals(new RedirectionOperation(Redirection.NONE, " foo"), ops.get(1));

        ops = RedirectionParser.findAllRedirections("bas > foo > foo2");
        assertEquals(new RedirectionOperation(Redirection.OVERWRITE_OUT, "bas "), ops.get(0));
        assertEquals(new RedirectionOperation(Redirection.OVERWRITE_OUT, " foo "), ops.get(1));
        assertEquals(new RedirectionOperation(Redirection.NONE, " foo2"), ops.get(2));
    }

    public void testFindLastRedirectionBeforeCursor() {
        assertEquals(0, RedirectionParser.findLastRedirectionPositionBeforeCursor(" foo", 5));
        assertEquals(2, RedirectionParser.findLastRedirectionPositionBeforeCursor(" > foo", 5));
        assertEquals(4, RedirectionParser.findLastRedirectionPositionBeforeCursor("ls > bah > foo", 6));
        assertEquals(10, RedirectionParser.findLastRedirectionPositionBeforeCursor("ls > bah > foo", 12));
        assertEquals(13, RedirectionParser.findLastRedirectionPositionBeforeCursor("ls > bah 2>&1 foo", 15));

    }
}
