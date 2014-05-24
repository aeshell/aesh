/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.constants;

import org.jboss.aesh.console.Config;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 * @author <a href="mailto:danielsoro@gmail.com">Daniel Cunha (soro)</a>
 *
 */
public interface AeshConstants {
    char ESCAPE = '\\';
    char SPACE = ' ';
    char TILDE = '~';
    char STAR = '*';
    char WILDCARD = '?';
    char DOT = '.';
    String HOME = "~" + Config.getPathSeparator();
    String PARENT = "..";
}
