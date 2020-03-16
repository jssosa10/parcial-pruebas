package org.apache.commons.lang3.builder;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

public class MultilineRecursiveToStringStyle extends RecursiveToStringStyle {
    private static final int INDENT = 2;
    private static final long serialVersionUID = 1;
    private int spaces = 2;

    public MultilineRecursiveToStringStyle() {
        resetIndent();
    }

    private void resetIndent() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append(System.lineSeparator());
        sb.append(spacer(this.spaces));
        setArrayStart(sb.toString());
        StringBuilder sb2 = new StringBuilder();
        sb2.append(",");
        sb2.append(System.lineSeparator());
        sb2.append(spacer(this.spaces));
        setArraySeparator(sb2.toString());
        StringBuilder sb3 = new StringBuilder();
        sb3.append(System.lineSeparator());
        sb3.append(spacer(this.spaces - 2));
        sb3.append("}");
        setArrayEnd(sb3.toString());
        StringBuilder sb4 = new StringBuilder();
        sb4.append("[");
        sb4.append(System.lineSeparator());
        sb4.append(spacer(this.spaces));
        setContentStart(sb4.toString());
        StringBuilder sb5 = new StringBuilder();
        sb5.append(",");
        sb5.append(System.lineSeparator());
        sb5.append(spacer(this.spaces));
        setFieldSeparator(sb5.toString());
        StringBuilder sb6 = new StringBuilder();
        sb6.append(System.lineSeparator());
        sb6.append(spacer(this.spaces - 2));
        sb6.append("]");
        setContentEnd(sb6.toString());
    }

    private StringBuilder spacer(int spaces2) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < spaces2; i++) {
            sb.append(StringUtils.SPACE);
        }
        return sb;
    }

    public void appendDetail(StringBuffer buffer, String fieldName, Object value) {
        if (ClassUtils.isPrimitiveWrapper(value.getClass()) || String.class.equals(value.getClass()) || !accept(value.getClass())) {
            super.appendDetail(buffer, fieldName, value);
            return;
        }
        this.spaces += 2;
        resetIndent();
        buffer.append(ReflectionToStringBuilder.toString(value, this));
        this.spaces -= 2;
        resetIndent();
    }

    /* access modifiers changed from: protected */
    public void appendDetail(StringBuffer buffer, String fieldName, Object[] array) {
        this.spaces += 2;
        resetIndent();
        super.appendDetail(buffer, fieldName, array);
        this.spaces -= 2;
        resetIndent();
    }

    /* access modifiers changed from: protected */
    public void reflectionAppendArrayDetail(StringBuffer buffer, String fieldName, Object array) {
        this.spaces += 2;
        resetIndent();
        super.reflectionAppendArrayDetail(buffer, fieldName, array);
        this.spaces -= 2;
        resetIndent();
    }

    /* access modifiers changed from: protected */
    public void appendDetail(StringBuffer buffer, String fieldName, long[] array) {
        this.spaces += 2;
        resetIndent();
        super.appendDetail(buffer, fieldName, array);
        this.spaces -= 2;
        resetIndent();
    }

    /* access modifiers changed from: protected */
    public void appendDetail(StringBuffer buffer, String fieldName, int[] array) {
        this.spaces += 2;
        resetIndent();
        super.appendDetail(buffer, fieldName, array);
        this.spaces -= 2;
        resetIndent();
    }

    /* access modifiers changed from: protected */
    public void appendDetail(StringBuffer buffer, String fieldName, short[] array) {
        this.spaces += 2;
        resetIndent();
        super.appendDetail(buffer, fieldName, array);
        this.spaces -= 2;
        resetIndent();
    }

    /* access modifiers changed from: protected */
    public void appendDetail(StringBuffer buffer, String fieldName, byte[] array) {
        this.spaces += 2;
        resetIndent();
        super.appendDetail(buffer, fieldName, array);
        this.spaces -= 2;
        resetIndent();
    }

    /* access modifiers changed from: protected */
    public void appendDetail(StringBuffer buffer, String fieldName, char[] array) {
        this.spaces += 2;
        resetIndent();
        super.appendDetail(buffer, fieldName, array);
        this.spaces -= 2;
        resetIndent();
    }

    /* access modifiers changed from: protected */
    public void appendDetail(StringBuffer buffer, String fieldName, double[] array) {
        this.spaces += 2;
        resetIndent();
        super.appendDetail(buffer, fieldName, array);
        this.spaces -= 2;
        resetIndent();
    }

    /* access modifiers changed from: protected */
    public void appendDetail(StringBuffer buffer, String fieldName, float[] array) {
        this.spaces += 2;
        resetIndent();
        super.appendDetail(buffer, fieldName, array);
        this.spaces -= 2;
        resetIndent();
    }

    /* access modifiers changed from: protected */
    public void appendDetail(StringBuffer buffer, String fieldName, boolean[] array) {
        this.spaces += 2;
        resetIndent();
        super.appendDetail(buffer, fieldName, array);
        this.spaces -= 2;
        resetIndent();
    }
}
