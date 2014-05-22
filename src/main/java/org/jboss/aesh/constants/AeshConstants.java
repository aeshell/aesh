/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.constants;

import org.jboss.aesh.console.Config;

/**
 * 
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 * @author <a href="mailto:danielsoro@gmail.com">Daniel Cunha (soro)</a>
 *
 */
public interface AeshConstants {
    static final char ESCAPE = '\\';
    static final String SPACE = " ";
    static final String TILDE = "~";
    static final String STAR = "*";
    static final String WILDCARD = "?";
    static final String DOT = ".";
    static final String HOME = "~" + Config.getPathSeparator();
    static final String PARENT = "..";
}
