package org.quantumbadger.redreader.jsonwrap;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

public final class JsonBufferedArray extends JsonBuffered implements Iterable<JsonValue> {
    /* access modifiers changed from: private */
    public final ArrayList<JsonValue> contents = new ArrayList<>(16);
    /* access modifiers changed from: private */
    public int items = 0;

    private class JsonBufferedArrayIterator implements Iterator<JsonValue> {
        private int currentId;

        private JsonBufferedArrayIterator() {
            this.currentId = 0;
        }

        public boolean hasNext() {
            boolean z;
            synchronized (JsonBufferedArray.this) {
                while (JsonBufferedArray.this.getStatus() == 0 && JsonBufferedArray.this.items <= this.currentId) {
                    try {
                        JsonBufferedArray.this.wait();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e2) {
                        throw new RuntimeException(e2);
                    }
                }
                if (JsonBufferedArray.this.getStatus() == 2) {
                    JsonBufferedArray.this.throwFailReasonException();
                }
                z = JsonBufferedArray.this.items > this.currentId;
            }
            return z;
        }

        public JsonValue next() {
            ArrayList access$200 = JsonBufferedArray.this.contents;
            int i = this.currentId;
            this.currentId = i + 1;
            return (JsonValue) access$200.get(i);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /* access modifiers changed from: protected */
    public void buildBuffered(JsonParser jp2) throws IOException {
        while (true) {
            JsonToken nextToken = jp2.nextToken();
            JsonToken jt = nextToken;
            if (nextToken != JsonToken.END_ARRAY) {
                JsonValue value = new JsonValue(jp2, jt);
                synchronized (this) {
                    this.contents.add(value);
                    this.items++;
                    notifyAll();
                }
                value.buildInThisThread();
            } else {
                return;
            }
        }
        while (true) {
        }
    }

    public JsonValue get(int id) throws InterruptedException, IOException {
        JsonValue jsonValue;
        if (id >= 0) {
            synchronized (this) {
                while (getStatus() == 0 && this.items <= id) {
                    wait();
                }
                if (getStatus() == 2) {
                    if (this.items <= id) {
                        if (getStatus() == 2) {
                            throwFailReasonException();
                        }
                        throw new ArrayIndexOutOfBoundsException(id);
                    }
                }
                jsonValue = (JsonValue) this.contents.get(id);
            }
            return jsonValue;
        }
        throw new ArrayIndexOutOfBoundsException(id);
    }

    public String getString(int id) throws InterruptedException, IOException {
        return get(id).asString();
    }

    public Long getLong(int id) throws InterruptedException, IOException {
        return get(id).asLong();
    }

    public Double getDouble(int id) throws InterruptedException, IOException {
        return get(id).asDouble();
    }

    public Boolean getBoolean(int id) throws InterruptedException, IOException {
        return get(id).asBoolean();
    }

    public JsonBufferedObject getObject(int id) throws InterruptedException, IOException {
        return get(id).asObject();
    }

    public <E> E getObject(int id, Class<E> clazz) throws InterruptedException, IOException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        return get(id).asObject(clazz);
    }

    public JsonBufferedArray getArray(int id) throws InterruptedException, IOException {
        return get(id).asArray();
    }

    public Iterator<JsonValue> iterator() {
        return new JsonBufferedArrayIterator();
    }

    /* access modifiers changed from: protected */
    public void prettyPrint(int indent, StringBuilder sb) throws InterruptedException, IOException {
        if (join() != 1) {
            throwFailReasonException();
        }
        sb.append('[');
        for (int item = 0; item < this.contents.size(); item++) {
            if (item != 0) {
                sb.append(',');
            }
            sb.append(10);
            for (int i = 0; i < indent + 1; i++) {
                sb.append("   ");
            }
            ((JsonValue) this.contents.get(item)).prettyPrint(indent + 1, sb);
        }
        sb.append(10);
        for (int i2 = 0; i2 < indent; i2++) {
            sb.append("   ");
        }
        sb.append(']');
    }

    public int getCurrentItemCount() {
        return this.items;
    }
}
