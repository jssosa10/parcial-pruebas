package com.google.android.exoplayer2.text.ttml;

import android.text.Layout.Alignment;
import android.text.SpannableStringBuilder;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.util.Assertions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

final class TtmlNode {
    public static final String ANONYMOUS_REGION_ID = "";
    public static final String ATTR_ID = "id";
    public static final String ATTR_TTS_BACKGROUND_COLOR = "backgroundColor";
    public static final String ATTR_TTS_COLOR = "color";
    public static final String ATTR_TTS_DISPLAY_ALIGN = "displayAlign";
    public static final String ATTR_TTS_EXTENT = "extent";
    public static final String ATTR_TTS_FONT_FAMILY = "fontFamily";
    public static final String ATTR_TTS_FONT_SIZE = "fontSize";
    public static final String ATTR_TTS_FONT_STYLE = "fontStyle";
    public static final String ATTR_TTS_FONT_WEIGHT = "fontWeight";
    public static final String ATTR_TTS_ORIGIN = "origin";
    public static final String ATTR_TTS_TEXT_ALIGN = "textAlign";
    public static final String ATTR_TTS_TEXT_DECORATION = "textDecoration";
    public static final String BOLD = "bold";
    public static final String CENTER = "center";
    public static final String END = "end";
    public static final String ITALIC = "italic";
    public static final String LEFT = "left";
    public static final String LINETHROUGH = "linethrough";
    public static final String NO_LINETHROUGH = "nolinethrough";
    public static final String NO_UNDERLINE = "nounderline";
    public static final String RIGHT = "right";
    public static final String START = "start";
    public static final String TAG_BODY = "body";
    public static final String TAG_BR = "br";
    public static final String TAG_DIV = "div";
    public static final String TAG_HEAD = "head";
    public static final String TAG_LAYOUT = "layout";
    public static final String TAG_METADATA = "metadata";
    public static final String TAG_P = "p";
    public static final String TAG_REGION = "region";
    public static final String TAG_SMPTE_DATA = "smpte:data";
    public static final String TAG_SMPTE_IMAGE = "smpte:image";
    public static final String TAG_SMPTE_INFORMATION = "smpte:information";
    public static final String TAG_SPAN = "span";
    public static final String TAG_STYLE = "style";
    public static final String TAG_STYLING = "styling";
    public static final String TAG_TT = "tt";
    public static final String UNDERLINE = "underline";
    private List<TtmlNode> children;
    public final long endTimeUs;
    public final boolean isTextNode;
    private final HashMap<String, Integer> nodeEndsByRegion;
    private final HashMap<String, Integer> nodeStartsByRegion;
    public final String regionId;
    public final long startTimeUs;
    public final TtmlStyle style;
    private final String[] styleIds;
    public final String tag;
    public final String text;

    public static TtmlNode buildTextNode(String text2) {
        TtmlNode ttmlNode = new TtmlNode(null, TtmlRenderUtil.applyTextElementSpacePolicy(text2), C.TIME_UNSET, C.TIME_UNSET, null, null, "");
        return ttmlNode;
    }

    public static TtmlNode buildNode(String tag2, long startTimeUs2, long endTimeUs2, TtmlStyle style2, String[] styleIds2, String regionId2) {
        TtmlNode ttmlNode = new TtmlNode(tag2, null, startTimeUs2, endTimeUs2, style2, styleIds2, regionId2);
        return ttmlNode;
    }

    private TtmlNode(String tag2, String text2, long startTimeUs2, long endTimeUs2, TtmlStyle style2, String[] styleIds2, String regionId2) {
        this.tag = tag2;
        this.text = text2;
        this.style = style2;
        this.styleIds = styleIds2;
        this.isTextNode = text2 != null;
        this.startTimeUs = startTimeUs2;
        this.endTimeUs = endTimeUs2;
        this.regionId = (String) Assertions.checkNotNull(regionId2);
        this.nodeStartsByRegion = new HashMap<>();
        this.nodeEndsByRegion = new HashMap<>();
    }

