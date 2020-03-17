/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.util.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class Settings {
    final private Map<String, String> arguments = new HashMap<>();
    final private List<String> directArguments = new ArrayList<>();

    private void parseArguments(String[] args) {
        String key = null;

        for (String arg : args) {
            if (arg.startsWith("--")) {
                if (key != null) {
                    arguments.put(key, "");
                }

                key = arg.substring(2);
            } else if (key != null) {
                arguments.put(key, arg);
                key = null;
            } else {
                directArguments.add(arg);
            }
        }

        if (key != null) {
            arguments.put(key, "");
        }
    }

    public Properties getProperties() {
        return properties;
    }

    public Settings(String[] args) {
        this(System.getProperties());
        parseArguments(args);
    }
    public Settings(Properties properties, String[] args) {
        this.properties = properties;
        parseArguments(args);
    }

    public Settings(Properties properties) {
        this.properties = properties;
    }

    public Settings() {
        this(System.getProperties());
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Property {
        String value();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Argument {
        String value();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface DirectArgument {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface HasProperties {
    }

    private final Properties properties;

    public void addProperties(Properties overrideProperties) {
        for (String key : overrideProperties.stringPropertyNames()) {
            properties.setProperty(key, overrideProperties.getProperty(key));
        }
    }

    public boolean addFileIfExists(File propertiesFile) {
        Properties overrideProperties = new Properties();

        if (propertiesFile.isFile()) {
            try {
                overrideProperties.load(new FileInputStream(propertiesFile));
                addProperties(overrideProperties);
                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return false;
    }

    private static Method checkStringMethod(Method method) {
        if (method.getParameterCount() == 1
                && method.getParameterTypes()[0] == String.class) {
            return method;
        } else {
            throw new UnsupportedOperationException("Unsupported parameter type");
        }
    }

    public void setProperties(Object obj) throws IllegalAccessException, InvocationTargetException
    {
        for (Method method : obj.getClass().getMethods()) {
            {
                Property annotation;

                if ((annotation = method.getDeclaredAnnotation(Property.class)) != null) {
                    if (properties.containsKey(annotation.value())) {
                        checkStringMethod(method).invoke(obj, properties.get(annotation.value()).toString());
                    }
                }
            }

            {
                Argument annotation;

                if ((annotation = method.getDeclaredAnnotation(Argument.class)) != null) {
                    if (arguments.containsKey(annotation.value())) {
                        checkStringMethod(method).invoke(obj, arguments.get(annotation.value()));
                    }
                }
            }

            {
                DirectArgument annotation;

                if ((annotation = method.getDeclaredAnnotation(DirectArgument.class)) != null) {
                    for (String value : directArguments) {
                        checkStringMethod(method).invoke(obj, value);
                    }
                }
            }

            if (method.getDeclaredAnnotation(HasProperties.class) != null) {
                if (method.getParameterCount() == 0) {
                    setProperties(method.invoke(obj));
                } else {
                    throw new UnsupportedOperationException("Unsupported parameter type");
                }
            }
        }
    }
}
