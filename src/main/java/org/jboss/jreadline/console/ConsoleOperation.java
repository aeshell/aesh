/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
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
package org.jboss.jreadline.console;

import org.jboss.jreadline.console.operator.ControlOperator;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ConsoleOperation {

    private ControlOperator controlOperator;
    private String buffer;


    public ConsoleOperation(ControlOperator controlOperator, String buffer) {
        this.controlOperator = controlOperator;
        this.buffer = buffer;
    }

    public String getBuffer() {
        return buffer;
    }

    public ControlOperator getControlOperator() {
        return controlOperator;
    }


    @Override
    public boolean equals(Object o) {
        if(o instanceof ConsoleOperation) {
            ConsoleOperation r = (ConsoleOperation) o;
            if(r.getBuffer().equals(getBuffer()) &&
                    r.getControlOperator().equals(getControlOperator()))
                return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 129384;
    }

    @Override
    public String toString() {
        return "ControlOperator: "+ getControlOperator()+", Buffer: "+buffer;
    }

}
