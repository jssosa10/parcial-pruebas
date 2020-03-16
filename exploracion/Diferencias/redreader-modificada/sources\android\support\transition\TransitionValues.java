package android.support.transition;

import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public class TransitionValues {
    final ArrayList<Transition> mTargetedTransitions = new ArrayList<>();
    public final Map<String, Object> values = new HashMap();
    public View view;

    public boolean equals(Object other) {
        if (!(other instanceof TransitionValues) || this.view != ((TransitionValues) other).view || !this.values.equals(((TransitionValues) other).values)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return (this.view.hashCode() * 31) + this.values.hashCode();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TransitionValues@");
        sb.append(Integer.toHexString(hashCode()));
        sb.append(":\n");
        String returnValue = sb.toString();
        StringBuilder sb2 = new StringBuilder();
        sb2.append(returnValue);
        sb2.append("    view = ");
        sb2.append(this.view);
        sb2.append(StringUtils.LF);
        String returnValue2 = sb2.toString();
        StringBuilder sb3 = new StringBuilder();
        sb3.append(returnValue2);
        sb3.append("    values:");
        String returnValue3 = sb3.toString();
        for (String s : this.values.keySet()) {
            StringBuilder sb4 = new StringBuilder();
            sb4.append(returnValue3);
            sb4.append("    ");
            sb4.append(s);
            sb4.append(": ");
            sb4.append(this.values.get(s));
            sb4.append(StringUtils.LF);
            returnValue3 = sb4.toString();
        }
        return returnValue3;
    }
}
