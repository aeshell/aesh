/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
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
