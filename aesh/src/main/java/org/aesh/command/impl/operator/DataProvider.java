/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.command.impl.operator;

import java.io.BufferedInputStream;

/**
 *
 * @author Aesh team
 */
public interface DataProvider {
    BufferedInputStream getData();

    /**
     * Closes the pipe input for the consumer. Providers that hand out a
     * shared cached stream close that stream; providers that never created
     * one only signal that the consumer is gone, without manufacturing a
     * reader merely to close it.
     */
    default void closeData() {
    }
}
