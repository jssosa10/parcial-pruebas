package org.joda.time.tz;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.joda.time.DateTimeUtils;

public class DefaultNameProvider implements NameProvider {
    private HashMap<Locale, Map<String, Map<String, Object>>> iByLocaleCache = createCache();
    private HashMap<Locale, Map<String, Map<Boolean, Object>>> iByLocaleCache2 = createCache();

    public String getShortName(Locale locale, String str, String str2) {
        String[] nameSet = getNameSet(locale, str, str2);
        if (nameSet == null) {
            return null;
        }
        return nameSet[0];
    }

    public String getName(Locale locale, String str, String str2) {
        String[] nameSet = getNameSet(locale, str, str2);
        if (nameSet == null) {
            return null;
        }
        return nameSet[1];
    }

    /* JADX WARNING: Code restructure failed: missing block: B:46:0x00d4, code lost:
        return null;
     */
    private synchronized String[] getNameSet(Locale locale, String str, String str2) {
        String[] strArr;
        String[] strArr2 = null;
        if (locale != null && str != null && str2 != null) {
            Map map = (Map) this.iByLocaleCache.get(locale);
            if (map == null) {
                HashMap<Locale, Map<String, Map<String, Object>>> hashMap = this.iByLocaleCache;
                Map createCache = createCache();
                hashMap.put(locale, createCache);
                map = createCache;
            }
            Map map2 = (Map) map.get(str);
            if (map2 == null) {
                map2 = createCache();
                map.put(str, map2);
                String[][] zoneStrings = DateTimeUtils.getDateFormatSymbols(Locale.ENGLISH).getZoneStrings();
                int length = zoneStrings.length;
                int i = 0;
                while (true) {
                    if (i >= length) {
                        strArr = null;
                        break;
                    }
                    strArr = zoneStrings[i];
                    if (strArr != null && strArr.length >= 5 && str.equals(strArr[0])) {
                        break;
                    }
                    i++;
                }
                String[][] zoneStrings2 = DateTimeUtils.getDateFormatSymbols(locale).getZoneStrings();
                int length2 = zoneStrings2.length;
                int i2 = 0;
                while (true) {
                    if (i2 >= length2) {
                        break;
                    }
                    String[] strArr3 = zoneStrings2[i2];
                    if (strArr3 != null && strArr3.length >= 5 && str.equals(strArr3[0])) {
                        strArr2 = strArr3;
                        break;
                    }
                    i2++;
                }
                if (!(strArr == null || strArr2 == null)) {
                    map2.put(strArr[2], new String[]{strArr2[2], strArr2[1]});
                    if (strArr[2].equals(strArr[4])) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(strArr[4]);
                        sb.append("-Summer");
                        map2.put(sb.toString(), new String[]{strArr2[4], strArr2[3]});
                    } else {
                        map2.put(strArr[4], new String[]{strArr2[4], strArr2[3]});
                    }
                }
            }
            return (String[]) map2.get(str2);
        }
    }

    public String getShortName(Locale locale, String str, String str2, boolean z) {
        String[] nameSet = getNameSet(locale, str, str2, z);
        if (nameSet == null) {
            return null;
        }
        return nameSet[0];
    }

    public String getName(Locale locale, String str, String str2, boolean z) {
        String[] nameSet = getNameSet(locale, str, str2, z);
        if (nameSet == null) {
            return null;
        }
        return nameSet[1];
    }

    /* JADX WARNING: Code restructure failed: missing block: B:47:0x00b9, code lost:
        return null;
     */
    private synchronized String[] getNameSet(Locale locale, String str, String str2, boolean z) {
        String[] strArr;
        String[] strArr2 = null;
        if (locale != null && str != null && str2 != null) {
            if (str.startsWith("Etc/")) {
                str = str.substring(4);
            }
            Map map = (Map) this.iByLocaleCache2.get(locale);
            if (map == null) {
                HashMap<Locale, Map<String, Map<Boolean, Object>>> hashMap = this.iByLocaleCache2;
                Map createCache = createCache();
                hashMap.put(locale, createCache);
                map = createCache;
            }
            Map map2 = (Map) map.get(str);
            if (map2 == null) {
                map2 = createCache();
                map.put(str, map2);
                String[][] zoneStrings = DateTimeUtils.getDateFormatSymbols(Locale.ENGLISH).getZoneStrings();
                int length = zoneStrings.length;
                int i = 0;
                while (true) {
                    if (i >= length) {
                        strArr = null;
                        break;
                    }
                    strArr = zoneStrings[i];
                    if (strArr != null && strArr.length >= 5 && str.equals(strArr[0])) {
                        break;
                    }
                    i++;
                }
                String[][] zoneStrings2 = DateTimeUtils.getDateFormatSymbols(locale).getZoneStrings();
                int length2 = zoneStrings2.length;
                int i2 = 0;
                while (true) {
                    if (i2 >= length2) {
                        break;
                    }
                    String[] strArr3 = zoneStrings2[i2];
                    if (strArr3 != null && strArr3.length >= 5 && str.equals(strArr3[0])) {
                        strArr2 = strArr3;
                        break;
                    }
                    i2++;
                }
                if (!(strArr == null || strArr2 == null)) {
                    map2.put(Boolean.TRUE, new String[]{strArr2[2], strArr2[1]});
                    map2.put(Boolean.FALSE, new String[]{strArr2[4], strArr2[3]});
                }
            }
            return (String[]) map2.get(Boolean.valueOf(z));
        }
    }

    private HashMap createCache() {
        return new HashMap(7);
    }
}
