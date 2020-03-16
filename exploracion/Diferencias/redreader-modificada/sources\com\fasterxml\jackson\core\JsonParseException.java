package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.util.RequestPayload;

public class JsonParseException extends JsonProcessingException {
    private static final long serialVersionUID = 2;
    protected transient JsonParser _processor;
    protected RequestPayload _requestPayload;

    @Deprecated
    public JsonParseException(String msg, JsonLocation loc) {
        super(msg, loc);
    }

    @Deprecated
    public JsonParseException(String msg, JsonLocation loc, Throwable root) {
        super(msg, loc, root);
    }

    public JsonParseException(JsonParser p, String msg) {
        super(msg, p == null ? null : p.getCurrentLocation());
        this._processor = p;
    }

    public JsonParseException(JsonParser p, String msg, Throwable root) {
        super(msg, p == null ? null : p.getCurrentLocation(), root);
        this._processor = p;
    }

    public JsonParseException(JsonParser p, String msg, JsonLocation loc) {
        super(msg, loc);
        this._processor = p;
    }

    public JsonParseException(JsonParser p, String msg, JsonLocation loc, Throwable root) {
        super(msg, loc, root);
        this._processor = p;
    }

    public JsonParseException withParser(JsonParser p) {
        this._processor = p;
        return this;
    }

    public JsonParseException withRequestPayload(RequestPayload p) {
        this._requestPayload = p;
        return this;
    }

    public JsonParser getProcessor() {
        return this._processor;
    }

    public RequestPayload getRequestPayload() {
        return this._requestPayload;
    }

    public String getRequestPayloadAsString() {
        RequestPayload requestPayload = this._requestPayload;
        if (requestPayload != null) {
            return requestPayload.toString();
        }
        return null;
    }

    public String getMessage() {
        String msg = super.getMessage();
        if (this._requestPayload == null) {
            return msg;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(msg);
        sb.append("\nRequest payload : ");
        sb.append(this._requestPayload.toString());
        return sb.toString();
    }
}
