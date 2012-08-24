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

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class RedirectionParserTest extends TestCase {

    public RedirectionParserTest(String name) {
        super(name);
    }

    public void testMatchesRedirectionOperation() {
        assertTrue( RedirectionParser.matchesRedirectionOperation("ls /var/log | less"));
        assertTrue( RedirectionParser.matchesRedirectionOperation("ls /var/log |& less"));
        assertTrue( RedirectionParser.matchesRedirectionOperation("more /var/log > foo.txt"));
        assertTrue( RedirectionParser.matchesRedirectionOperation("more < /var/log > foo.txt"));
        assertTrue( RedirectionParser.matchesRedirectionOperation("more /var/log 2> foo.txt"));
        assertTrue( RedirectionParser.matchesRedirectionOperation("more /var/log 2>&1 foo.txt"));
    }

    public void testParseBuffer() {
        //assertEquals(" foo.txt", RedirectionParser.parseBuffer("ls . 2>&1 foo.txt").get(0).getBuffer());
        //assertEquals(new RedirectionOperation(Redirection.OVERWRITE_OUT_AND_ERR, " foo.txt"),
        //        RedirectionParser.parseBuffer("ls . 2>&1 foo.txt").get(0));
        //assertEquals(new RedirectionOperation(Redirection.OVERWRITE_OUT_AND_ERR, " foo.txt "),
        //        RedirectionParser.parseBuffer("ls . 2>&1 foo.txt > bah").get(0));
    }

    public void testFindNextRedirection() {
        assertEquals(new RedirectionOperation(Redirection.PIPE," foo"),
                RedirectionParser.findNextRedirection("| foo"));
        assertEquals(new RedirectionOperation(Redirection.OVERWRITE_OUT," foo"),
                RedirectionParser.findNextRedirection("> foo"));
        //assertEquals(Redirection.PIPE_OUT_AND_ERR, RedirectionParser.findNextRedirection("|& foo"));
        //assertEquals(Redirection.APPEND_ERR, RedirectionParser.findNextRedirection("2>> foo"));
        //assertEquals(Redirection.OVERWRITE_OUT_AND_ERR, RedirectionParser.findNextRedirection("2>&1 foo"));
    }
}
