package com.fasterxml.jackson.core.filter;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.JsonParserDelegate;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

public class FilteringParserDelegate extends JsonParserDelegate {
    protected boolean _allowMultipleMatches;
    protected JsonToken _currToken;
    protected TokenFilterContext _exposedContext;
    protected TokenFilterContext _headContext;
    @Deprecated
    protected boolean _includeImmediateParent;
    protected boolean _includePath;
    protected TokenFilter _itemFilter;
    protected JsonToken _lastClearedToken;
    protected int _matchCount;
    protected TokenFilter rootFilter;

    public FilteringParserDelegate(JsonParser p, TokenFilter f, boolean includePath, boolean allowMultipleMatches) {
        super(p);
        this.rootFilter = f;
        this._itemFilter = f;
        this._headContext = TokenFilterContext.createRootContext(f);
        this._includePath = includePath;
        this._allowMultipleMatches = allowMultipleMatches;
    }

    public TokenFilter getFilter() {
        return this.rootFilter;
    }

    public int getMatchCount() {
        return this._matchCount;
    }

    public JsonToken getCurrentToken() {
        return this._currToken;
    }

    public JsonToken currentToken() {
        return this._currToken;
    }

    public final int getCurrentTokenId() {
        JsonToken t = this._currToken;
        if (t == null) {
            return 0;
        }
        return t.id();
    }

    public final int currentTokenId() {
        JsonToken t = this._currToken;
        if (t == null) {
            return 0;
        }
        return t.id();
    }

    public boolean hasCurrentToken() {
        return this._currToken != null;
    }

    public boolean hasTokenId(int id) {
        JsonToken t = this._currToken;
        boolean z = true;
        if (t == null) {
            if (id != 0) {
                z = false;
            }
            return z;
        }
        if (t.id() != id) {
            z = false;
        }
        return z;
    }

    public final boolean hasToken(JsonToken t) {
        return this._currToken == t;
    }

    public boolean isExpectedStartArrayToken() {
        return this._currToken == JsonToken.START_ARRAY;
    }

    public boolean isExpectedStartObjectToken() {
        return this._currToken == JsonToken.START_OBJECT;
    }

    public JsonLocation getCurrentLocation() {
        return this.delegate.getCurrentLocation();
    }

    public JsonStreamContext getParsingContext() {
        return _filterContext();
    }

    public String getCurrentName() throws IOException {
        JsonStreamContext ctxt = _filterContext();
        if (this._currToken != JsonToken.START_OBJECT && this._currToken != JsonToken.START_ARRAY) {
            return ctxt.getCurrentName();
        }
        JsonStreamContext parent = ctxt.getParent();
        return parent == null ? null : parent.getCurrentName();
    }

    public void clearCurrentToken() {
        JsonToken jsonToken = this._currToken;
        if (jsonToken != null) {
            this._lastClearedToken = jsonToken;
            this._currToken = null;
        }
    }

    public JsonToken getLastClearedToken() {
        return this._lastClearedToken;
    }

    public void overrideCurrentName(String name) {
        throw new UnsupportedOperationException("Can not currently override name during filtering read");
    }

