package com.google.android.exoplayer2.source;

public final class DefaultCompositeSequenceableLoaderFactory implements CompositeSequenceableLoaderFactory {
    public SequenceableLoader createCompositeSequenceableLoader(SequenceableLoader... loaders) {
        return new CompositeSequenceableLoader(loaders);
    }
}
