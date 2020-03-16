package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.core.type.WritableTypeId.Inclusion;
import com.fasterxml.jackson.core.util.VersionUtil;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class JsonGenerator implements Closeable, Flushable, Versioned {
    protected PrettyPrinter _cfgPrettyPrinter;

    public enum Feature {
        AUTO_CLOSE_TARGET(true),
        AUTO_CLOSE_JSON_CONTENT(true),
        FLUSH_PASSED_TO_STREAM(true),
        QUOTE_FIELD_NAMES(true),
        QUOTE_NON_NUMERIC_NUMBERS(true),
        WRITE_NUMBERS_AS_STRINGS(false),
        WRITE_BIGDECIMAL_AS_PLAIN(false),
        ESCAPE_NON_ASCII(false),
        STRICT_DUPLICATE_DETECTION(false),
        IGNORE_UNKNOWN(false);
        
        private final boolean _defaultState;
        private final int _mask;

        public static int collectDefaults() {
            Feature[] values;
            int flags = 0;
            for (Feature f : values()) {
                if (f.enabledByDefault()) {
                    flags |= f.getMask();
                }
            }
            return flags;
        }

        private Feature(boolean defaultState) {
            this._defaultState = defaultState;
            this._mask = 1 << ordinal();
        }

        public boolean enabledByDefault() {
            return this._defaultState;
        }

        public boolean enabledIn(int flags) {
            return (this._mask & flags) != 0;
        }

        public int getMask() {
            return this._mask;
        }
    }

    public abstract void close() throws IOException;

    public abstract JsonGenerator disable(Feature feature);

    public abstract JsonGenerator enable(Feature feature);

    public abstract void flush() throws IOException;

    public abstract ObjectCodec getCodec();

    public abstract int getFeatureMask();

    public abstract JsonStreamContext getOutputContext();

    public abstract boolean isClosed();

    public abstract boolean isEnabled(Feature feature);

    public abstract JsonGenerator setCodec(ObjectCodec objectCodec);

    @Deprecated
    public abstract JsonGenerator setFeatureMask(int i);

    public abstract JsonGenerator useDefaultPrettyPrinter();

    public abstract Version version();

    public abstract int writeBinary(Base64Variant base64Variant, InputStream inputStream, int i) throws IOException;

    public abstract void writeBinary(Base64Variant base64Variant, byte[] bArr, int i, int i2) throws IOException;

    public abstract void writeBoolean(boolean z) throws IOException;

    public abstract void writeEndArray() throws IOException;

    public abstract void writeEndObject() throws IOException;

    public abstract void writeFieldName(SerializableString serializableString) throws IOException;

    public abstract void writeFieldName(String str) throws IOException;

    public abstract void writeNull() throws IOException;

    public abstract void writeNumber(double d) throws IOException;

    public abstract void writeNumber(float f) throws IOException;

    public abstract void writeNumber(int i) throws IOException;

    public abstract void writeNumber(long j) throws IOException;

    public abstract void writeNumber(String str) throws IOException;

    public abstract void writeNumber(BigDecimal bigDecimal) throws IOException;

    public abstract void writeNumber(BigInteger bigInteger) throws IOException;

    public abstract void writeObject(Object obj) throws IOException;

    public abstract void writeRaw(char c) throws IOException;

    public abstract void writeRaw(String str) throws IOException;

    public abstract void writeRaw(String str, int i, int i2) throws IOException;

    public abstract void writeRaw(char[] cArr, int i, int i2) throws IOException;

    public abstract void writeRawUTF8String(byte[] bArr, int i, int i2) throws IOException;

    public abstract void writeRawValue(String str) throws IOException;

    public abstract void writeRawValue(String str, int i, int i2) throws IOException;

    public abstract void writeRawValue(char[] cArr, int i, int i2) throws IOException;

    public abstract void writeStartArray() throws IOException;

    public abstract void writeStartObject() throws IOException;

    public abstract void writeString(SerializableString serializableString) throws IOException;

    public abstract void writeString(String str) throws IOException;

    public abstract void writeString(char[] cArr, int i, int i2) throws IOException;

    public abstract void writeTree(TreeNode treeNode) throws IOException;

    public abstract void writeUTF8String(byte[] bArr, int i, int i2) throws IOException;

    protected JsonGenerator() {
    }

    public final JsonGenerator configure(Feature f, boolean state) {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }

    public JsonGenerator overrideStdFeatures(int values, int mask) {
        return setFeatureMask(((mask ^ -1) & getFeatureMask()) | (values & mask));
    }

    public int getFormatFeatures() {
        return 0;
    }

    public JsonGenerator overrideFormatFeatures(int values, int mask) {
        StringBuilder sb = new StringBuilder();
        sb.append("No FormatFeatures defined for generator of type ");
        sb.append(getClass().getName());
        throw new IllegalArgumentException(sb.toString());
    }

    public void setSchema(FormatSchema schema) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generator of type ");
        sb.append(getClass().getName());
        sb.append(" does not support schema of type '");
        sb.append(schema.getSchemaType());
        sb.append("'");
        throw new UnsupportedOperationException(sb.toString());
    }

    public FormatSchema getSchema() {
        return null;
    }

    public JsonGenerator setPrettyPrinter(PrettyPrinter pp) {
        this._cfgPrettyPrinter = pp;
        return this;
    }

    public PrettyPrinter getPrettyPrinter() {
        return this._cfgPrettyPrinter;
    }

    public JsonGenerator setHighestNonEscapedChar(int charCode) {
        return this;
    }

    public int getHighestEscapedChar() {
        return 0;
    }

    public CharacterEscapes getCharacterEscapes() {
        return null;
    }

    public JsonGenerator setCharacterEscapes(CharacterEscapes esc) {
        return this;
    }

    public JsonGenerator setRootValueSeparator(SerializableString sep) {
        throw new UnsupportedOperationException();
    }

    public Object getOutputTarget() {
        return null;
    }

    public int getOutputBuffered() {
        return -1;
    }

    public Object getCurrentValue() {
        JsonStreamContext ctxt = getOutputContext();
        if (ctxt == null) {
            return null;
        }
        return ctxt.getCurrentValue();
    }

    public void setCurrentValue(Object v) {
        JsonStreamContext ctxt = getOutputContext();
        if (ctxt != null) {
            ctxt.setCurrentValue(v);
        }
    }

    public boolean canUseSchema(FormatSchema schema) {
        return false;
    }

    public boolean canWriteObjectId() {
        return false;
    }

    public boolean canWriteTypeId() {
        return false;
    }

    public boolean canWriteBinaryNatively() {
        return false;
    }

    public boolean canOmitFields() {
        return true;
    }

    public boolean canWriteFormattedNumbers() {
        return false;
    }

    public void writeStartArray(int size) throws IOException {
        writeStartArray();
    }

    public void writeStartObject(Object forValue) throws IOException {
        writeStartObject();
        setCurrentValue(forValue);
    }

    public void writeFieldId(long id) throws IOException {
        writeFieldName(Long.toString(id));
    }

    public void writeArray(int[] array, int offset, int length) throws IOException {
        if (array != null) {
            _verifyOffsets(array.length, offset, length);
            writeStartArray();
            int end = offset + length;
            for (int i = offset; i < end; i++) {
                writeNumber(array[i]);
            }
            writeEndArray();
            return;
        }
        throw new IllegalArgumentException("null array");
    }

    public void writeArray(long[] array, int offset, int length) throws IOException {
        if (array != null) {
            _verifyOffsets(array.length, offset, length);
            writeStartArray();
            int end = offset + length;
            for (int i = offset; i < end; i++) {
                writeNumber(array[i]);
            }
            writeEndArray();
            return;
        }
        throw new IllegalArgumentException("null array");
    }

    public void writeArray(double[] array, int offset, int length) throws IOException {
        if (array != null) {
            _verifyOffsets(array.length, offset, length);
            writeStartArray();
            int end = offset + length;
            for (int i = offset; i < end; i++) {
                writeNumber(array[i]);
            }
            writeEndArray();
            return;
        }
        throw new IllegalArgumentException("null array");
    }

    public void writeString(Reader reader, int len) throws IOException {
        _reportUnsupportedOperation();
    }

    public void writeRaw(SerializableString raw) throws IOException {
        writeRaw(raw.getValue());
    }

    public void writeRawValue(SerializableString raw) throws IOException {
        writeRawValue(raw.getValue());
    }

    public void writeBinary(byte[] data, int offset, int len) throws IOException {
        writeBinary(Base64Variants.getDefaultVariant(), data, offset, len);
    }

    public void writeBinary(byte[] data) throws IOException {
        writeBinary(Base64Variants.getDefaultVariant(), data, 0, data.length);
    }

    public int writeBinary(InputStream data, int dataLength) throws IOException {
        return writeBinary(Base64Variants.getDefaultVariant(), data, dataLength);
    }

    public void writeNumber(short v) throws IOException {
        writeNumber((int) v);
    }

    public void writeEmbeddedObject(Object object) throws IOException {
        if (object == null) {
            writeNull();
        } else if (object instanceof byte[]) {
            writeBinary((byte[]) object);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("No native support for writing embedded objects of type ");
            sb.append(object.getClass().getName());
            throw new JsonGenerationException(sb.toString(), this);
        }
    }

    public void writeObjectId(Object id) throws IOException {
        throw new JsonGenerationException("No native support for writing Object Ids", this);
    }

    public void writeObjectRef(Object id) throws IOException {
        throw new JsonGenerationException("No native support for writing Object Ids", this);
    }

    public void writeTypeId(Object id) throws IOException {
        throw new JsonGenerationException("No native support for writing Type Ids", this);
    }

    public WritableTypeId writeTypePrefix(WritableTypeId typeIdDef) throws IOException {
        Object id = typeIdDef.id;
        JsonToken valueShape = typeIdDef.valueShape;
        if (!canWriteTypeId()) {
            String idStr = id instanceof String ? (String) id : String.valueOf(id);
            typeIdDef.wrapperWritten = true;
            Inclusion incl = typeIdDef.include;
            if (valueShape != JsonToken.START_OBJECT && incl.requiresObjectContext()) {
                Inclusion inclusion = Inclusion.WRAPPER_ARRAY;
                incl = inclusion;
                typeIdDef.include = inclusion;
            }
            switch (incl) {
                case PARENT_PROPERTY:
                case PAYLOAD_PROPERTY:
                    break;
                case METADATA_PROPERTY:
                    writeStartObject(typeIdDef.forValue);
                    writeStringField(typeIdDef.asProperty, idStr);
                    return typeIdDef;
                case WRAPPER_OBJECT:
                    writeStartObject();
                    writeFieldName(idStr);
                    break;
                default:
                    writeStartArray();
                    writeString(idStr);
                    break;
            }
        } else {
            typeIdDef.wrapperWritten = false;
            writeTypeId(id);
        }
        if (valueShape == JsonToken.START_OBJECT) {
            writeStartObject(typeIdDef.forValue);
        } else if (valueShape == JsonToken.START_ARRAY) {
            writeStartArray();
        }
        return typeIdDef;
    }

    public WritableTypeId writeTypeSuffix(WritableTypeId typeIdDef) throws IOException {
        JsonToken valueShape = typeIdDef.valueShape;
        if (valueShape == JsonToken.START_OBJECT) {
            writeEndObject();
        } else if (valueShape == JsonToken.START_ARRAY) {
            writeEndArray();
        }
        if (typeIdDef.wrapperWritten) {
            int i = AnonymousClass1.$SwitchMap$com$fasterxml$jackson$core$type$WritableTypeId$Inclusion[typeIdDef.include.ordinal()];
            if (i != 5) {
                switch (i) {
                    case 1:
                        Object id = typeIdDef.id;
                        writeStringField(typeIdDef.asProperty, id instanceof String ? (String) id : String.valueOf(id));
                        break;
                    case 2:
                    case 3:
                        break;
                    default:
                        writeEndObject();
                        break;
                }
            } else {
                writeEndArray();
            }
        }
        return typeIdDef;
    }

    public void writeStringField(String fieldName, String value) throws IOException {
        writeFieldName(fieldName);
        writeString(value);
    }

    public final void writeBooleanField(String fieldName, boolean value) throws IOException {
        writeFieldName(fieldName);
        writeBoolean(value);
    }

    public final void writeNullField(String fieldName) throws IOException {
        writeFieldName(fieldName);
        writeNull();
    }

    public final void writeNumberField(String fieldName, int value) throws IOException {
        writeFieldName(fieldName);
        writeNumber(value);
    }

    public final void writeNumberField(String fieldName, long value) throws IOException {
        writeFieldName(fieldName);
        writeNumber(value);
    }

    public final void writeNumberField(String fieldName, double value) throws IOException {
        writeFieldName(fieldName);
        writeNumber(value);
    }

    public final void writeNumberField(String fieldName, float value) throws IOException {
        writeFieldName(fieldName);
        writeNumber(value);
    }

    public final void writeNumberField(String fieldName, BigDecimal value) throws IOException {
        writeFieldName(fieldName);
        writeNumber(value);
    }

    public final void writeBinaryField(String fieldName, byte[] data) throws IOException {
        writeFieldName(fieldName);
        writeBinary(data);
    }

    public final void writeArrayFieldStart(String fieldName) throws IOException {
        writeFieldName(fieldName);
        writeStartArray();
    }

    public final void writeObjectFieldStart(String fieldName) throws IOException {
        writeFieldName(fieldName);
        writeStartObject();
    }

    public final void writeObjectField(String fieldName, Object pojo) throws IOException {
        writeFieldName(fieldName);
        writeObject(pojo);
    }

    public void writeOmittedField(String fieldName) throws IOException {
    }

    public void copyCurrentEvent(JsonParser p) throws IOException {
        JsonToken t = p.currentToken();
        if (t == null) {
            _reportError("No current event to copy");
        }
        int id = t.id();
        if (id != -1) {
            switch (id) {
                case 1:
                    writeStartObject();
                    return;
                case 2:
                    writeEndObject();
                    return;
                case 3:
                    writeStartArray();
                    return;
                case 4:
                    writeEndArray();
                    return;
                case 5:
                    writeFieldName(p.getCurrentName());
                    return;
                case 6:
                    if (p.hasTextCharacters()) {
                        writeString(p.getTextCharacters(), p.getTextOffset(), p.getTextLength());
                        return;
                    } else {
                        writeString(p.getText());
                        return;
                    }
                case 7:
                    NumberType n = p.getNumberType();
                    if (n == NumberType.INT) {
                        writeNumber(p.getIntValue());
                        return;
                    } else if (n == NumberType.BIG_INTEGER) {
                        writeNumber(p.getBigIntegerValue());
                        return;
                    } else {
                        writeNumber(p.getLongValue());
                        return;
                    }
                case 8:
                    NumberType n2 = p.getNumberType();
                    if (n2 == NumberType.BIG_DECIMAL) {
                        writeNumber(p.getDecimalValue());
                        return;
                    } else if (n2 == NumberType.FLOAT) {
                        writeNumber(p.getFloatValue());
                        return;
                    } else {
                        writeNumber(p.getDoubleValue());
                        return;
                    }
                case 9:
                    writeBoolean(true);
                    return;
                case 10:
                    writeBoolean(false);
                    return;
                case 11:
                    writeNull();
                    return;
                case 12:
                    writeObject(p.getEmbeddedObject());
                    return;
                default:
                    _throwInternal();
                    return;
            }
        } else {
            _reportError("No current event to copy");
        }
    }

    public void copyCurrentStructure(JsonParser p) throws IOException {
        JsonToken t = p.currentToken();
        if (t == null) {
            _reportError("No current event to copy");
        }
        int id = t.id();
        if (id == 5) {
            writeFieldName(p.getCurrentName());
            id = p.nextToken().id();
        }
        if (id == 1) {
            writeStartObject();
            while (p.nextToken() != JsonToken.END_OBJECT) {
                copyCurrentStructure(p);
            }
            writeEndObject();
        } else if (id != 3) {
            copyCurrentEvent(p);
        } else {
            writeStartArray();
            while (p.nextToken() != JsonToken.END_ARRAY) {
                copyCurrentStructure(p);
            }
            writeEndArray();
        }
    }

    /* access modifiers changed from: protected */
    public void _reportError(String msg) throws JsonGenerationException {
        throw new JsonGenerationException(msg, this);
    }

    /* access modifiers changed from: protected */
    public final void _throwInternal() {
        VersionUtil.throwInternal();
    }

    /* access modifiers changed from: protected */
    public void _reportUnsupportedOperation() {
        StringBuilder sb = new StringBuilder();
        sb.append("Operation not supported by generator of type ");
        sb.append(getClass().getName());
        throw new UnsupportedOperationException(sb.toString());
    }

    /* access modifiers changed from: protected */
    public final void _verifyOffsets(int arrayLength, int offset, int length) {
        if (offset < 0 || offset + length > arrayLength) {
            throw new IllegalArgumentException(String.format("invalid argument(s) (offset=%d, length=%d) for input array of %d element", new Object[]{Integer.valueOf(offset), Integer.valueOf(length), Integer.valueOf(arrayLength)}));
        }
    }

    /* access modifiers changed from: protected */
    public void _writeSimpleObject(Object value) throws IOException {
        if (value == null) {
            writeNull();
        } else if (value instanceof String) {
            writeString((String) value);
        } else {
            if (value instanceof Number) {
                Number n = (Number) value;
                if (n instanceof Integer) {
                    writeNumber(n.intValue());
                    return;
                } else if (n instanceof Long) {
                    writeNumber(n.longValue());
                    return;
                } else if (n instanceof Double) {
                    writeNumber(n.doubleValue());
                    return;
                } else if (n instanceof Float) {
                    writeNumber(n.floatValue());
                    return;
                } else if (n instanceof Short) {
                    writeNumber(n.shortValue());
                    return;
                } else if (n instanceof Byte) {
                    writeNumber((short) n.byteValue());
                    return;
                } else if (n instanceof BigInteger) {
                    writeNumber((BigInteger) n);
                    return;
                } else if (n instanceof BigDecimal) {
                    writeNumber((BigDecimal) n);
                    return;
                } else if (n instanceof AtomicInteger) {
                    writeNumber(((AtomicInteger) n).get());
                    return;
                } else if (n instanceof AtomicLong) {
                    writeNumber(((AtomicLong) n).get());
                    return;
                }
            } else if (value instanceof byte[]) {
                writeBinary((byte[]) value);
                return;
            } else if (value instanceof Boolean) {
                writeBoolean(((Boolean) value).booleanValue());
                return;
            } else if (value instanceof AtomicBoolean) {
                writeBoolean(((AtomicBoolean) value).get());
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("No ObjectCodec defined for the generator, can only serialize simple wrapper types (type passed ");
            sb.append(value.getClass().getName());
            sb.append(")");
            throw new IllegalStateException(sb.toString());
        }
    }
}