    public boolean isActive(long timeUs) {
        return (this.startTimeUs == C.TIME_UNSET && this.endTimeUs == C.TIME_UNSET) || (this.startTimeUs <= timeUs && this.endTimeUs == C.TIME_UNSET) || ((this.startTimeUs == C.TIME_UNSET && timeUs < this.endTimeUs) || (this.startTimeUs <= timeUs && timeUs < this.endTimeUs));
    }

    public void addChild(TtmlNode child) {
        if (this.children == null) {
            this.children = new ArrayList();
        }
        this.children.add(child);
    }

    public TtmlNode getChild(int index) {
        List<TtmlNode> list = this.children;
        if (list != null) {
            return (TtmlNode) list.get(index);
        }
        throw new IndexOutOfBoundsException();
    }

    public int getChildCount() {
        List<TtmlNode> list = this.children;
        if (list == null) {
            return 0;
        }
        return list.size();
    }

    public long[] getEventTimesUs() {
        TreeSet<Long> eventTimeSet = new TreeSet<>();
        getEventTimes(eventTimeSet, false);
        long[] eventTimes = new long[eventTimeSet.size()];
        int i = 0;
        Iterator it = eventTimeSet.iterator();
        while (it.hasNext()) {
            int i2 = i + 1;
            eventTimes[i] = ((Long) it.next()).longValue();
            i = i2;
        }
        return eventTimes;
    }

    private void getEventTimes(TreeSet<Long> out, boolean descendsPNode) {
        boolean isPNode = TAG_P.equals(this.tag);
        if (descendsPNode || isPNode) {
            long j = this.startTimeUs;
            if (j != C.TIME_UNSET) {
                out.add(Long.valueOf(j));
            }
            long j2 = this.endTimeUs;
            if (j2 != C.TIME_UNSET) {
                out.add(Long.valueOf(j2));
            }
        }
        if (this.children != null) {
            for (int i = 0; i < this.children.size(); i++) {
                ((TtmlNode) this.children.get(i)).getEventTimes(out, descendsPNode || isPNode);
            }
        }
    }

    public String[] getStyleIds() {
        return this.styleIds;
    }

    public List<Cue> getCues(long timeUs, Map<String, TtmlStyle> globalStyles, Map<String, TtmlRegion> regionMap) {
        TreeMap treeMap = new TreeMap();
        traverseForText(timeUs, false, this.regionId, treeMap);
        traverseForStyle(timeUs, globalStyles, treeMap);
        List<Cue> cues = new ArrayList<>();
        for (Entry<String, SpannableStringBuilder> entry : treeMap.entrySet()) {
            TtmlRegion region = (TtmlRegion) regionMap.get(entry.getKey());
            SpannableStringBuilder cleanUpText = cleanUpText((SpannableStringBuilder) entry.getValue());
            float f = region.line;
            int i = region.lineType;
            int i2 = region.lineAnchor;
            Cue cue = r10;
            int i3 = i2;
            Cue cue2 = new Cue((CharSequence) cleanUpText, (Alignment) null, f, i, i3, region.position, Integer.MIN_VALUE, region.width, region.textSizeType, region.textSize);
            cues.add(cue);
        }
        Map<String, TtmlRegion> map = regionMap;
        return cues;
    }

    private void traverseForText(long timeUs, boolean descendsPNode, String inheritedRegion, Map<String, SpannableStringBuilder> regionOutputs) {
        this.nodeStartsByRegion.clear();
        this.nodeEndsByRegion.clear();
        if (!TAG_METADATA.equals(this.tag)) {
            String resolvedRegionId = "".equals(this.regionId) ? inheritedRegion : this.regionId;
            if (this.isTextNode && descendsPNode) {
                getRegionOutput(resolvedRegionId, regionOutputs).append(this.text);
            } else if (TAG_BR.equals(this.tag) && descendsPNode) {
                getRegionOutput(resolvedRegionId, regionOutputs).append(10);
            } else if (isActive(timeUs)) {
                for (Entry<String, SpannableStringBuilder> entry : regionOutputs.entrySet()) {
                    this.nodeStartsByRegion.put(entry.getKey(), Integer.valueOf(((SpannableStringBuilder) entry.getValue()).length()));
                }
                boolean isPNode = TAG_P.equals(this.tag);
                for (int i = 0; i < getChildCount(); i++) {
                    getChild(i).traverseForText(timeUs, descendsPNode || isPNode, resolvedRegionId, regionOutputs);
                }
                if (isPNode) {
                    TtmlRenderUtil.endParagraph(getRegionOutput(resolvedRegionId, regionOutputs));
                }
                for (Entry<String, SpannableStringBuilder> entry2 : regionOutputs.entrySet()) {
                    this.nodeEndsByRegion.put(entry2.getKey(), Integer.valueOf(((SpannableStringBuilder) entry2.getValue()).length()));
                }
            }
        }
    }

