package com.google.android.exoplayer2.offline;

import android.net.Uri;
import com.google.android.exoplayer2.offline.FilterableManifest;
import com.google.android.exoplayer2.upstream.ParsingLoadable.Parser;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public final class FilteringManifestParser<T extends FilterableManifest<T>> implements Parser<T> {
    private final Parser<T> parser;
    private final List<StreamKey> streamKeys;

    public FilteringManifestParser(Parser<T> parser2, List<StreamKey> streamKeys2) {
        this.parser = parser2;
        this.streamKeys = streamKeys2;
    }

    public T parse(Uri uri, InputStream inputStream) throws IOException {
        T manifest = (FilterableManifest) this.parser.parse(uri, inputStream);
        List<StreamKey> list = this.streamKeys;
        return (list == null || list.isEmpty()) ? manifest : (FilterableManifest) manifest.copy(this.streamKeys);
    }
}
