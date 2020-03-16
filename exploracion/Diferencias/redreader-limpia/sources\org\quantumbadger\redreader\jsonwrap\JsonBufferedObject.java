package org.quantumbadger.redreader.jsonwrap;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

public final class JsonBufferedObject extends JsonBuffered implements Iterable<Entry<String, JsonValue>> {
    private final HashMap<String, JsonValue> properties = new HashMap<>();

    /* access modifiers changed from: protected */
    public void buildBuffered(JsonParser jp2) throws IOException {
        while (true) {
            JsonToken nextToken = jp2.nextToken();
            JsonToken jt = nextToken;
            if (nextToken == JsonToken.END_OBJECT) {
                return;
            }
            if (jt == JsonToken.FIELD_NAME) {
                String fieldName = jp2.getCurrentName();
                JsonValue value = new JsonValue(jp2);
                synchronized (this) {
                    this.properties.put(fieldName, value);
                    notifyAll();
                }
                value.buildInThisThread();
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Expecting field name, got ");
                sb.append(jt.name());
                throw new JsonParseException(sb.toString(), jp2.getCurrentLocation());
            }
        }
        while (true) {
        }
    }

    public JsonValue get(String name) throws InterruptedException, IOException {
        synchronized (this) {
            while (getStatus() == 0 && !this.properties.containsKey(name)) {
                wait();
            }
            if (getStatus() == 2) {
                if (!this.properties.containsKey(name)) {
                    if (getStatus() == 2) {
                        throwFailReasonException();
                    }
                    return null;
                }
            }
            JsonValue jsonValue = (JsonValue) this.properties.get(name);
            return jsonValue;
        }
    }

    public String getString(String name) throws InterruptedException, IOException {
        JsonValue jsonValue = get(name);
        if (jsonValue == null) {
            return null;
        }
        return jsonValue.asString();
    }

    public Long getLong(String name) throws InterruptedException, IOException {
        return get(name).asLong();
    }

    public Double getDouble(String name) throws InterruptedException, IOException {
        return get(name).asDouble();
    }

    public Boolean getBoolean(String name) throws InterruptedException, IOException {
        return get(name).asBoolean();
    }

    public JsonBufferedObject getObject(String name) throws InterruptedException, IOException {
        return get(name).asObject();
    }

    public <E> E getObject(String name, Class<E> clazz) throws InterruptedException, IOException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        return get(name).asObject(clazz);
    }

    public JsonBufferedArray getArray(String name) throws InterruptedException, IOException {
        return get(name).asArray();
    }

    /* access modifiers changed from: protected */
    public void prettyPrint(int indent, StringBuilder sb) throws InterruptedException, IOException {
        if (join() != 1) {
            throwFailReasonException();
        }
        sb.append('{');
        Set<String> propertyKeySet = this.properties.keySet();
        String[] fieldNames = (String[]) propertyKeySet.toArray(new String[propertyKeySet.size()]);
        for (int prop = 0; prop < fieldNames.length; prop++) {
            if (prop != 0) {
                sb.append(',');
            }
            sb.append(10);
            for (int i = 0; i < indent + 1; i++) {
                sb.append("   ");
            }
            sb.append("\"");
            sb.append(fieldNames[prop].replace("\\", "\\\\").replace("\"", "\\\""));
            sb.append("\": ");
            ((JsonValue) this.properties.get(fieldNames[prop])).prettyPrint(indent + 1, sb);
        }
        sb.append(10);
        for (int i2 = 0; i2 < indent; i2++) {
            sb.append("   ");
        }
        sb.append('}');
    }

    public <E> E asObject(Class<E> clazz) throws InstantiationException, IllegalAccessException, InterruptedException, IOException, NoSuchMethodException, InvocationTargetException {
        E obj = clazz.getConstructor(new Class[0]).newInstance(new Object[0]);
        populateObject(obj);
        return obj;
    }

    /* JADX WARNING: type inference failed for: r7v31 */
    /* JADX WARNING: type inference failed for: r5v7 */
    /* JADX WARNING: Multi-variable type inference failed */
    /* JADX WARNING: Unknown variable types count: 1 */
    public void populateObject(Object o) throws InterruptedException, IOException, IllegalArgumentException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        Field[] objectFields;
        JsonValue val;
        Object result;
        if (join() != 1) {
            throwFailReasonException();
        }
        try {
            for (Field objectField : o.getClass().getFields()) {
                if ((objectField.getModifiers() & 128) == 0) {
                    if (this.properties.containsKey(objectField.getName())) {
                        val = (JsonValue) this.properties.get(objectField.getName());
                    } else {
                        val = objectField.getName().startsWith("_json_") ? (JsonValue) this.properties.get(objectField.getName().substring("_json_".length())) : 0;
                    }
                    if (val != 0) {
                        objectField.setAccessible(true);
                        Class<?> fieldType = objectField.getType();
                        if (fieldType != Long.class) {
                            if (fieldType != Long.TYPE) {
                                if (fieldType != Double.class) {
                                    if (fieldType != Double.TYPE) {
                                        Object obj = null;
                                        if (fieldType != Integer.class) {
                                            if (fieldType != Integer.TYPE) {
                                                if (fieldType != Float.class) {
                                                    if (fieldType != Float.TYPE) {
                                                        if (fieldType != Boolean.class) {
                                                            if (fieldType != Boolean.TYPE) {
                                                                if (fieldType == String.class) {
                                                                    objectField.set(o, val.asString());
                                                                } else if (fieldType == JsonBufferedArray.class) {
                                                                    objectField.set(o, val.asArray());
                                                                } else if (fieldType == JsonBufferedObject.class) {
                                                                    objectField.set(o, val.asObject());
                                                                } else if (fieldType == JsonValue.class) {
                                                                    objectField.set(o, val);
                                                                } else if (fieldType == Object.class) {
                                                                    switch (val.getType()) {
                                                                        case 3:
                                                                            result = val.asBoolean();
                                                                            break;
                                                                        case 4:
                                                                            result = val.asString();
                                                                            break;
                                                                        case 5:
                                                                            result = val.asDouble();
                                                                            break;
                                                                        case 6:
                                                                            result = val.asLong();
                                                                            break;
                                                                        default:
                                                                            result = val;
                                                                            break;
                                                                    }
                                                                    objectField.set(o, result);
                                                                } else {
                                                                    objectField.set(o, val.asObject(fieldType));
                                                                }
                                                            }
                                                        }
                                                        objectField.set(o, val.asBoolean());
                                                    }
                                                }
                                                if (!val.isNull()) {
                                                    obj = Float.valueOf(val.asDouble().floatValue());
                                                }
                                                objectField.set(o, obj);
                                            }
                                        }
                                        if (!val.isNull()) {
                                            obj = Integer.valueOf(val.asLong().intValue());
                                        }
                                        objectField.set(o, obj);
                                    }
                                }
                                objectField.set(o, val.asDouble());
                            }
                        }
                        objectField.set(o, val.asLong());
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public Iterator<Entry<String, JsonValue>> iterator() {
        try {
            join();
            return this.properties.entrySet().iterator();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