    private static SpannableStringBuilder getRegionOutput(String resolvedRegionId, Map<String, SpannableStringBuilder> regionOutputs) {
        if (!regionOutputs.containsKey(resolvedRegionId)) {
            regionOutputs.put(resolvedRegionId, new SpannableStringBuilder());
        }
        return (SpannableStringBuilder) regionOutputs.get(resolvedRegionId);
    }

    private void traverseForStyle(long timeUs, Map<String, TtmlStyle> globalStyles, Map<String, SpannableStringBuilder> regionOutputs) {
        if (isActive(timeUs)) {
            for (Entry<String, Integer> entry : this.nodeEndsByRegion.entrySet()) {
                String regionId2 = (String) entry.getKey();
                int start = this.nodeStartsByRegion.containsKey(regionId2) ? ((Integer) this.nodeStartsByRegion.get(regionId2)).intValue() : 0;
                int end = ((Integer) entry.getValue()).intValue();
                if (start != end) {
                    applyStyleToOutput(globalStyles, (SpannableStringBuilder) regionOutputs.get(regionId2), start, end);
                }
            }
            for (int i = 0; i < getChildCount(); i++) {
                getChild(i).traverseForStyle(timeUs, globalStyles, regionOutputs);
            }
        }
    }

    private void applyStyleToOutput(Map<String, TtmlStyle> globalStyles, SpannableStringBuilder regionOutput, int start, int end) {
        TtmlStyle resolvedStyle = TtmlRenderUtil.resolveStyle(this.style, this.styleIds, globalStyles);
        if (resolvedStyle != null) {
            TtmlRenderUtil.applyStylesToSpan(regionOutput, start, end, resolvedStyle);
        }
    }

    private SpannableStringBuilder cleanUpText(SpannableStringBuilder builder) {
        int builderLength = builder.length();
        for (int i = 0; i < builderLength; i++) {
            if (builder.charAt(i) == ' ') {
                int j = i + 1;
                while (j < builder.length() && builder.charAt(j) == ' ') {
                    j++;
                }
                int spacesToDelete = j - (i + 1);
                if (spacesToDelete > 0) {
                    builder.delete(i, i + spacesToDelete);
                    builderLength -= spacesToDelete;
                }
            }
        }
        if (builderLength > 0 && builder.charAt(0) == ' ') {
            builder.delete(0, 1);
            builderLength--;
        }
        for (int i2 = 0; i2 < builderLength - 1; i2++) {
            if (builder.charAt(i2) == 10 && builder.charAt(i2 + 1) == ' ') {
                builder.delete(i2 + 1, i2 + 2);
                builderLength--;
            }
        }
        if (builderLength > 0 && builder.charAt(builderLength - 1) == ' ') {
            builder.delete(builderLength - 1, builderLength);
            builderLength--;
        }
        for (int i3 = 0; i3 < builderLength - 1; i3++) {
            if (builder.charAt(i3) == ' ' && builder.charAt(i3 + 1) == 10) {
                builder.delete(i3, i3 + 1);
                builderLength--;
            }
        }
        if (builderLength > 0 && builder.charAt(builderLength - 1) == 10) {
            builder.delete(builderLength - 1, builderLength);
        }
        return builder;
    }
}
