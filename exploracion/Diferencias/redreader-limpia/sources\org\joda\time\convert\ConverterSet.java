package org.joda.time.convert;

class ConverterSet {
    private final Converter[] iConverters;
    private Entry[] iSelectEntries = new Entry[16];

    static class Entry {
        final Converter iConverter;
        final Class<?> iType;

        Entry(Class<?> cls, Converter converter) {
            this.iType = cls;
            this.iConverter = converter;
        }
    }

    ConverterSet(Converter[] converterArr) {
        this.iConverters = converterArr;
    }

    /* access modifiers changed from: 0000 */
    public Converter select(Class<?> cls) throws IllegalStateException {
        Entry[] entryArr = this.iSelectEntries;
        int length = entryArr.length;
        int hashCode = cls == null ? 0 : cls.hashCode() & (length - 1);
        while (true) {
            Entry entry = entryArr[hashCode];
            if (entry == null) {
                Converter selectSlow = selectSlow(this, cls);
                Entry entry2 = new Entry(cls, selectSlow);
                Entry[] entryArr2 = (Entry[]) entryArr.clone();
                entryArr2[hashCode] = entry2;
                for (int i = 0; i < length; i++) {
                    if (entryArr2[i] == null) {
                        this.iSelectEntries = entryArr2;
                        return selectSlow;
                    }
                }
                int i2 = length << 1;
                Entry[] entryArr3 = new Entry[i2];
                for (int i3 = 0; i3 < length; i3++) {
                    Entry entry3 = entryArr2[i3];
                    Class<?> cls2 = entry3.iType;
                    int hashCode2 = cls2 == null ? 0 : cls2.hashCode() & (i2 - 1);
                    while (entryArr3[hashCode2] != null) {
                        int i4 = hashCode2 + 1;
                        if (i4 >= i2) {
                            i4 = 0;
                        }
                    }
                    entryArr3[hashCode2] = entry3;
                }
                this.iSelectEntries = entryArr3;
                return selectSlow;
            } else if (entry.iType == cls) {
                return entry.iConverter;
            } else {
                int i5 = hashCode + 1;
                if (i5 >= length) {
                    i5 = 0;
                }
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public int size() {
        return this.iConverters.length;
    }

    /* access modifiers changed from: 0000 */
    public void copyInto(Converter[] converterArr) {
        Converter[] converterArr2 = this.iConverters;
        System.arraycopy(converterArr2, 0, converterArr, 0, converterArr2.length);
    }

    /* access modifiers changed from: 0000 */
    public ConverterSet add(Converter converter, Converter[] converterArr) {
        Converter[] converterArr2 = this.iConverters;
        int length = converterArr2.length;
        int i = 0;
        while (i < length) {
            Converter converter2 = converterArr2[i];
            if (converter.equals(converter2)) {
                if (converterArr != null) {
                    converterArr[0] = null;
                }
                return this;
            } else if (converter.getSupportedType() == converter2.getSupportedType()) {
                Converter[] converterArr3 = new Converter[length];
                for (int i2 = 0; i2 < length; i2++) {
                    if (i2 != i) {
                        converterArr3[i2] = converterArr2[i2];
                    } else {
                        converterArr3[i2] = converter;
                    }
                }
                if (converterArr != null) {
                    converterArr[0] = converter2;
                }
                return new ConverterSet(converterArr3);
            } else {
                i++;
            }
        }
        Converter[] converterArr4 = new Converter[(length + 1)];
        System.arraycopy(converterArr2, 0, converterArr4, 0, length);
        converterArr4[length] = converter;
        if (converterArr != null) {
            converterArr[0] = null;
        }
        return new ConverterSet(converterArr4);
    }

    /* access modifiers changed from: 0000 */
    public ConverterSet remove(Converter converter, Converter[] converterArr) {
        Converter[] converterArr2 = this.iConverters;
        int length = converterArr2.length;
        for (int i = 0; i < length; i++) {
            if (converter.equals(converterArr2[i])) {
                return remove(i, converterArr);
            }
        }
        if (converterArr != null) {
            converterArr[0] = null;
        }
        return this;
    }

    /* access modifiers changed from: 0000 */
    public ConverterSet remove(int i, Converter[] converterArr) {
        Converter[] converterArr2 = this.iConverters;
        int length = converterArr2.length;
        if (i < length) {
            if (converterArr != null) {
                converterArr[0] = converterArr2[i];
            }
            Converter[] converterArr3 = new Converter[(length - 1)];
            int i2 = 0;
            for (int i3 = 0; i3 < length; i3++) {
                if (i3 != i) {
                    int i4 = i2 + 1;
                    converterArr3[i2] = converterArr2[i3];
                    i2 = i4;
                }
            }
            return new ConverterSet(converterArr3);
        }
        throw new IndexOutOfBoundsException();
    }

    private static Converter selectSlow(ConverterSet converterSet, Class<?> cls) {
        Converter[] converterArr = converterSet.iConverters;
        int length = converterArr.length;
        ConverterSet converterSet2 = converterSet;
        int i = length;
        while (true) {
            length--;
            if (length >= 0) {
                Converter converter = converterArr[length];
                Class<?> supportedType = converter.getSupportedType();
                if (supportedType == cls) {
                    return converter;
                }
                if (supportedType == null || (cls != null && !supportedType.isAssignableFrom(cls))) {
                    ConverterSet remove = converterSet2.remove(length, (Converter[]) null);
                    converterArr = remove.iConverters;
                    converterSet2 = remove;
                    i = converterArr.length;
                }
            } else if (cls == null || i == 0) {
                return null;
            } else {
                if (i == 1) {
                    return converterArr[0];
                }
                ConverterSet converterSet3 = converterSet2;
                Converter[] converterArr2 = converterArr;
                int i2 = i;
                while (true) {
                    int i3 = i - 1;
                    if (i3 < 0) {
                        break;
                    }
                    Class supportedType2 = converterArr2[i3].getSupportedType();
                    ConverterSet converterSet4 = converterSet3;
                    int i4 = i3;
                    int i5 = i2;
                    while (true) {
                        i2--;
                        if (i2 < 0) {
                            break;
                        } else if (i2 != i4 && converterArr2[i2].getSupportedType().isAssignableFrom(supportedType2)) {
                            converterSet4 = converterSet4.remove(i2, (Converter[]) null);
                            converterArr2 = converterSet4.iConverters;
                            i5 = converterArr2.length;
                            i4 = i5 - 1;
                        }
                    }
                    i2 = i5;
                    i = i4;
                    converterSet3 = converterSet4;
                }
                if (i2 == 1) {
                    return converterArr2[0];
                }
                StringBuilder sb = new StringBuilder();
                sb.append("Unable to find best converter for type \"");
                sb.append(cls.getName());
                sb.append("\" from remaining set: ");
                for (int i6 = 0; i6 < i2; i6++) {
                    Converter converter2 = converterArr2[i6];
                    Class supportedType3 = converter2.getSupportedType();
                    sb.append(converter2.getClass().getName());
                    sb.append('[');
                    sb.append(supportedType3 == null ? null : supportedType3.getName());
                    sb.append("], ");
                }
                throw new IllegalStateException(sb.toString());
            }
        }
    }
}
