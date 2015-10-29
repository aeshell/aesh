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

import java.util.ArrayList;
import java.util.List;

/**
 * A simple in-memory history implementation
 * By default max size is 500
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class InMemoryHistory extends History {

    private final List<String> historyList;
    private int lastId = 0;
    private String current;
    private SearchDirection searchDirection = SearchDirection.REVERSE;
    private final int maxSize;
    private String lastSearchArgument;

    public InMemoryHistory() {
        this(500);
    }

    public InMemoryHistory(int maxSize) {
        if(maxSize == -1)
            this.maxSize = Integer.MAX_VALUE;
        else
            this.maxSize = maxSize;
        historyList = new ArrayList<String>();
        current = "";
    }

    @Override
    public void push(String entry) {
        if(entry != null && entry.trim().length() > 0) {
            // Don't add repeated lines to the history
            if(historyList.size() > 0 && historyList.get(historyList.size()-1).equals(entry.trim())) {
               return;
            }

            if(historyList.size() >= maxSize) {
                historyList.remove(0);
            }

            historyList.add(entry.trim());
            lastId = size();
        }
    }

    @Override
    public String find(String search) {
        int index = historyList.indexOf(search);
        if(index >= 0) {
            return get(index);
        }
        else
            return null;

    }

    @Override
    public String get(int index) {
        return historyList.get(index);
    }

   @Override
    public int size() {
       return historyList.size();
   }

    @Override
    public void setSearchDirection(SearchDirection direction) {
        if(searchDirection != direction) {
            searchDirection = direction;
            lastSearchArgument = null;
            lastId = 0;
        }
    }

    @Override
    public SearchDirection getSearchDirection() {
        return searchDirection;
    }

    @Override
    public String getPreviousFetch() {
        if(size() < 1)
            return null;

        if(lastId > 0)
            return get(--lastId);
        else {
            return get(lastId);
        }
    }

    @Override
    public String getNextFetch() {
        if(size() < 1)
            return null;

        if(lastId < size()-1)
            return get(++lastId);
        else if(lastId == size()-1) {
            lastId++;
            return getCurrent();
        }
        else
            return getCurrent();
    }

    @Override
    public String search(String search) {
        if(searchDirection == SearchDirection.REVERSE)
            return searchReverse(search);
        else
            return searchForward(search);
    }

    private String searchReverse(String search) {
        if(lastId <= 0 || lastId > size()-1)
            lastId = size()-1;
        else if(lastSearchArgument != null && lastSearchArgument.equals(search))
            lastId--;

        for(; lastId >= 0; lastId--)
            if(historyList.get(lastId).contains(search)) {
                lastSearchArgument = search;
                return get(lastId);
            }

        return null;
    }

    private String searchForward(String search) {
        if(lastId >= size())
            lastId = 0;
        else if(lastSearchArgument != null && lastSearchArgument.equals(search))
          lastId++;

        for(; lastId < size(); lastId++ ) {
            if(historyList.get(lastId).contains(search)) {
                lastSearchArgument = search;
                return get(lastId);
            }
        }
        return null;
    }

    @Override
    public void setCurrent(String line) {
        this.current = line;
    }

    @Override
    public String getCurrent() {
        return current;
    }

    @Override
    public List<String> getAll() {
        return historyList;
    }

    @Override
    public void clear() {
        lastId = 0;
        historyList.clear();
        current = "";
    }

    @Override
    public void stop() {
        //does nothing for in-memory atm
    }
}
