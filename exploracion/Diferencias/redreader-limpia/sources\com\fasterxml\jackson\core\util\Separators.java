package com.fasterxml.jackson.core.util;

import java.io.Serializable;

public class Separators implements Serializable {
    private static final long serialVersionUID = 1;
    private final char arrayValueSeparator;
    private final char objectEntrySeparator;
    private final char objectFieldValueSeparator;

    public static Separators createDefaultInstance() {
        return new Separators();
    }

    public Separators() {
        this(':', ',', ',');
    }

    public Separators(char objectFieldValueSeparator2, char objectEntrySeparator2, char arrayValueSeparator2) {
        this.objectFieldValueSeparator = objectFieldValueSeparator2;
        this.objectEntrySeparator = objectEntrySeparator2;
        this.arrayValueSeparator = arrayValueSeparator2;
    }

    public Separators withObjectFieldValueSeparator(char sep) {
        return this.objectFieldValueSeparator == sep ? this : new Separators(sep, this.objectEntrySeparator, this.arrayValueSeparator);
    }

    public Separators withObjectEntrySeparator(char sep) {
        return this.objectEntrySeparator == sep ? this : new Separators(this.objectFieldValueSeparator, sep, this.arrayValueSeparator);
    }

    public Separators withArrayValueSeparator(char sep) {
        return this.arrayValueSeparator == sep ? this : new Separators(this.objectFieldValueSeparator, this.objectEntrySeparator, sep);
    }

    public char getObjectFieldValueSeparator() {
        return this.objectFieldValueSeparator;
    }

    public char getObjectEntrySeparator() {
        return this.objectEntrySeparator;
    }

    public char getArrayValueSeparator() {
        return this.arrayValueSeparator;
    }
}
