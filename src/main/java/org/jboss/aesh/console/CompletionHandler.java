/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.console.alias.AliasManager;

import java.io.IOException;
import java.io.PrintStream;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface CompletionHandler {

    void addCompletion(Completion completion);

    void removeCompletion(Completion completion);

    void setAskDisplayCompletion(boolean askDisplayCompletion);

    boolean doAskDisplayCompletion();

    void setAskCompletionSize(int size);

    int getAskCompletionSize();

    void complete(PrintStream out, Buffer buffer) throws IOException;

    void setAliasManager(AliasManager aliasManager);
}
