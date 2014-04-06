package org.jboss.aesh.cl.internal;

import org.jboss.aesh.cl.PipeInput;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ProcessedPipeline {
   private String name;
    private Class<?> type;

    public ProcessedPipeline(String name, Class<?> type, PipeInput.Type pipeinputType) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public void injectValuesIntoPipeline(Object instance, String value) {
        try {
            Field field = instance.getClass().getDeclaredField(name);
            if(!Modifier.isPublic(field.getModifiers()))
                field.setAccessible(true);
            if(!Modifier.isPublic(instance.getClass().getModifiers())) {
                Constructor constructor = instance.getClass().getDeclaredConstructor();
                if(constructor != null)
                    constructor.setAccessible(true);
            }

            if(field.getType().isInterface() || Modifier.isAbstract(field.getType().getModifiers())) {
                if(Set.class.isAssignableFrom(field.getType())) {
                    Set tmpSet = new HashSet<Object>();
                    tmpSet.add(value);
                    field.set(instance, tmpSet);
                }
                else if(List.class.isAssignableFrom(field.getType())) {
                    List tmpList = new ArrayList();
                    tmpList.add(value);
                    field.set(instance, tmpList);
                }
                //todo: should support more that List/Set
            }
            else {
                Collection tmpInstance = (Collection) field.getType().newInstance();
                tmpInstance.add(value);
                field.set(instance, tmpInstance);
            }
        }
        catch (NoSuchMethodException e1) {
            e1.printStackTrace();
        }
        catch (InstantiationException e1) {
            e1.printStackTrace();
        }
        catch (IllegalAccessException e1) {
            e1.printStackTrace();
        }
        catch (NoSuchFieldException e1) {
            e1.printStackTrace();
        }

    }
}


