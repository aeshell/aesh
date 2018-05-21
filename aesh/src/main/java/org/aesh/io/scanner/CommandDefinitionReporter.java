package org.aesh.io.scanner;

import org.aesh.command.CommandDefinition;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class CommandDefinitionReporter implements AnnotationDetector.TypeReporter  {

    private List<String> commands;

    public CommandDefinitionReporter() {
        commands = new ArrayList<>();
    }

    public List<String> getCommands() {
        return commands;
    }

    @Override
    public void reportTypeAnnotation(Class<? extends Annotation> annotation, String className) {
        //only load commands that's outside of org.aesh
        if(!className.startsWith("org.aesh"))
            commands.add(className);
    }

    @Override
    public Class<? extends Annotation>[] annotations() {
        try {
            return new Class[]{ Class.forName(CommandDefinition.class.getCanonicalName())};
        }
        catch (ClassNotFoundException e) {
            return null;
        }
    }
}