    public JsonToken nextToken() throws IOException {
        TokenFilter f;
        if (!this._allowMultipleMatches) {
            JsonToken jsonToken = this._currToken;
            if (jsonToken != null && this._exposedContext == null && jsonToken.isScalarValue() && !this._headContext.isStartHandled() && !this._includePath && this._itemFilter == TokenFilter.INCLUDE_ALL) {
                this._currToken = null;
                return null;
            }
        }
        TokenFilterContext ctxt = this._exposedContext;
        if (ctxt != null) {
            do {
                JsonToken t = ctxt.nextTokenToRead();
                if (t != null) {
                    this._currToken = t;
                    return t;
                }
                TokenFilterContext tokenFilterContext = this._headContext;
                if (ctxt == tokenFilterContext) {
                    this._exposedContext = null;
                    if (ctxt.inArray()) {
                        JsonToken t2 = this.delegate.getCurrentToken();
                        this._currToken = t2;
                        return t2;
                    }
                } else {
                    ctxt = tokenFilterContext.findChildOf(ctxt);
                    this._exposedContext = ctxt;
                }
            } while (ctxt != null);
            throw _constructError("Unexpected problem: chain of filtered context broken");
        }
        JsonToken t3 = this.delegate.nextToken();
        if (t3 == null) {
            this._currToken = t3;
            return t3;
        }
        switch (t3.id()) {
            case 1:
                TokenFilter f2 = this._itemFilter;
                if (f2 == TokenFilter.INCLUDE_ALL) {
                    this._headContext = this._headContext.createChildObjectContext(f2, true);
                    this._currToken = t3;
                    return t3;
                } else if (f2 == null) {
                    this.delegate.skipChildren();
                    break;
                } else {
                    TokenFilter f3 = this._headContext.checkValue(f2);
                    if (f3 == null) {
                        this.delegate.skipChildren();
                        break;
                    } else {
                        if (f3 != TokenFilter.INCLUDE_ALL) {
                            f3 = f3.filterStartObject();
                        }
                        this._itemFilter = f3;
                        if (f3 == TokenFilter.INCLUDE_ALL) {
                            this._headContext = this._headContext.createChildObjectContext(f3, true);
                            this._currToken = t3;
                            return t3;
                        }
                        this._headContext = this._headContext.createChildObjectContext(f3, false);
                        if (this._includePath) {
                            JsonToken t4 = _nextTokenWithBuffering(this._headContext);
                            if (t4 != null) {
                                this._currToken = t4;
                                return t4;
                            }
                        }
                    }
                }
                break;
            case 2:
            case 4:
                boolean returnEnd = this._headContext.isStartHandled();
                TokenFilter f4 = this._headContext.getFilter();
                if (!(f4 == null || f4 == TokenFilter.INCLUDE_ALL)) {
                    f4.filterFinishArray();
                }
                this._headContext = this._headContext.getParent();
                this._itemFilter = this._headContext.getFilter();
                if (!returnEnd) {
                    TokenFilter tokenFilter = f4;
                    break;
                } else {
                    this._currToken = t3;
                    return t3;
                }
            case 3:
                TokenFilter f5 = this._itemFilter;
                if (f5 == TokenFilter.INCLUDE_ALL) {
                    this._headContext = this._headContext.createChildArrayContext(f5, true);
                    this._currToken = t3;
                    return t3;
                } else if (f5 == null) {
                    this.delegate.skipChildren();
                    break;
                } else {
                    TokenFilter f6 = this._headContext.checkValue(f5);
                    if (f6 == null) {
                        this.delegate.skipChildren();
                        break;
                    } else {
                        if (f6 != TokenFilter.INCLUDE_ALL) {
                            f6 = f6.filterStartArray();
                        }
                        this._itemFilter = f6;
                        if (f6 == TokenFilter.INCLUDE_ALL) {
                            this._headContext = this._headContext.createChildArrayContext(f6, true);
                            this._currToken = t3;
                            return t3;
                        }
                        this._headContext = this._headContext.createChildArrayContext(f6, false);
                        if (this._includePath) {
                            JsonToken t5 = _nextTokenWithBuffering(this._headContext);
                            if (t5 != null) {
                                this._currToken = t5;
                                return t5;
                            }
                        }
                    }
                }
                break;
            case 5:
                String name = this.delegate.getCurrentName();
                TokenFilter f7 = this._headContext.setFieldName(name);
                if (f7 != TokenFilter.INCLUDE_ALL) {
                    if (f7 != null) {
                        f = f7.includeProperty(name);
                        if (f != null) {
                            this._itemFilter = f;
                            if (f == TokenFilter.INCLUDE_ALL) {
                                if (!_verifyAllowedMatches()) {
                                    this.delegate.nextToken();
                                    this.delegate.skipChildren();
                                } else if (this._includePath) {
                                    this._currToken = t3;
                                    return t3;
                                }
                            }
                            if (this._includePath) {
                                JsonToken t6 = _nextTokenWithBuffering(this._headContext);
                                if (t6 == null) {
                                    TokenFilter tokenFilter2 = f;
                                    break;
                                } else {
                                    this._currToken = t6;
                                    return t6;
                                }
                            }
                        } else {
                            this.delegate.nextToken();
                            this.delegate.skipChildren();
                        }
                    } else {
                        this.delegate.nextToken();
                        this.delegate.skipChildren();
                        TokenFilter tokenFilter3 = f7;
                        break;
                    }
                } else {
                    this._itemFilter = f7;
                    if (!this._includePath && this._includeImmediateParent && !this._headContext.isStartHandled()) {
                        t3 = this._headContext.nextTokenToRead();
                        this._exposedContext = this._headContext;
                    }
                    this._currToken = t3;
                    return t3;
                }
            default:
                TokenFilter f8 = this._itemFilter;
                if (f8 != TokenFilter.INCLUDE_ALL) {
                    if (f8 == null) {
                        TokenFilter tokenFilter4 = f8;
                        break;
                    } else {
                        f = this._headContext.checkValue(f8);
                        if ((f == TokenFilter.INCLUDE_ALL || (f != null && f.includeValue(this.delegate))) && _verifyAllowedMatches()) {
                            this._currToken = t3;
                            return t3;
                        }
                        TokenFilter tokenFilter5 = f;
                        break;
                    }
                } else {
                    this._currToken = t3;
                    return t3;
                }
        }
        return _nextToken2();
    }

