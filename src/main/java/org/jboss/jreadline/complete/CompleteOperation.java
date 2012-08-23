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
        sb.append(", candidates:"+completionCandidates);
        
        return sb.toString();
    }

}
