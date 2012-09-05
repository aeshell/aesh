/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.jreadline.complete;

import org.jboss.jreadline.util.Parser;

import java.util.ArrayList;
import java.util.List;

/**
 * A payload object to store completion data
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class CompleteOperation {
    private String buffer;
    private int cursor;
    private int offset;
    private List<String> completionCandidates;

    public CompleteOperation(String buffer, int cursor) {
        setBuffer(buffer);
        setCursor(cursor);
        completionCandidates = new ArrayList<String>();
    }

    public String getBuffer() {
        return buffer;
    }

    private void setBuffer(String buffer) {
        this.buffer = buffer;
    }

    public int getCursor() {
        return cursor;
    }

    private void setCursor(int cursor) {
        this.cursor = cursor;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public List<String> getCompletionCandidates() {
        return completionCandidates;
    }

    public void setCompletionCandidates(List<String> completionCandidates) {
        this.completionCandidates = completionCandidates;
    }

    public void addCompletionCandidate(String completionCandidate) {
        this.completionCandidates.add(completionCandidate);
    }

    public void addCompletionCandidates(List<String> completionCandidates) {
        this.completionCandidates.addAll(completionCandidates);
    }

    public void removeEscapedSpacesFromCompletionCandidates() {
        setCompletionCandidates(Parser.switchEscapedSpacesToSpacesInList(getCompletionCandidates()));
    }

    public List<String> getFormattedCompletionCandidates() {
        if(offset < cursor) {
            List<String> fixedCandidates = new ArrayList<String>(completionCandidates.size());
            int pos = cursor - offset;
            for(String c : completionCandidates) {
                if(c.length() >= pos)
                    fixedCandidates.add(c.substring(pos));
                else
                    fixedCandidates.add("");
            }
            return fixedCandidates;
        }
        else
            return completionCandidates;
    }

    public String getFormattedCompletion(String completion) {
        if(offset < cursor) {
            int pos = cursor - offset;
            if(completion.length() > pos)
                return completion.substring(pos);
            else
                return "";
        }
        else
            return completion;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Buffer: ").append(buffer).append(", Cursor:").append(cursor).append(", Offset:").append(offset);
        sb.append(", candidates:").append(completionCandidates);

        return sb.toString();
    }

}