    /* access modifiers changed from: protected */
    public final JsonToken _nextToken2() throws IOException {
        while (true) {
            JsonToken t = this.delegate.nextToken();
            if (t == null) {
                this._currToken = t;
                return t;
            }
            switch (t.id()) {
                case 1:
                    TokenFilter f = this._itemFilter;
                    if (f == TokenFilter.INCLUDE_ALL) {
                        this._headContext = this._headContext.createChildObjectContext(f, true);
                        this._currToken = t;
                        return t;
                    } else if (f == null) {
                        this.delegate.skipChildren();
                        break;
                    } else {
                        TokenFilter f2 = this._headContext.checkValue(f);
                        if (f2 == null) {
                            this.delegate.skipChildren();
                            break;
                        } else {
                            if (f2 != TokenFilter.INCLUDE_ALL) {
                                f2 = f2.filterStartObject();
                            }
                            this._itemFilter = f2;
                            if (f2 == TokenFilter.INCLUDE_ALL) {
                                this._headContext = this._headContext.createChildObjectContext(f2, true);
                                this._currToken = t;
                                return t;
                            }
                            this._headContext = this._headContext.createChildObjectContext(f2, false);
                            if (this._includePath) {
                                JsonToken t2 = _nextTokenWithBuffering(this._headContext);
                                if (t2 == null) {
                                    break;
                                } else {
                                    this._currToken = t2;
                                    return t2;
                                }
                            } else {
                                continue;
                            }
                        }
                    }
                case 2:
                case 4:
                    boolean returnEnd = this._headContext.isStartHandled();
                    TokenFilter f3 = this._headContext.getFilter();
                    if (!(f3 == null || f3 == TokenFilter.INCLUDE_ALL)) {
                        f3.filterFinishArray();
                    }
                    this._headContext = this._headContext.getParent();
                    this._itemFilter = this._headContext.getFilter();
                    if (!returnEnd) {
                        break;
                    } else {
                        this._currToken = t;
                        return t;
                    }
                case 3:
                    TokenFilter f4 = this._itemFilter;
                    if (f4 == TokenFilter.INCLUDE_ALL) {
                        this._headContext = this._headContext.createChildArrayContext(f4, true);
                        this._currToken = t;
                        return t;
                    } else if (f4 == null) {
                        this.delegate.skipChildren();
                        break;
                    } else {
                        TokenFilter f5 = this._headContext.checkValue(f4);
                        if (f5 == null) {
                            this.delegate.skipChildren();
                            break;
                        } else {
                            if (f5 != TokenFilter.INCLUDE_ALL) {
                                f5 = f5.filterStartArray();
                            }
                            this._itemFilter = f5;
                            if (f5 == TokenFilter.INCLUDE_ALL) {
                                this._headContext = this._headContext.createChildArrayContext(f5, true);
                                this._currToken = t;
                                return t;
                            }
                            this._headContext = this._headContext.createChildArrayContext(f5, false);
                            if (this._includePath) {
                                JsonToken t3 = _nextTokenWithBuffering(this._headContext);
                                if (t3 == null) {
                                    break;
                                } else {
                                    this._currToken = t3;
                                    return t3;
                                }
                            } else {
                                continue;
                            }
                        }
                    }
                case 5:
                    String name = this.delegate.getCurrentName();
                    TokenFilter f6 = this._headContext.setFieldName(name);
                    if (f6 == TokenFilter.INCLUDE_ALL) {
                        this._itemFilter = f6;
                        this._currToken = t;
                        return t;
                    } else if (f6 == null) {
                        this.delegate.nextToken();
                        this.delegate.skipChildren();
                        break;
                    } else {
                        TokenFilter f7 = f6.includeProperty(name);
                        if (f7 == null) {
                            this.delegate.nextToken();
                            this.delegate.skipChildren();
                            break;
                        } else {
                            this._itemFilter = f7;
                            if (f7 == TokenFilter.INCLUDE_ALL) {
                                if (_verifyAllowedMatches() && this._includePath) {
                                    this._currToken = t;
                                    return t;
                                }
                            } else if (this._includePath) {
                                JsonToken t4 = _nextTokenWithBuffering(this._headContext);
                                if (t4 == null) {
                                    break;
                                } else {
                                    this._currToken = t4;
                                    return t4;
                                }
                            } else {
                                continue;
                            }
                        }
                    }
                default:
                    TokenFilter f8 = this._itemFilter;
                    if (f8 == TokenFilter.INCLUDE_ALL) {
                        this._currToken = t;
                        return t;
                    } else if (f8 != null) {
                        TokenFilter f9 = this._headContext.checkValue(f8);
                        if ((f9 == TokenFilter.INCLUDE_ALL || (f9 != null && f9.includeValue(this.delegate))) && _verifyAllowedMatches()) {
                            this._currToken = t;
                            return t;
                        }
                    } else {
                        continue;
                    }
            }
        }
    }

