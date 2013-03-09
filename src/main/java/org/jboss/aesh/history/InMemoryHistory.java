/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
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
public class InMemoryHistory implements History {

    private List<String> historyList;
    private int lastFetchedId = -1;
    private int lastSearchedId = 0;
    private String current;
    private SearchDirection searchDirection = SearchDirection.REVERSE;
    private int maxSize;

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
            if(historyList.contains(entry.trim())) {
               historyList.remove(entry.trim());
            }
            else {
                if(historyList.size() >= maxSize)
                    historyList.remove(0);
            }
            historyList.add(entry.trim());
            lastFetchedId = size();
            lastSearchedId = 0;
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
        //lastFetchedId = index;
        return historyList.get(index);
    }

   @Override
    public int size() {
       return historyList.size();
   }

    @Override
    public void setSearchDirection(SearchDirection direction) {
        searchDirection = direction;
    }

    @Override
    public SearchDirection getSearchDirection() {
        return searchDirection;
    }

    @Override
    public String getPreviousFetch() {
        if(size() < 1)
            return null;

        if(lastFetchedId > 0)
            return get(--lastFetchedId);
        else {
            return get(lastFetchedId);
        }
    }

    @Override
    public String getNextFetch() {
        if(size() < 1)
            return null;

        if(lastFetchedId < size()-1)
            return get(++lastFetchedId);
        else if(lastFetchedId == size()-1) {
            lastFetchedId++;
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
        if(lastSearchedId <= 0)
            lastSearchedId = size()-1;

        for(; lastSearchedId >= 0; lastSearchedId--)
            if(historyList.get(lastSearchedId).contains(search))
                return get(lastSearchedId);

        return null;
    }

    private String searchForward(String search) {
        if(lastSearchedId >= size())
            lastSearchedId = 0;

        for(; lastSearchedId < size(); lastSearchedId++ ) {
            if(historyList.get(lastSearchedId).contains(search))
                return get(lastSearchedId);
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
        lastFetchedId = -1;
        lastSearchedId = 0;
        historyList.clear();
        current = "";
    }

    @Override
    public void stop() {
        //does nothing for in-memory atm
    }
}
