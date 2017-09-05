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

/**
 * <code>FileAccessPermission</code> defines file access permission like readable, writable.
 *
 * <i>readable</i> and <i>writable</i> will be true by default.
 *
 * @author Lin Gao <lgao@redhat.com>
 *
 */
public class FileAccessPermission {

    private boolean readable = true;

    private boolean readableOwnerOnly;

    private boolean writable = true;

    private boolean writableOwnerOnly;

    private boolean executable = false;

    private boolean executableOwnerOnly;

    /**
     * Default constructor
     */
    public FileAccessPermission() {
        super();
    }

    /**
     * @return the executable
     */
    public boolean isExecutable() {
        return executable;
    }

    /**
     * @param executable the executable to set
     */
    public void setExecutable(boolean executable) {
        this.executable = executable;
    }

    /**
     * @return the executableOwnerOnly
     */
    public boolean isExecutableOwnerOnly() {
        return executableOwnerOnly;
    }

    /**
     * @param executableOwnerOnly the executableOwnerOnly to set
     */
    public void setExecutableOwnerOnly(boolean executableOwnerOnly) {
        this.executableOwnerOnly = executableOwnerOnly;
    }

    /**
     * @return the readable
     */
    public boolean isReadable() {
        return readable;
    }

    /**
     * @param readable the readable to set
     */
    public void setReadable(boolean readable) {
        this.readable = readable;
    }

    /**
     * @return the readableOwnerOnly
     */
    public boolean isReadableOwnerOnly() {
        return readableOwnerOnly;
    }

    /**
     * @param readableOwnerOnly the readableOwnerOnly to set
     */
    public void setReadableOwnerOnly(boolean readableOwnerOnly) {
        this.readableOwnerOnly = readableOwnerOnly;
    }

    /**
     * @return the writable
     */
    public boolean isWritable() {
        return writable;
    }

    /**
     * @param writable the writable to set
     */
    public void setWritable(boolean writable) {
        this.writable = writable;
    }

    /**
     * @return the writableOwnerOnly
     */
    public boolean isWritableOwnerOnly() {
        return writableOwnerOnly;
    }

    /**
     * @param writableOwnerOnly the writableOwnerOnly to set
     */
    public void setWritableOwnerOnly(boolean writableOwnerOnly) {
        this.writableOwnerOnly = writableOwnerOnly;
    }

}