    /* access modifiers changed from: protected */
    public final JsonToken _nextTokenWithBuffering(TokenFilterContext buffRoot) throws IOException {
        while (true) {
            JsonToken t = this.delegate.nextToken();
            if (t == null) {
                return t;
            }
            boolean returnEnd = false;
            switch (t.id()) {
                case 1:
                    TokenFilter f = this._itemFilter;
                    if (f != TokenFilter.INCLUDE_ALL) {
                        if (f != null) {
                            TokenFilter f2 = this._headContext.checkValue(f);
                            if (f2 != null) {
                                if (f2 != TokenFilter.INCLUDE_ALL) {
                                    f2 = f2.filterStartObject();
                                }
                                this._itemFilter = f2;
                                if (f2 != TokenFilter.INCLUDE_ALL) {
                                    this._headContext = this._headContext.createChildObjectContext(f2, false);
                                    break;
                                } else {
                                    this._headContext = this._headContext.createChildObjectContext(f2, true);
                                    return _nextBuffered(buffRoot);
                                }
                            } else {
                                this.delegate.skipChildren();
                                break;
                            }
                        } else {
                            this.delegate.skipChildren();
                            break;
                        }
                    } else {
                        this._headContext = this._headContext.createChildObjectContext(f, true);
                        return t;
                    }
                case 2:
                case 4:
                    TokenFilter f3 = this._headContext.getFilter();
                    if (!(f3 == null || f3 == TokenFilter.INCLUDE_ALL)) {
                        f3.filterFinishArray();
                    }
                    if ((this._headContext == buffRoot) && this._headContext.isStartHandled()) {
                        returnEnd = true;
                    }
                    this._headContext = this._headContext.getParent();
                    this._itemFilter = this._headContext.getFilter();
                    if (!returnEnd) {
                        break;
                    } else {
                        return t;
                    }
                case 3:
                    TokenFilter f4 = this._headContext.checkValue(this._itemFilter);
                    if (f4 != null) {
                        if (f4 != TokenFilter.INCLUDE_ALL) {
                            f4 = f4.filterStartArray();
                        }
                        this._itemFilter = f4;
                        if (f4 != TokenFilter.INCLUDE_ALL) {
                            this._headContext = this._headContext.createChildArrayContext(f4, false);
                            break;
                        } else {
                            this._headContext = this._headContext.createChildArrayContext(f4, true);
                            return _nextBuffered(buffRoot);
                        }
                    } else {
                        this.delegate.skipChildren();
                        break;
                    }
                case 5:
                    String name = this.delegate.getCurrentName();
                    TokenFilter f5 = this._headContext.setFieldName(name);
                    if (f5 != TokenFilter.INCLUDE_ALL) {
                        if (f5 != null) {
                            TokenFilter f6 = f5.includeProperty(name);
                            if (f6 != null) {
                                this._itemFilter = f6;
                                if (f6 == TokenFilter.INCLUDE_ALL) {
                                    if (!_verifyAllowedMatches()) {
                                        this._itemFilter = this._headContext.setFieldName(name);
                                        break;
                                    } else {
                                        return _nextBuffered(buffRoot);
                                    }
                                } else {
                                    continue;
                                }
                            } else {
                                this.delegate.nextToken();
                                this.delegate.skipChildren();
                                break;
                            }
                        } else {
                            this.delegate.nextToken();
                            this.delegate.skipChildren();
                            break;
                        }
                    } else {
                        this._itemFilter = f5;
                        return _nextBuffered(buffRoot);
                    }
                default:
                    TokenFilter f7 = this._itemFilter;
                    if (f7 == TokenFilter.INCLUDE_ALL) {
                        return _nextBuffered(buffRoot);
                    }
                    if (f7 != null) {
                        TokenFilter f8 = this._headContext.checkValue(f7);
                        if ((f8 == TokenFilter.INCLUDE_ALL || (f8 != null && f8.includeValue(this.delegate))) && _verifyAllowedMatches()) {
                            return _nextBuffered(buffRoot);
                        }
                    } else {
                        continue;
                    }
            }
        }
    }

