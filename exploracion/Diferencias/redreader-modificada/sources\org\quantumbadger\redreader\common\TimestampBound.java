package org.quantumbadger.redreader.common;

public abstract class TimestampBound {
    public static final TimestampBound ANY = new TimestampBound() {
        public boolean verifyTimestamp(long timestamp) {
            return true;
        }
    };
    public static final TimestampBound NONE = new TimestampBound() {
        public boolean verifyTimestamp(long timestamp) {
            return false;
        }
    };

    public static final class MoreRecentThanBound extends TimestampBound {
        private final long minTimestamp;

        public MoreRecentThanBound(long minTimestamp2) {
            this.minTimestamp = minTimestamp2;
        }

        public boolean verifyTimestamp(long timestamp) {
            return timestamp >= this.minTimestamp;
        }
    }

    public abstract boolean verifyTimestamp(long j);

    public static MoreRecentThanBound notOlderThan(long ageMs) {
        return new MoreRecentThanBound(System.currentTimeMillis() - ageMs);
    }
}
