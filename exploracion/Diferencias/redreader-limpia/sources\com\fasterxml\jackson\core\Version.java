package com.fasterxml.jackson.core;

import java.io.Serializable;
import org.apache.commons.lang3.ClassUtils;

public class Version implements Comparable<Version>, Serializable {
    private static final Version UNKNOWN_VERSION;
    private static final long serialVersionUID = 1;
    protected final String _artifactId;
    protected final String _groupId;
    protected final int _majorVersion;
    protected final int _minorVersion;
    protected final int _patchLevel;
    protected final String _snapshotInfo;

    static {
        Version version = new Version(0, 0, 0, null, null, null);
        UNKNOWN_VERSION = version;
    }

    @Deprecated
    public Version(int major, int minor, int patchLevel, String snapshotInfo) {
        this(major, minor, patchLevel, snapshotInfo, null, null);
    }

    public Version(int major, int minor, int patchLevel, String snapshotInfo, String groupId, String artifactId) {
        this._majorVersion = major;
        this._minorVersion = minor;
        this._patchLevel = patchLevel;
        this._snapshotInfo = snapshotInfo;
        this._groupId = groupId == null ? "" : groupId;
        this._artifactId = artifactId == null ? "" : artifactId;
    }

    public static Version unknownVersion() {
        return UNKNOWN_VERSION;
    }

    public boolean isUnknownVersion() {
        return this == UNKNOWN_VERSION;
    }

    public boolean isSnapshot() {
        String str = this._snapshotInfo;
        return str != null && str.length() > 0;
    }

    @Deprecated
    public boolean isUknownVersion() {
        return isUnknownVersion();
    }

    public int getMajorVersion() {
        return this._majorVersion;
    }

    public int getMinorVersion() {
        return this._minorVersion;
    }

    public int getPatchLevel() {
        return this._patchLevel;
    }

    public String getGroupId() {
        return this._groupId;
    }

    public String getArtifactId() {
        return this._artifactId;
    }

    public String toFullString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this._groupId);
        sb.append(JsonPointer.SEPARATOR);
        sb.append(this._artifactId);
        sb.append(JsonPointer.SEPARATOR);
        sb.append(toString());
        return sb.toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this._majorVersion);
        sb.append(ClassUtils.PACKAGE_SEPARATOR_CHAR);
        sb.append(this._minorVersion);
        sb.append(ClassUtils.PACKAGE_SEPARATOR_CHAR);
        sb.append(this._patchLevel);
        if (isSnapshot()) {
            sb.append('-');
            sb.append(this._snapshotInfo);
        }
        return sb.toString();
    }

    public int hashCode() {
        return this._artifactId.hashCode() ^ (((this._groupId.hashCode() + this._majorVersion) - this._minorVersion) + this._patchLevel);
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (o == this) {
            return true;
        }
        if (o == null || o.getClass() != getClass()) {
            return false;
        }
        Version other = (Version) o;
        if (!(other._majorVersion == this._majorVersion && other._minorVersion == this._minorVersion && other._patchLevel == this._patchLevel && other._artifactId.equals(this._artifactId) && other._groupId.equals(this._groupId))) {
            z = false;
        }
        return z;
    }

    public int compareTo(Version other) {
        if (other == this) {
            return 0;
        }
        int diff = this._groupId.compareTo(other._groupId);
        if (diff == 0) {
            diff = this._artifactId.compareTo(other._artifactId);
            if (diff == 0) {
                diff = this._majorVersion - other._majorVersion;
                if (diff == 0) {
                    diff = this._minorVersion - other._minorVersion;
                    if (diff == 0) {
                        diff = this._patchLevel - other._patchLevel;
                    }
                }
            }
        }
        return diff;
    }
}