    private JsonToken _nextBuffered(TokenFilterContext buffRoot) throws IOException {
        this._exposedContext = buffRoot;
        TokenFilterContext ctxt = buffRoot;
        JsonToken t = ctxt.nextTokenToRead();
        if (t != null) {
            return t;
        }
        while (ctxt != this._headContext) {
            ctxt = this._exposedContext.findChildOf(ctxt);
            this._exposedContext = ctxt;
            if (ctxt != null) {
                JsonToken t2 = this._exposedContext.nextTokenToRead();
                if (t2 != null) {
                    return t2;
                }
            } else {
                throw _constructError("Unexpected problem: chain of filtered context broken");
            }
        }
        throw _constructError("Internal error: failed to locate expected buffered tokens");
    }

    private final boolean _verifyAllowedMatches() throws IOException {
        if (this._matchCount != 0 && !this._allowMultipleMatches) {
            return false;
        }
        this._matchCount++;
        return true;
    }

    public JsonToken nextValue() throws IOException {
        JsonToken t = nextToken();
        if (t == JsonToken.FIELD_NAME) {
            return nextToken();
        }
        return t;
    }

    public JsonParser skipChildren() throws IOException {
        if (this._currToken != JsonToken.START_OBJECT && this._currToken != JsonToken.START_ARRAY) {
            return this;
        }
        int open = 1;
        while (true) {
            JsonToken t = nextToken();
            if (t == null) {
                return this;
            }
            if (t.isStructStart()) {
                open++;
            } else if (t.isStructEnd()) {
                open--;
                if (open == 0) {
                    return this;
                }
            } else {
                continue;
            }
        }
    }

