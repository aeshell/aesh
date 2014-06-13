/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.io.filter;

import org.jboss.aesh.io.Resource;

import static org.jboss.aesh.constants.AeshConstants.DOT;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 * @author <a href="mailto:danielsoro@gmail.com">Daniel Cunha (soro)</a>
 *
 */
public class NoDotNamesFilter implements ResourceFilter {

    @Override
    public boolean accept(Resource pathname) {
        return !pathname.getName().startsWith(Character.toString(DOT));
    }
}