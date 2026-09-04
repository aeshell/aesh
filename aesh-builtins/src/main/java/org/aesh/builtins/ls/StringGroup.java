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
package org.aesh.builtins.ls;

import org.aesh.terminal.utils.Parser;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class StringGroup {
    private String[] strings;
    private int maxLength = 0;

    public StringGroup(int size) {
        strings = new String[size];
    }

    public int largestString() {
       return maxLength;
    }

    public String[] getStrings() {
        return strings;
    }

    public void addString(String s, int place) {
        strings[place] = s;
        if(s.length() > maxLength)
            maxLength = s.length();
    }

    public String getString(int place) {
        return strings[place];
    }

    public String getFormattedString(int place) {
        return Parser.padLeft(maxLength+1, strings[place]);
    }

    public String getFormattedStringPadRight(int place) {
        return " " + Parser.padRight(maxLength, strings[place]);
    }

    public void formatStringsBasedOnMaxLength() {
        for(int i=0; i<strings.length;i++)
            strings[i] = Parser.padLeft(maxLength, strings[i]);
    }

}
