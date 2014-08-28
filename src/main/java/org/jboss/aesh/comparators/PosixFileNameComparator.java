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
package org.jboss.aesh.comparators;

import static org.jboss.aesh.constants.AeshConstants.DOT;

import java.util.Comparator;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 * @author <a href="mailto:danielsoro@gmail.com">Daniel Cunha (soro)</a>
 * @author <a href="mailto:robert@balent.cz">Robert Balent</a>
 *
 */
public class PosixFileNameComparator implements Comparator<String> {
    @Override
    public int compare(String o1, String o2) {
        String o1WithoutDot = o1;
        String o2WithoutDot = o2;

        if (o1.indexOf(DOT) == 0) {
            o1WithoutDot = o1.substring(1);
        }
        if (o2.indexOf(DOT) == 0) {
            o2WithoutDot = o2.substring(1);
        }

        // if names are same when removed dot, make without dot first
        // if names are same when ignored case, make lower case first (by default compareTo returns upper case first)
        if (o1WithoutDot.compareTo(o2WithoutDot) == 0) {
            return o2.compareTo(o1);
        } else if (o1WithoutDot.compareToIgnoreCase(o2WithoutDot) == 0) {
            return o2WithoutDot.compareTo(o1WithoutDot);
        } else {
            return o1WithoutDot.compareToIgnoreCase(o2WithoutDot);
        }
    }
}
