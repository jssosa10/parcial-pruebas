package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonStreamContext;

public class JsonWriteContext extends JsonStreamContext {
    public static final int STATUS_EXPECT_NAME = 5;
    public static final int STATUS_EXPECT_VALUE = 4;
    public static final int STATUS_OK_AFTER_COLON = 2;
    public static final int STATUS_OK_AFTER_COMMA = 1;
    public static final int STATUS_OK_AFTER_SPACE = 3;
    public static final int STATUS_OK_AS_IS = 0;
    protected JsonWriteContext _child;
    protected String _currentName;
    protected Object _currentValue;
    protected DupDetector _dups;
    protected boolean _gotName;
    protected final JsonWriteContext _parent;

    protected JsonWriteContext(int type, JsonWriteContext parent, DupDetector dups) {
        this._type = type;
        this._parent = parent;
        this._dups = dups;
        this._index = -1;
    }

    /* access modifiers changed from: protected */
    public JsonWriteContext reset(int type) {
        this._type = type;
        this._index = -1;
        this._currentName = null;
        this._gotName = false;
        this._currentValue = null;
        DupDetector dupDetector = this._dups;
        if (dupDetector != null) {
            dupDetector.reset();
        }
        return this;
    }

    public JsonWriteContext withDupDetector(DupDetector dups) {
        this._dups = dups;
        return this;
    }

    public Object getCurrentValue() {
        return this._currentValue;
    }

    public void setCurrentValue(Object v) {
        this._currentValue = v;
    }

    @Deprecated
    public static JsonWriteContext createRootContext() {
        return createRootContext(null);
    }

    public static JsonWriteContext createRootContext(DupDetector dd) {
        return new JsonWriteContext(0, null, dd);
    }

    public JsonWriteContext createChildArrayContext() {
        JsonWriteContext ctxt = this._child;
        if (ctxt != null) {
            return ctxt.reset(1);
        }
        DupDetector dupDetector = this._dups;
        JsonWriteContext jsonWriteContext = new JsonWriteContext(1, this, dupDetector == null ? null : dupDetector.child());
        JsonWriteContext ctxt2 = jsonWriteContext;
        this._child = jsonWriteContext;
        return ctxt2;
    }

    public JsonWriteContext createChildObjectContext() {
        JsonWriteContext ctxt = this._child;
        if (ctxt != null) {
            return ctxt.reset(2);
        }
        DupDetector dupDetector = this._dups;
        JsonWriteContext jsonWriteContext = new JsonWriteContext(2, this, dupDetector == null ? null : dupDetector.child());
        JsonWriteContext ctxt2 = jsonWriteContext;
        this._child = jsonWriteContext;
        return ctxt2;
    }

    public final JsonWriteContext getParent() {
        return this._parent;
    }

    public final String getCurrentName() {
        return this._currentName;
    }

    public boolean hasCurrentName() {
        return this._currentName != null;
    }

    public JsonWriteContext clearAndGetParent() {
        this._currentValue = null;
        return this._parent;
    }

    public DupDetector getDupDetector() {
        return this._dups;
    }

    public int writeFieldName(String name) throws JsonProcessingException {
        if (this._type != 2 || this._gotName) {
            return 4;
        }
        int i = 1;
        this._gotName = true;
        this._currentName = name;
        DupDetector dupDetector = this._dups;
        if (dupDetector != null) {
            _checkDup(dupDetector, name);
        }
        if (this._index < 0) {
            i = 0;
        }
        return i;
    }

    private final void _checkDup(DupDetector dd, String name) throws JsonProcessingException {
        if (dd.isDup(name)) {
            Object src = dd.getSource();
            StringBuilder sb = new StringBuilder();
            sb.append("Duplicate field '");
            sb.append(name);
            sb.append("'");
            throw new JsonGenerationException(sb.toString(), src instanceof JsonGenerator ? (JsonGenerator) src : null);
        }
    }

    public int writeValue() {
        int i = 0;
        if (this._type == 2) {
            if (!this._gotName) {
                return 5;
            }
            this._gotName = false;
            this._index++;
            return 2;
        } else if (this._type == 1) {
            int ix = this._index;
            this._index++;
            if (ix >= 0) {
                i = 1;
            }
            return i;
        } else {
            this._index++;
            if (this._index != 0) {
                i = 3;
            }
            return i;
        }
    }
}
