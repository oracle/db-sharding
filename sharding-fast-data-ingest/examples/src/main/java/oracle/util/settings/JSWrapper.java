/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.util.settings;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.*;
import java.io.Closeable;
import java.io.Reader;
import java.util.*;
import java.util.function.Function;

public class JSWrapper {
    private Object object;

    public JSWrapper(Object object) {
        this.object = object;
    }

    private static JSWrapper NULL_OBJECT = new JSWrapper(null);

    public static JSWrapper nullObject() { return NULL_OBJECT; }

    public static JSWrapper of(Object x) {
        return (x == null) ? NULL_OBJECT :
                ((x instanceof JSWrapper) ? (JSWrapper) x : new JSWrapper(x) );
    }

    public boolean asBoolean()
    {
        if (object == null) { return false; }
        else if (object instanceof String) {
            String x = (String) object;
            return x.equalsIgnoreCase("yes") || x.equalsIgnoreCase("true") || x.equalsIgnoreCase("1");
        }
        else if (object instanceof Boolean) { return (Boolean) object; }
        else if (object instanceof Integer) { return ((Integer) object) != 0; }
        else if (object instanceof Long)    { return ((Long) object) != 0; }
        else if (isFunction()) { return callSelf().asBoolean(); }
        else if (isSingletonArray()) { return get(0).get().asBoolean(); }

        return true;
    }

    public Number asNumber(Number defaultValue)
    {
        if (object == null) { return defaultValue; }
        else if (object instanceof String)  { return Long.parseLong((String) object); }
        else if (object instanceof Boolean) { return (Boolean) object ? 1 : 0; }
        else if (object instanceof Number)  { return (Number) object; }
        else if (isFunction()) { return callSelf().asNumber(defaultValue); }
        else if (isSingletonArray()) { return get(0).get().asNumber(defaultValue); }

        return defaultValue;
    }

    public String asString()
    {
        if (object == null) { return ""; }
        else if (isFunction()) { return callSelf().asString(); }
        else if (isSingletonArray()) { return get(0).get().asString(); }
/*
        else if (object instanceof ScriptObjectMirror)
        {
            ScriptObjectMirror scriptObject = (ScriptObjectMirror) object;

            if (scriptObject.isArray() && scriptObject.values().size() == 1) {
                return scriptObject.values().iterator().next().toString();
            }
        }
*/
        return object.toString();
    }

    public Collection<Object> asCollection()
    {
        if (object != null)
        {
            if (isFunction()) {
                return callSelf().asCollection();
            } else if (isArray()) {
                return ((ScriptObjectMirror) object).values();
            } else if (object instanceof Object[]) {
                return Arrays.asList((Object[]) object);
            } else if (object instanceof Collection) {
                return (Collection<Object>) object;
            } else {
                return Collections.singletonList(object);
            }
        }

        return Collections.emptyList();
    }

    public Map<String, Object> asMap() {
        if (object instanceof ScriptObjectMirror) {
            return (ScriptObjectMirror) object;
        } else {
            return Collections.emptyMap();
        }
    }

    public <T, R> Function<T, R> asFunction() {
        return asFunction(null);
    }

    public <T, R> Function<T, R> asFunction(Object thiz) {
        if (object instanceof Function) {
            return (Function) object;
        } else if (isFunction()) {
            return x -> (R) this.callOrGetValue(extractValue(thiz), x);
        } else {
            return x -> null;
        }
    }

    public Object getValue() { return object; }

    public static Object extractValue(Object x) {
        return x instanceof JSWrapper ? ((JSWrapper) x).getValue() : x;
    }

    public JSWrapper call(Object thiz, Object ... args)
    {
        return new JSWrapper(mustBeFunction().call(extractValue(thiz), args));
    }

    public JSWrapper callOrGetValue(Object thiz, Object ... args) {
        return isFunction() ? call(extractValue(thiz), args) : this;
    }

    public JSWrapper callSelf(Object ... args)
    {
        return new JSWrapper(mustBeFunction().call(object, args));
    }

    public boolean isFunction() {
        return object instanceof ScriptObjectMirror && ((ScriptObjectMirror) object).isFunction();
    }

    public boolean isArray() {
        return object instanceof ScriptObjectMirror && ((ScriptObjectMirror) object).isArray();
    }

    public Optional<JSWrapper> get(int i) {
        return isArray() ? Optional.of(new JSWrapper(((ScriptObjectMirror) object).get(i))) : Optional.empty();
    }

    public boolean isSingletonArray() {
        return isArray() && ((ScriptObjectMirror) object).size() == 1;
    }

    public Optional<JSWrapper> get(String key) {
        return object instanceof Map ? Optional.of(new JSWrapper(((Map) object).get(key))) : Optional.empty();
    }

    public ScriptObjectMirror mustBeFunction()
    {
        if (object == null || !isFunction()) {
            throw new RuntimeException("Function type expected");
        }

        return ((ScriptObjectMirror) object);
    }

    public static JSWrapper eval(Reader reader, Map<String, Object> stuff)
            throws ScriptException
    {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");

        try (GlobalMap globals = new GlobalMap(stuff)) {
            engine.eval(reader, globals);
        }

        return new JSWrapper(stuff);
    }

    public static JSWrapper eval(Reader reader)
            throws ScriptException
    {
        return JSWrapper.eval(reader, new HashMap<String, Object>());
    }

    public static class GlobalMap extends SimpleBindings implements Closeable {

        private final static String NASHORN_GLOBAL = "nashorn.global";
        private Bindings global;
        private Set<String> original_keys;

        public GlobalMap(Map<String, Object> map) {
            super(map);
        }

        @Override
        public Object put(String key, Object value) {
            if (key.equals(NASHORN_GLOBAL) && value instanceof Bindings) {
                global = (Bindings) value;
                original_keys = new HashSet<>(global.keySet());
                return null;
            }
            return super.put(key, value);
        }

        @Override
        public Object get(Object key) {
            return key.equals(NASHORN_GLOBAL) ? global : super.get(key);
        }

        @Override
        public void close() {
            if (global != null) {
                Set<String> keys = new HashSet<>(global.keySet());
                keys.removeAll(original_keys);
                for (String key : keys) {
                    put(key, global.remove(key));
                }
            }
        }
    }
}
