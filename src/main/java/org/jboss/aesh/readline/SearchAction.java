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
package org.jboss.aesh.readline;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface SearchAction extends Action {

    void input(Action action, KeyEvent key);

    boolean isSearching();

    enum Status {
        SEARCH_NOT_STARTED,
        SEARCH_EXIT,
        SEARCH_INPUT,
        SEARCH_INTERRUPT,
        SEARCH_END,
        SEARCH_PREV,
        SEARCH_NEXT,
        SEARCH_DELETE,
        SEARCH_MOVE_PREV,
        SEARCH_MOVE_NEXT,
        SEARCH_HISTORY_PREV,
        SEARCH_MOVE_RIGHT, SEARCH_MOVE_LEFT, SEARCH_HISTORY_NEXT
    }
}
