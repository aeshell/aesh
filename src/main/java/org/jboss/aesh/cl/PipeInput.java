package org.jboss.aesh.cl;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface PipeInput {

    Type type() default Type.STANDARD;

    public enum Type {
        STANDARD, ERROR
    }
}
