/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
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
package org.jboss.aesh.console.man;

import org.jboss.aesh.console.man.parser.ManParserUtil;
import org.jboss.aesh.util.ANSI;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ManParserUtilTest {

    @Test
    public void testBoldParser() {
        assertEquals(ANSI.getBold()+"foo"+ ANSI.defaultText(),
                ManParserUtil.convertStringToAnsi("*foo*"));

        assertEquals("12"+ ANSI.getBold()+"foo"+ ANSI.defaultText(),
                ManParserUtil.convertStringToAnsi("12*foo*"));

        assertEquals("12"+ ANSI.getBold()+"foo"+ ANSI.defaultText()+"34",
                ManParserUtil.convertStringToAnsi("12*foo*34"));

        assertEquals("12"+ ANSI.getUnderline()+"foo"+ ANSI.defaultText()+"34",
                ManParserUtil.convertStringToAnsi("12'foo'34"));

        assertEquals("Define or delete document attribute. "+
                ANSI.getUnderline()+
                "ATTRIBUTE"+
                ANSI.defaultText()+
                " is formatted like",
                ManParserUtil.convertStringToAnsi("Define or delete document attribute. 'ATTRIBUTE' is formatted like"));


        assertEquals(ANSI.getBold()+"ZIP_FILE"+ ANSI.defaultText(),
                ManParserUtil.convertStringToAnsi("*ZIP_FILE*::"));
    }
}
