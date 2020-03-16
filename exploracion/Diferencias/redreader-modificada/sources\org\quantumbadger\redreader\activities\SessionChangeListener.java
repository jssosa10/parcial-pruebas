package org.quantumbadger.redreader.activities;

import java.util.UUID;

public interface SessionChangeListener {

    public enum SessionChangeType {
        POSTS,
        COMMENTS
    }

    void onSessionChanged(UUID uuid, SessionChangeType sessionChangeType, long j);

    void onSessionRefreshSelected(SessionChangeType sessionChangeType);

    void onSessionSelected(UUID uuid, SessionChangeType sessionChangeType);
}