    public String getText() throws IOException {
        return this.delegate.getText();
    }

    public boolean hasTextCharacters() {
        return this.delegate.hasTextCharacters();
    }

    public char[] getTextCharacters() throws IOException {
        return this.delegate.getTextCharacters();
    }

    public int getTextLength() throws IOException {
        return this.delegate.getTextLength();
    }

    public int getTextOffset() throws IOException {
        return this.delegate.getTextOffset();
    }

    public BigInteger getBigIntegerValue() throws IOException {
        return this.delegate.getBigIntegerValue();
    }

    public boolean getBooleanValue() throws IOException {
        return this.delegate.getBooleanValue();
    }

    public byte getByteValue() throws IOException {
        return this.delegate.getByteValue();
    }

    public short getShortValue() throws IOException {
        return this.delegate.getShortValue();
    }

    public BigDecimal getDecimalValue() throws IOException {
        return this.delegate.getDecimalValue();
    }

    public double getDoubleValue() throws IOException {
        return this.delegate.getDoubleValue();
    }

    public float getFloatValue() throws IOException {
        return this.delegate.getFloatValue();
    }

    public int getIntValue() throws IOException {
        return this.delegate.getIntValue();
    }

    public long getLongValue() throws IOException {
        return this.delegate.getLongValue();
    }

    public NumberType getNumberType() throws IOException {
        return this.delegate.getNumberType();
    }

    public Number getNumberValue() throws IOException {
        return this.delegate.getNumberValue();
    }

    public int getValueAsInt() throws IOException {
        return this.delegate.getValueAsInt();
    }

    public int getValueAsInt(int defaultValue) throws IOException {
        return this.delegate.getValueAsInt(defaultValue);
    }

    public long getValueAsLong() throws IOException {
        return this.delegate.getValueAsLong();
    }

    public long getValueAsLong(long defaultValue) throws IOException {
        return this.delegate.getValueAsLong(defaultValue);
    }

    public double getValueAsDouble() throws IOException {
        return this.delegate.getValueAsDouble();
    }

    public double getValueAsDouble(double defaultValue) throws IOException {
        return this.delegate.getValueAsDouble(defaultValue);
    }

    public boolean getValueAsBoolean() throws IOException {
        return this.delegate.getValueAsBoolean();
    }

    public boolean getValueAsBoolean(boolean defaultValue) throws IOException {
        return this.delegate.getValueAsBoolean(defaultValue);
    }

    public String getValueAsString() throws IOException {
        return this.delegate.getValueAsString();
    }

    public String getValueAsString(String defaultValue) throws IOException {
        return this.delegate.getValueAsString(defaultValue);
    }

    public Object getEmbeddedObject() throws IOException {
        return this.delegate.getEmbeddedObject();
    }

    public byte[] getBinaryValue(Base64Variant b64variant) throws IOException {
        return this.delegate.getBinaryValue(b64variant);
    }

    public int readBinaryValue(Base64Variant b64variant, OutputStream out) throws IOException {
        return this.delegate.readBinaryValue(b64variant, out);
    }

    public JsonLocation getTokenLocation() {
        return this.delegate.getTokenLocation();
    }

    /* access modifiers changed from: protected */
    public JsonStreamContext _filterContext() {
        TokenFilterContext tokenFilterContext = this._exposedContext;
        if (tokenFilterContext != null) {
            return tokenFilterContext;
        }
        return this._headContext;
    }
}
