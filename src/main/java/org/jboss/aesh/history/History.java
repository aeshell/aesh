/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.history;

import java.util.List;

/**
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface History {

    void push(String entry);

    String find(String search);

    String get(int index);

    int size();

    void setSearchDirection(SearchDirection direction);

    SearchDirection getSearchDirection();

    String getNextFetch();

    String getPreviousFetch();

    String search(String search);

    void setCurrent(String line);

    String getCurrent();

    List<String> getAll();

    void clear();

    void stop();

}
