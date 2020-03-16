package org.quantumbadger.redreader.reddit.api;

import android.annotation.SuppressLint;
import android.content.Context;
import java.net.URI;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.RRError;

public class SubredditRequestFailure {
    public final String readableMessage;
    public final int requestFailureType;
    public final Integer statusLine;
    public final Throwable t;
    public final String url;

    public SubredditRequestFailure(int requestFailureType2, Throwable t2, Integer statusLine2, String readableMessage2, String url2) {
        this.requestFailureType = requestFailureType2;
        this.t = t2;
        this.statusLine = statusLine2;
        this.readableMessage = readableMessage2;
        this.url = url2;
    }

    public SubredditRequestFailure(int requestFailureType2, Throwable t2, Integer statusLine2, String readableMessage2, URI url2) {
        this(requestFailureType2, t2, statusLine2, readableMessage2, url2 != null ? url2.toString() : null);
    }

    @SuppressLint({"WrongConstant"})
    public RRError asError(Context context) {
        return General.getGeneralErrorForFailure(context, this.requestFailureType, this.t, this.statusLine, this.url);
    }
}
