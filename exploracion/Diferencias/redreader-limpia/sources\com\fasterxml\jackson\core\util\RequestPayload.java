package com.fasterxml.jackson.core.util;

import java.io.IOException;
import java.io.Serializable;

public class RequestPayload implements Serializable {
    private static final long serialVersionUID = 1;
    protected String _charset;
    protected byte[] _payloadAsBytes;
    protected CharSequence _payloadAsText;

    public RequestPayload(byte[] bytes, String charset) {
        if (bytes != null) {
            this._payloadAsBytes = bytes;
            this._charset = (charset == null || charset.isEmpty()) ? "UTF-8" : charset;
            return;
        }
        throw new IllegalArgumentException();
    }

    public RequestPayload(CharSequence str) {
        if (str != null) {
            this._payloadAsText = str;
            return;
        }
        throw new IllegalArgumentException();
    }

    public Object getRawPayload() {
        byte[] bArr = this._payloadAsBytes;
        if (bArr != null) {
            return bArr;
        }
        return this._payloadAsText;
    }

    public String toString() {
        byte[] bArr = this._payloadAsBytes;
        if (bArr == null) {
            return this._payloadAsText.toString();
        }
        try {
            return new String(bArr, this._charset);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
