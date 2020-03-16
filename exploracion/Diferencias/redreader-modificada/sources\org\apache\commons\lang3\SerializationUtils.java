package org.apache.commons.lang3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SerializationUtils {

    static class ClassLoaderAwareObjectInputStream extends ObjectInputStream {
        private static final Map<String, Class<?>> primitiveTypes = new HashMap();
        private final ClassLoader classLoader;

        static {
            primitiveTypes.put("byte", Byte.TYPE);
            primitiveTypes.put("short", Short.TYPE);
            primitiveTypes.put("int", Integer.TYPE);
            primitiveTypes.put("long", Long.TYPE);
            primitiveTypes.put("float", Float.TYPE);
            primitiveTypes.put("double", Double.TYPE);
            primitiveTypes.put("boolean", Boolean.TYPE);
            primitiveTypes.put("char", Character.TYPE);
            primitiveTypes.put("void", Void.TYPE);
        }

        ClassLoaderAwareObjectInputStream(InputStream in, ClassLoader classLoader2) throws IOException {
            super(in);
            this.classLoader = classLoader2;
        }

        /* access modifiers changed from: protected */
        public Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            String name = desc.getName();
            try {
                return Class.forName(name, false, this.classLoader);
            } catch (ClassNotFoundException e) {
                try {
                    return Class.forName(name, false, Thread.currentThread().getContextClassLoader());
                } catch (ClassNotFoundException cnfe) {
                    Class<?> cls = (Class) primitiveTypes.get(name);
                    if (cls != null) {
                        return cls;
                    }
                    throw cnfe;
                }
            }
        }
    }

    public static <T extends Serializable> T clone(T object) {
        ClassLoaderAwareObjectInputStream in;
        if (object == null) {
            return null;
        }
        try {
            in = new ClassLoaderAwareObjectInputStream(new ByteArrayInputStream(serialize(object)), object.getClass().getClassLoader());
            Serializable serializable = (Serializable) in.readObject();
            in.close();
            return serializable;
        } catch (ClassNotFoundException ex) {
            throw new SerializationException("ClassNotFoundException while reading cloned object data", ex);
        } catch (IOException ex2) {
            throw new SerializationException("IOException while reading or closing cloned object data", ex2);
        } catch (Throwable th) {
        }
        throw th;
    }

    public static <T extends Serializable> T roundtrip(T msg) {
        return (Serializable) deserialize(serialize(msg));
    }

    public static void serialize(Serializable obj, OutputStream outputStream) {
        ObjectOutputStream out;
        Validate.isTrue(outputStream != null, "The OutputStream must not be null", new Object[0]);
        try {
            out = new ObjectOutputStream(outputStream);
            out.writeObject(obj);
            out.close();
            return;
        } catch (IOException ex) {
            throw new SerializationException((Throwable) ex);
        } catch (Throwable th) {
        }
        throw th;
    }

    public static byte[] serialize(Serializable obj) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
        serialize(obj, baos);
        return baos.toByteArray();
    }

    public static <T> T deserialize(InputStream inputStream) {
        ObjectInputStream in;
        Validate.isTrue(inputStream != null, "The InputStream must not be null", new Object[0]);
        try {
            in = new ObjectInputStream(inputStream);
            T obj = in.readObject();
            in.close();
            return obj;
        } catch (IOException | ClassNotFoundException ex) {
            throw new SerializationException((Throwable) ex);
        } catch (Throwable th) {
        }
        throw th;
    }

    public static <T> T deserialize(byte[] objectData) {
        Validate.isTrue(objectData != null, "The byte[] must not be null", new Object[0]);
        return deserialize((InputStream) new ByteArrayInputStream(objectData));
    }
}
