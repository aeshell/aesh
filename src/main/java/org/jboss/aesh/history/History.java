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
package org.jboss.aesh.history;

import java.util.List;

/**
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public abstract class History {

    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable(){
        this.enabled = false;
    }

    public abstract void push(String entry);

    public abstract String find(String search);

    public abstract String get(int index);

    public abstract int size();

    public abstract void setSearchDirection(SearchDirection direction);

    public abstract SearchDirection getSearchDirection();

    public abstract String getNextFetch();

    public abstract String getPreviousFetch();

    public abstract String search(String search);

    public abstract void setCurrent(String line);

    public abstract String getCurrent();

    public abstract List<String> getAll();

    public abstract void clear();

    public abstract void stop();

}
