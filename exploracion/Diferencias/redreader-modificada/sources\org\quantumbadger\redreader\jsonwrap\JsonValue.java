package org.quantumbadger.redreader.jsonwrap;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

public final class JsonValue {
    public static final int TYPE_ARRAY = 1;
    public static final int TYPE_BOOLEAN = 3;
    public static final int TYPE_FLOAT = 5;
    public static final int TYPE_INTEGER = 6;
    public static final int TYPE_NULL = 2;
    public static final int TYPE_OBJECT = 0;
    public static final int TYPE_STRING = 4;

    /* renamed from: jp reason: collision with root package name */
    private JsonParser f22jp;
    private final int type;
    private final Object value;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {
    }

    public JsonValue(JsonParser jp2) throws IOException {
        this(jp2, jp2.nextToken());
    }

    public JsonValue(InputStream source) throws IOException {
        this(new JsonFactory().createParser(source));
    }

    public JsonValue(URL source) throws IOException {
        this(new JsonFactory().createParser(source));
    }

    public JsonValue(byte[] source) throws IOException {
        this(new JsonFactory().createParser(source));
    }

    public JsonValue(String source) throws IOException {
        this(new JsonFactory().createParser(source));
    }

    public JsonValue(File source) throws IOException {
        this(new JsonFactory().createParser(source));
    }

    public JsonValue(Reader source) throws IOException {
        this(new JsonFactory().createParser(source));
    }

    protected JsonValue(JsonParser jp2, JsonToken firstToken) throws IOException {
        this.f22jp = null;
        switch (firstToken) {
            case START_OBJECT:
                this.type = 0;
                this.value = new JsonBufferedObject();
                this.f22jp = jp2;
                return;
            case START_ARRAY:
                this.type = 1;
                this.value = new JsonBufferedArray();
                this.f22jp = jp2;
                return;
            case VALUE_FALSE:
                this.type = 3;
                this.value = Boolean.valueOf(false);
                return;
            case VALUE_TRUE:
                this.type = 3;
                this.value = Boolean.valueOf(true);
                return;
            case VALUE_NULL:
                this.type = 2;
                this.value = null;
                return;
            case VALUE_STRING:
                this.type = 4;
                this.value = jp2.getValueAsString();
                return;
            case VALUE_NUMBER_FLOAT:
                if (jp2.getValueAsDouble() == ((double) jp2.getValueAsLong())) {
                    this.type = 6;
                    this.value = Long.valueOf(jp2.getValueAsLong());
                    return;
                }
                this.type = 5;
                this.value = Double.valueOf(jp2.getValueAsDouble());
                return;
            case VALUE_NUMBER_INT:
                this.type = 6;
                this.value = Long.valueOf(jp2.getValueAsLong());
                return;
            default:
                throw new JsonParseException("Expecting an object, literal, or array", jp2.getCurrentLocation());
        }
    }

    public void buildInThisThread() throws IOException {
        int i = this.type;
        if (i == 0 || i == 1) {
            ((JsonBuffered) this.value).build(this.f22jp);
        }
        this.f22jp = null;
    }

    public int getType() {
        return this.type;
    }

    public boolean isNull() {
        return this.type == 2;
    }

    public JsonBufferedObject asObject() {
        if (this.type != 2) {
            return (JsonBufferedObject) this.value;
        }
        return null;
    }

    public <E> E asObject(Class<E> clazz) throws InstantiationException, IllegalAccessException, InterruptedException, IOException, NoSuchMethodException, InvocationTargetException {
        if (this.type != 2) {
            return asObject().asObject(clazz);
        }
        return null;
    }

    public JsonBufferedArray asArray() {
        if (this.type != 2) {
            return (JsonBufferedArray) this.value;
        }
        return null;
    }

    public Boolean asBoolean() {
        if (this.type != 2) {
            return (Boolean) this.value;
        }
        return null;
    }

    public String asString() {
        switch (this.type) {
            case 2:
                return null;
            case 3:
                return String.valueOf(asBoolean());
            case 5:
                return String.valueOf(asDouble());
            case 6:
                return String.valueOf(asLong());
            default:
                return (String) this.value;
        }
    }

    public Double asDouble() {
        int i = this.type;
        if (i == 2) {
            return null;
        }
        if (i == 4) {
            return Double.valueOf(Double.parseDouble(asString()));
        }
        if (i != 6) {
            return (Double) this.value;
        }
        return Double.valueOf(((Long) this.value).doubleValue());
    }

    public Long asLong() {
        int i = this.type;
        if (i == 2) {
            return null;
        }
        switch (i) {
            case 4:
                return Long.valueOf(Long.parseLong(asString()));
            case 5:
                return Long.valueOf(((Double) this.value).longValue());
            default:
                return (Long) this.value;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        try {
            prettyPrint(0, sb);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        return sb.toString();
    }

    /* access modifiers changed from: protected */
    public void prettyPrint(int indent, StringBuilder sb) throws InterruptedException, IOException {
        switch (this.type) {
            case 0:
            case 1:
                ((JsonBuffered) this.value).prettyPrint(indent, sb);
                return;
            case 2:
                sb.append("null");
                return;
            case 3:
                sb.append(asBoolean());
                return;
            case 4:
                sb.append("\"");
                sb.append(asString().replace("\\", "\\\\").replace("\"", "\\\""));
                sb.append("\"");
                return;
            case 5:
                sb.append(asDouble());
                return;
            case 6:
                sb.append(asLong());
                return;
            default:
                return;
        }
    }

    public void join() throws InterruptedException {
        if (getType() == 1 || getType() == 0) {
            ((JsonBuffered) this.value).join();
        }
    }
}
