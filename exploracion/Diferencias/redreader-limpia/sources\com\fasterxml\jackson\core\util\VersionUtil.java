package com.fasterxml.jackson.core.util;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.Versioned;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Pattern;

public class VersionUtil {
    private static final Pattern V_SEP = Pattern.compile("[-_./;:]");

    protected VersionUtil() {
    }

    @Deprecated
    public Version version() {
        return Version.unknownVersion();
    }

    public static Version versionFor(Class<?> cls) {
        Version version = packageVersionFor(cls);
        return version == null ? Version.unknownVersion() : version;
    }

    public static Version packageVersionFor(Class<?> cls) {
        Class<?> vClass;
        Version v = null;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(cls.getPackage().getName());
            sb.append(".PackageVersion");
            vClass = Class.forName(sb.toString(), true, cls.getClassLoader());
            v = ((Versioned) vClass.getDeclaredConstructor(new Class[0]).newInstance(new Object[0])).version();
        } catch (Exception e) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Failed to get Versioned out of ");
            sb2.append(vClass);
            throw new IllegalArgumentException(sb2.toString());
        } catch (Exception e2) {
        }
        return v == null ? Version.unknownVersion() : v;
    }

    @Deprecated
    public static Version mavenVersionFor(ClassLoader cl, String groupId, String artifactId) {
        StringBuilder sb = new StringBuilder();
        sb.append("META-INF/maven/");
        sb.append(groupId.replaceAll("\\.", "/"));
        sb.append("/");
        sb.append(artifactId);
        sb.append("/pom.properties");
        InputStream pomProperties = cl.getResourceAsStream(sb.toString());
        if (pomProperties != null) {
            try {
                Properties props = new Properties();
                props.load(pomProperties);
                return parseVersion(props.getProperty("version"), props.getProperty("groupId"), props.getProperty("artifactId"));
            } catch (IOException e) {
            } finally {
                _close(pomProperties);
            }
        }
        return Version.unknownVersion();
    }

    public static Version parseVersion(String s, String groupId, String artifactId) {
        if (s != null) {
            String trim = s.trim();
            String s2 = trim;
            if (trim.length() > 0) {
                String[] parts = V_SEP.split(s2);
                Version version = new Version(parseVersionPart(parts[0]), parts.length > 1 ? parseVersionPart(parts[1]) : 0, parts.length > 2 ? parseVersionPart(parts[2]) : 0, parts.length > 3 ? parts[3] : null, groupId, artifactId);
                return version;
            }
        }
        return Version.unknownVersion();
    }

    protected static int parseVersionPart(String s) {
        int number = 0;
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c > '9' || c < '0') {
                break;
            }
            number = (number * 10) + (c - '0');
        }
        return number;
    }

    private static final void _close(Closeable c) {
        try {
            c.close();
        } catch (IOException e) {
        }
    }

    public static final void throwInternal() {
        throw new RuntimeException("Internal error: this code path should never get executed");
    }
}
