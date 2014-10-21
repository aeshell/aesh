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
package org.jboss.aesh.console.man.parser;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public enum SectionType {
    NAME("NAME"),
    SYNOPSIS("SYNOPSIS"),
    DESCRIPTION("DESCRIPTION"),
    OPTIONS("OPTIONS"),
    AUTHOR("AUTHOR"),
    RESOURCES("RESOURCES"),
    COPYING("COPYING"),
    NOT_SPECIFIED("NOT_SPECIFIED"),
    IGNORED("IGNORED");

    private String type;

    SectionType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static SectionType getSectionType(String name) {
        if(NAME.getType().equals(name))
            return NAME;
        else if(SYNOPSIS.getType().equals(name))
            return SYNOPSIS;
        else if(DESCRIPTION.getType().equals(name))
            return DESCRIPTION;
        else if(OPTIONS.getType().equals(name))
            return OPTIONS;
        else if(AUTHOR.getType().equals(name))
            return AUTHOR;
        else if(RESOURCES.getType().equals(name))
            return RESOURCES;
        else if(COPYING.getType().equals(name))
            return COPYING;
        else
            return NOT_SPECIFIED;
    }

}
