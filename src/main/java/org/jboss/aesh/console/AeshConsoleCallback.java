/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.console.command.CommandOperation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public abstract class AeshConsoleCallback implements ConsoleCallback {

    private CountDownLatch latch;
    private List<CommandOperation> operations;
    private boolean resetOperations = false;

    public AeshConsoleCallback() {
    }

    @Override
    public List<CommandOperation> getInput() {
        reset();
        if(operations != null && operations.size() > 0) {
            resetOperations = true;
            return operations;
        }
        else {
            latch = new CountDownLatch(1);
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            resetOperations = true;

            return operations;
        }
    }

    @Override
    public void release() {
       if(latch != null && latch.getCount() > 0)
           latch.countDown();
    }

    @Override
    public void addCommandOperation(CommandOperation commandOperation) {
        if(operations == null)
            operations = new ArrayList<>(1);
        else {
            reset();
        }
        operations.add(commandOperation);
    }

    @Override
    public boolean isSleeping() {
        return (latch != null && latch.getCount() > 0);
    }

    private void reset() {
        if(resetOperations) {
            operations.clear();
            resetOperations = false;
        }
    }

}
