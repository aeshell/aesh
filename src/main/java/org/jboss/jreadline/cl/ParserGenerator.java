/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.jreadline.cl;

import org.jboss.jreadline.cl.internal.OptionInt;
import org.jboss.jreadline.cl.internal.ParameterInt;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParserGenerator {


    public static CommandLineParser generateParser(Class<?> clazz) {

        Parameter param = clazz.getAnnotation(Parameter.class);
        if(param == null)
            throw new RuntimeException("Can only create parser from class thats annotated with Parameter");

        if(param.options() != null) {
            OptionInt[] options = new OptionInt[param.options().length];
            for(int i=0; i < param.options().length; i++) {
                Option o = param.options()[i];
                options[i] =  new OptionInt(
                        o.name(), o.longName(), o.description(),
                        o.hasValue(), o.argument(), o.required(), o.valueSeparator(),
                        o.isProperty(), null);
            }

            return new CommandLineParser(new ParameterInt(param.name(), param.usage(), options));
        }
        else
            return new CommandLineParser(new ParameterInt(param.name(), param.usage(), new OptionInt[0]));

    }
}
