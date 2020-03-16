package org.quantumbadger.redreader.common;

public class RRError {
    public final Integer httpStatus;
    public final String message;
    public final Throwable t;
    public final String title;
    public final String url;

    public RRError(String title2, String message2) {
        this(title2, message2, null, null, null);
    }

    public RRError(String title2, String message2, Throwable t2) {
        this(title2, message2, t2, null, null);
    }

    public RRError(String title2, String message2, Throwable t2, Integer httpStatus2, String url2) {
        this.title = title2;
        this.message = message2;
        this.t = t2;
        this.httpStatus = httpStatus2;
        this.url = url2;
    }
}
