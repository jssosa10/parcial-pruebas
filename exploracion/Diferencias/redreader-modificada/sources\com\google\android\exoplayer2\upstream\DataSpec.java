package com.google.android.exoplayer2.upstream;

import android.net.Uri;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.util.Assertions;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;

public final class DataSpec {
    public static final int FLAG_ALLOW_CACHING_UNKNOWN_LENGTH = 2;
    public static final int FLAG_ALLOW_GZIP = 1;
    public static final int HTTP_METHOD_GET = 1;
    public static final int HTTP_METHOD_HEAD = 3;
    public static final int HTTP_METHOD_POST = 2;
    public final long absoluteStreamPosition;
    public final int flags;
    @Nullable
    public final byte[] httpBody;
    public final int httpMethod;
    @Nullable
    public final String key;
    public final long length;
    public final long position;
    @Nullable
    @Deprecated
    public final byte[] postBody;
    public final Uri uri;

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface Flags {
    }

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface HttpMethod {
    }

    public DataSpec(Uri uri2) {
        this(uri2, 0);
    }

    public DataSpec(Uri uri2, int flags2) {
        this(uri2, 0, -1, null, flags2);
    }

    public DataSpec(Uri uri2, long absoluteStreamPosition2, long length2, @Nullable String key2) {
        this(uri2, absoluteStreamPosition2, absoluteStreamPosition2, length2, key2, 0);
    }

    public DataSpec(Uri uri2, long absoluteStreamPosition2, long length2, @Nullable String key2, int flags2) {
        this(uri2, absoluteStreamPosition2, absoluteStreamPosition2, length2, key2, flags2);
    }

    public DataSpec(Uri uri2, long absoluteStreamPosition2, long position2, long length2, @Nullable String key2, int flags2) {
        this(uri2, null, absoluteStreamPosition2, position2, length2, key2, flags2);
    }

    public DataSpec(Uri uri2, @Nullable byte[] postBody2, long absoluteStreamPosition2, long position2, long length2, @Nullable String key2, int flags2) {
        this(uri2, postBody2 != null ? 2 : 1, postBody2, absoluteStreamPosition2, position2, length2, key2, flags2);
    }

    public DataSpec(Uri uri2, int httpMethod2, @Nullable byte[] httpBody2, long absoluteStreamPosition2, long position2, long length2, @Nullable String key2, int flags2) {
        byte[] bArr = httpBody2;
        long j = absoluteStreamPosition2;
        long j2 = position2;
        long j3 = length2;
        boolean z = true;
        Assertions.checkArgument(j >= 0);
        Assertions.checkArgument(j2 >= 0);
        if (j3 <= 0 && j3 != -1) {
            z = false;
        }
        Assertions.checkArgument(z);
        this.uri = uri2;
        this.httpMethod = httpMethod2;
        this.httpBody = (bArr == null || bArr.length == 0) ? null : bArr;
        this.postBody = this.httpBody;
        this.absoluteStreamPosition = j;
        this.position = j2;
        this.length = j3;
        this.key = key2;
        this.flags = flags2;
    }

    public boolean isFlagSet(int flag) {
        return (this.flags & flag) == flag;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DataSpec[");
        sb.append(getHttpMethodString());
        sb.append(StringUtils.SPACE);
        sb.append(this.uri);
        sb.append(", ");
        sb.append(Arrays.toString(this.httpBody));
        sb.append(", ");
        sb.append(this.absoluteStreamPosition);
        sb.append(", ");
        sb.append(this.position);
        sb.append(", ");
        sb.append(this.length);
        sb.append(", ");
        sb.append(this.key);
        sb.append(", ");
        sb.append(this.flags);
        sb.append("]");
        return sb.toString();
    }

    public final String getHttpMethodString() {
        return getStringForHttpMethod(this.httpMethod);
    }

    public static String getStringForHttpMethod(int httpMethod2) {
        switch (httpMethod2) {
            case 1:
                return "GET";
            case 2:
                return "POST";
            case 3:
                return "HEAD";
            default:
                throw new AssertionError(httpMethod2);
        }
    }

    public DataSpec subrange(long offset) {
        long j = this.length;
        long j2 = -1;
        if (j != -1) {
            j2 = j - offset;
        }
        return subrange(offset, j2);
    }

    public DataSpec subrange(long offset, long length2) {
        if (offset == 0 && this.length == length2) {
            return this;
        }
        DataSpec dataSpec = new DataSpec(this.uri, this.httpMethod, this.httpBody, this.absoluteStreamPosition + offset, this.position + offset, length2, this.key, this.flags);
        return dataSpec;
    }

    public DataSpec withUri(Uri uri2) {
        DataSpec dataSpec = new DataSpec(uri2, this.httpMethod, this.httpBody, this.absoluteStreamPosition, this.position, this.length, this.key, this.flags);
        return dataSpec;
    }
}
