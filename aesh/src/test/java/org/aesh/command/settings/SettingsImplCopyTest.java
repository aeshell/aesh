/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.aesh.command.settings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.aesh.command.invocation.CommandInvocation;
import org.aesh.console.AeshContext;
import org.aesh.console.DefaultAeshContext;
import org.junit.Test;

/**
 * Copying settings must carry over an explicitly configured
 * {@link AeshContext} by identity, while a lazily defaulted context must
 * not be shared: the copy gets its own fresh default instead (#687).
 */
public class SettingsImplCopyTest {

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void explicitContextIsCarriedOverByIdentity() {
        AeshContext custom = new DefaultAeshContext();
        Settings<CommandInvocation> base = SettingsBuilder.builder().aeshContext(custom).build();
        assertTrue(base.hasExplicitAeshContext());

        Settings<CommandInvocation> copy = new SettingsImpl<>(base);

        assertTrue(copy.hasExplicitAeshContext());
        assertSame("explicitly configured context must be shared with the copy",
                custom, copy.aeshContext());
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void nonExplicitContextIsNotShared() {
        Settings<CommandInvocation> base = SettingsBuilder.builder().build();
        assertFalse(base.hasExplicitAeshContext());
        AeshContext baseDefault = base.aeshContext();
        assertNotNull(baseDefault);

        Settings<CommandInvocation> copy = new SettingsImpl<>(base);

        assertFalse(copy.hasExplicitAeshContext());
        assertNotNull(copy.aeshContext());
        assertNotSame("lazily defaulted context must not leak from base into copy",
                baseDefault, copy.aeshContext());
    }
}
