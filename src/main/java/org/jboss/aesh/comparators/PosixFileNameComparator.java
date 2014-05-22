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
 *
 */
public class PosixFileNameComparator implements Comparator<String> {
    @Override
    public int compare(String o1, String o2) {
        if (o1.length() > 0 && o2.length() > 0) {
            if (o1.indexOf(DOT) == 0) {
                if (o2.indexOf(DOT) == 0)
                    return o1.substring(1).compareToIgnoreCase(o2.substring(1));
                else
                    return o1.substring(1).compareToIgnoreCase(o2);
            }
            else {
                if (o2.indexOf(DOT) == 0)
                    return o1.compareToIgnoreCase(o2.substring(1));
                else
                    return o1.compareToIgnoreCase(o2);
            }
        }
        else
            return 0;
    }
}
