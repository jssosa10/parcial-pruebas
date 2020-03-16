package com.fasterxml.jackson.core.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.SerializedString;
import java.io.IOException;
import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;

public class DefaultPrettyPrinter implements PrettyPrinter, Instantiatable<DefaultPrettyPrinter>, Serializable {
    public static final SerializedString DEFAULT_ROOT_VALUE_SEPARATOR = new SerializedString(StringUtils.SPACE);
    private static final long serialVersionUID = 1;
    protected Indenter _arrayIndenter;
    protected transient int _nesting;
    protected String _objectFieldValueSeparatorWithSpaces;
    protected Indenter _objectIndenter;
    protected final SerializableString _rootSeparator;
    protected Separators _separators;
    protected boolean _spacesInObjectEntries;

    public static class FixedSpaceIndenter extends NopIndenter {
        public static final FixedSpaceIndenter instance = new FixedSpaceIndenter();

        public void writeIndentation(JsonGenerator g, int level) throws IOException {
            g.writeRaw(' ');
        }

        public boolean isInline() {
            return true;
        }
    }

    public interface Indenter {
        boolean isInline();

        void writeIndentation(JsonGenerator jsonGenerator, int i) throws IOException;
    }

    public static class NopIndenter implements Indenter, Serializable {
        public static final NopIndenter instance = new NopIndenter();

        public void writeIndentation(JsonGenerator g, int level) throws IOException {
        }

        public boolean isInline() {
            return true;
        }
    }

    public DefaultPrettyPrinter() {
        this((SerializableString) DEFAULT_ROOT_VALUE_SEPARATOR);
    }

    public DefaultPrettyPrinter(String rootSeparator) {
        this((SerializableString) rootSeparator == null ? null : new SerializedString(rootSeparator));
    }

    public DefaultPrettyPrinter(SerializableString rootSeparator) {
        this._arrayIndenter = FixedSpaceIndenter.instance;
        this._objectIndenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE;
        this._spacesInObjectEntries = true;
        this._rootSeparator = rootSeparator;
        withSeparators(DEFAULT_SEPARATORS);
    }

    public DefaultPrettyPrinter(DefaultPrettyPrinter base) {
        this(base, base._rootSeparator);
    }

    public DefaultPrettyPrinter(DefaultPrettyPrinter base, SerializableString rootSeparator) {
        this._arrayIndenter = FixedSpaceIndenter.instance;
        this._objectIndenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE;
        this._spacesInObjectEntries = true;
        this._arrayIndenter = base._arrayIndenter;
        this._objectIndenter = base._objectIndenter;
        this._spacesInObjectEntries = base._spacesInObjectEntries;
        this._nesting = base._nesting;
        this._separators = base._separators;
        this._objectFieldValueSeparatorWithSpaces = base._objectFieldValueSeparatorWithSpaces;
        this._rootSeparator = rootSeparator;
    }

    public DefaultPrettyPrinter withRootSeparator(SerializableString rootSeparator) {
        SerializableString serializableString = this._rootSeparator;
        if (serializableString == rootSeparator || (rootSeparator != null && rootSeparator.equals(serializableString))) {
            return this;
        }
        return new DefaultPrettyPrinter(this, rootSeparator);
    }

    public DefaultPrettyPrinter withRootSeparator(String rootSeparator) {
        return withRootSeparator((SerializableString) rootSeparator == null ? null : new SerializedString(rootSeparator));
    }

    public void indentArraysWith(Indenter i) {
        this._arrayIndenter = i == null ? NopIndenter.instance : i;
    }

    public void indentObjectsWith(Indenter i) {
        this._objectIndenter = i == null ? NopIndenter.instance : i;
    }

    public DefaultPrettyPrinter withArrayIndenter(Indenter i) {
        if (i == null) {
            i = NopIndenter.instance;
        }
        if (this._arrayIndenter == i) {
            return this;
        }
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter(this);
        pp._arrayIndenter = i;
        return pp;
    }

    public DefaultPrettyPrinter withObjectIndenter(Indenter i) {
        if (i == null) {
            i = NopIndenter.instance;
        }
        if (this._objectIndenter == i) {
            return this;
        }
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter(this);
        pp._objectIndenter = i;
        return pp;
    }

    public DefaultPrettyPrinter withSpacesInObjectEntries() {
        return _withSpaces(true);
    }

    public DefaultPrettyPrinter withoutSpacesInObjectEntries() {
        return _withSpaces(false);
    }

    /* access modifiers changed from: protected */
    public DefaultPrettyPrinter _withSpaces(boolean state) {
        if (this._spacesInObjectEntries == state) {
            return this;
        }
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter(this);
        pp._spacesInObjectEntries = state;
        return pp;
    }

    public DefaultPrettyPrinter withSeparators(Separators separators) {
        this._separators = separators;
        StringBuilder sb = new StringBuilder();
        sb.append(StringUtils.SPACE);
        sb.append(separators.getObjectFieldValueSeparator());
        sb.append(StringUtils.SPACE);
        this._objectFieldValueSeparatorWithSpaces = sb.toString();
        return this;
    }

    public DefaultPrettyPrinter createInstance() {
        return new DefaultPrettyPrinter(this);
    }

    public void writeRootValueSeparator(JsonGenerator g) throws IOException {
        SerializableString serializableString = this._rootSeparator;
        if (serializableString != null) {
            g.writeRaw(serializableString);
        }
    }

    public void writeStartObject(JsonGenerator g) throws IOException {
        g.writeRaw('{');
        if (!this._objectIndenter.isInline()) {
            this._nesting++;
        }
    }

    public void beforeObjectEntries(JsonGenerator g) throws IOException {
        this._objectIndenter.writeIndentation(g, this._nesting);
    }

    public void writeObjectFieldValueSeparator(JsonGenerator g) throws IOException {
        if (this._spacesInObjectEntries) {
            g.writeRaw(this._objectFieldValueSeparatorWithSpaces);
        } else {
            g.writeRaw(this._separators.getObjectFieldValueSeparator());
        }
    }

    public void writeObjectEntrySeparator(JsonGenerator g) throws IOException {
        g.writeRaw(this._separators.getObjectEntrySeparator());
        this._objectIndenter.writeIndentation(g, this._nesting);
    }

    public void writeEndObject(JsonGenerator g, int nrOfEntries) throws IOException {
        if (!this._objectIndenter.isInline()) {
            this._nesting--;
        }
        if (nrOfEntries > 0) {
            this._objectIndenter.writeIndentation(g, this._nesting);
        } else {
            g.writeRaw(' ');
        }
        g.writeRaw('}');
    }

    public void writeStartArray(JsonGenerator g) throws IOException {
        if (!this._arrayIndenter.isInline()) {
            this._nesting++;
        }
        g.writeRaw('[');
    }

    public void beforeArrayValues(JsonGenerator g) throws IOException {
        this._arrayIndenter.writeIndentation(g, this._nesting);
    }

    public void writeArrayValueSeparator(JsonGenerator g) throws IOException {
        g.writeRaw(this._separators.getArrayValueSeparator());
        this._arrayIndenter.writeIndentation(g, this._nesting);
    }

    public void writeEndArray(JsonGenerator g, int nrOfValues) throws IOException {
        if (!this._arrayIndenter.isInline()) {
            this._nesting--;
        }
        if (nrOfValues > 0) {
            this._arrayIndenter.writeIndentation(g, this._nesting);
        } else {
            g.writeRaw(' ');
        }
        g.writeRaw(']');
    }
}
