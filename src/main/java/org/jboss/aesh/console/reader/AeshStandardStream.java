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
 */ackage org.jboss.aesh.console.reader;

import java.io.BufferedInputStream;

/**
 * Pipes the standard input and standard error
 * to the current running command
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshStandardStream {

    private BufferedInputStream stdIn;
    private BufferedInputStream stdError;

    public AeshStandardStream() {
        stdIn = new BufferedInputStream(null);
        stdError = new BufferedInputStream(null);
    }

    public AeshStandardStream(BufferedInputStream stdIn) {
        this.stdIn = stdIn;
        stdError = new BufferedInputStream(null);
    }

    public AeshStandardStream(BufferedInputStream stdIn, BufferedInputStream stdError) {
        this.stdIn = stdIn;
        this.stdError = stdError;
    }

    public BufferedInputStream getStdIn() {
        return stdIn;
    }

    public void setStdIn(BufferedInputStream stdIn) {
        this.stdIn = stdIn;
    }

    public BufferedInputStream getStdError() {
        return stdError;
    }

    public void setStdError(BufferedInputStream stdError) {
        this.stdError = stdError;
    }
}
