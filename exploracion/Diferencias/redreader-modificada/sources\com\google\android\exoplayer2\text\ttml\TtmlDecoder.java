package com.google.android.exoplayer2.text.ttml;

import android.text.Layout.Alignment;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.text.SimpleSubtitleDecoder;
import com.google.android.exoplayer2.text.SubtitleDecoderException;
import com.google.android.exoplayer2.util.ColorParser;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.util.XmlPullParserUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public final class TtmlDecoder extends SimpleSubtitleDecoder {
    private static final String ATTR_BEGIN = "begin";
    private static final String ATTR_DURATION = "dur";
    private static final String ATTR_END = "end";
    private static final String ATTR_REGION = "region";
    private static final String ATTR_STYLE = "style";
    private static final Pattern CELL_RESOLUTION = Pattern.compile("^(\\d+) (\\d+)$");
    private static final Pattern CLOCK_TIME = Pattern.compile("^([0-9][0-9]+):([0-9][0-9]):([0-9][0-9])(?:(\\.[0-9]+)|:([0-9][0-9])(?:\\.([0-9]+))?)?$");
    private static final CellResolution DEFAULT_CELL_RESOLUTION = new CellResolution(32, 15);
    private static final FrameAndTickRate DEFAULT_FRAME_AND_TICK_RATE = new FrameAndTickRate(30.0f, 1, 1);
    private static final int DEFAULT_FRAME_RATE = 30;
    private static final Pattern FONT_SIZE = Pattern.compile("^(([0-9]*.)?[0-9]+)(px|em|%)$");
    private static final Pattern OFFSET_TIME = Pattern.compile("^([0-9]+(?:\\.[0-9]+)?)(h|m|s|ms|f|t)$");
    private static final Pattern PERCENTAGE_COORDINATES = Pattern.compile("^(\\d+\\.?\\d*?)% (\\d+\\.?\\d*?)%$");
    private static final String TAG = "TtmlDecoder";
    private static final String TTP = "http://www.w3.org/ns/ttml#parameter";
    private final XmlPullParserFactory xmlParserFactory;

    private static final class CellResolution {
        final int columns;
        final int rows;

        CellResolution(int columns2, int rows2) {
            this.columns = columns2;
            this.rows = rows2;
        }
    }

    private static final class FrameAndTickRate {
        final float effectiveFrameRate;
        final int subFrameRate;
        final int tickRate;

        FrameAndTickRate(float effectiveFrameRate2, int subFrameRate2, int tickRate2) {
            this.effectiveFrameRate = effectiveFrameRate2;
            this.subFrameRate = subFrameRate2;
            this.tickRate = tickRate2;
        }
    }

    public TtmlDecoder() {
        super(TAG);
        try {
            this.xmlParserFactory = XmlPullParserFactory.newInstance();
            this.xmlParserFactory.setNamespaceAware(true);
        } catch (XmlPullParserException e) {
            throw new RuntimeException("Couldn't create XmlPullParserFactory instance", e);
        }
    }

    /* access modifiers changed from: protected */
    public TtmlSubtitle decode(byte[] bytes, int length, boolean reset) throws SubtitleDecoderException {
        ByteArrayInputStream inputStream;
        try {
            XmlPullParser xmlParser = this.xmlParserFactory.newPullParser();
            Map<String, TtmlStyle> globalStyles = new HashMap<>();
            Map<String, TtmlRegion> regionMap = new HashMap<>();
            regionMap.put("", new TtmlRegion(null));
            try {
                ByteArrayInputStream inputStream2 = new ByteArrayInputStream(bytes, 0, length);
                xmlParser.setInput(inputStream2, null);
                ArrayDeque<TtmlNode> nodeStack = new ArrayDeque<>();
                int eventType = xmlParser.getEventType();
                FrameAndTickRate frameAndTickRate = DEFAULT_FRAME_AND_TICK_RATE;
                CellResolution cellResolution = DEFAULT_CELL_RESOLUTION;
                int unsupportedNodeDepth = 0;
                TtmlSubtitle ttmlSubtitle = null;
                while (eventType != 1) {
                    TtmlNode parent = (TtmlNode) nodeStack.peek();
                    if (unsupportedNodeDepth == 0) {
                        String name = xmlParser.getName();
                        if (eventType == 2) {
                            String name2 = name;
                            if (TtmlNode.TAG_TT.equals(name2)) {
                                frameAndTickRate = parseFrameAndTickRates(xmlParser);
                                cellResolution = parseCellResolution(xmlParser, DEFAULT_CELL_RESOLUTION);
                            }
                            if (!isSupportedTag(name2)) {
                                String str = TAG;
                                inputStream = inputStream2;
                                StringBuilder sb = new StringBuilder();
                                sb.append("Ignoring unsupported tag: ");
                                sb.append(xmlParser.getName());
                                Log.i(str, sb.toString());
                                unsupportedNodeDepth++;
                            } else {
                                inputStream = inputStream2;
                                if (TtmlNode.TAG_HEAD.equals(name2)) {
                                    parseHeader(xmlParser, globalStyles, regionMap, cellResolution);
                                } else {
                                    try {
                                        TtmlNode node = parseNode(xmlParser, parent, regionMap, frameAndTickRate);
                                        nodeStack.push(node);
                                        if (parent != null) {
                                            parent.addChild(node);
                                        }
                                    } catch (SubtitleDecoderException e) {
                                        Log.w(TAG, "Suppressing parser error", e);
                                        unsupportedNodeDepth++;
                                    }
                                }
                            }
                        } else {
                            inputStream = inputStream2;
                            String str2 = name;
                            if (eventType == 4) {
                                parent.addChild(TtmlNode.buildTextNode(xmlParser.getText()));
                            } else if (eventType == 3) {
                                if (xmlParser.getName().equals(TtmlNode.TAG_TT)) {
                                    ttmlSubtitle = new TtmlSubtitle((TtmlNode) nodeStack.peek(), globalStyles, regionMap);
                                }
                                nodeStack.pop();
                            }
                        }
                    } else {
                        inputStream = inputStream2;
                        if (eventType == 2) {
                            unsupportedNodeDepth++;
                        } else if (eventType == 3) {
                            unsupportedNodeDepth--;
                        }
                    }
                    xmlParser.next();
                    eventType = xmlParser.getEventType();
                    inputStream2 = inputStream;
                    byte[] bArr = bytes;
                }
                return ttmlSubtitle;
            } catch (XmlPullParserException e2) {
                xppe = e2;
                throw new SubtitleDecoderException("Unable to decode source", xppe);
            } catch (IOException e3) {
                e = e3;
                throw new IllegalStateException("Unexpected error when reading input.", e);
            }
        } catch (XmlPullParserException e4) {
            xppe = e4;
            int i = length;
            throw new SubtitleDecoderException("Unable to decode source", xppe);
        } catch (IOException e5) {
            e = e5;
            int i2 = length;
            throw new IllegalStateException("Unexpected error when reading input.", e);
        }
    }

    private FrameAndTickRate parseFrameAndTickRates(XmlPullParser xmlParser) throws SubtitleDecoderException {
        int frameRate = 30;
        String frameRateString = xmlParser.getAttributeValue(TTP, "frameRate");
        if (frameRateString != null) {
            frameRate = Integer.parseInt(frameRateString);
        }
        float frameRateMultiplier = 1.0f;
        String frameRateMultiplierString = xmlParser.getAttributeValue(TTP, "frameRateMultiplier");
        if (frameRateMultiplierString != null) {
            String[] parts = Util.split(frameRateMultiplierString, StringUtils.SPACE);
            if (parts.length == 2) {
                frameRateMultiplier = ((float) Integer.parseInt(parts[0])) / ((float) Integer.parseInt(parts[1]));
            } else {
                throw new SubtitleDecoderException("frameRateMultiplier doesn't have 2 parts");
            }
        }
        int subFrameRate = DEFAULT_FRAME_AND_TICK_RATE.subFrameRate;
        String subFrameRateString = xmlParser.getAttributeValue(TTP, "subFrameRate");
        if (subFrameRateString != null) {
            subFrameRate = Integer.parseInt(subFrameRateString);
        }
        int tickRate = DEFAULT_FRAME_AND_TICK_RATE.tickRate;
        String tickRateString = xmlParser.getAttributeValue(TTP, "tickRate");
        if (tickRateString != null) {
            tickRate = Integer.parseInt(tickRateString);
        }
        return new FrameAndTickRate(((float) frameRate) * frameRateMultiplier, subFrameRate, tickRate);
    }

    private CellResolution parseCellResolution(XmlPullParser xmlParser, CellResolution defaultValue) throws SubtitleDecoderException {
        String cellResolution = xmlParser.getAttributeValue(TTP, "cellResolution");
        if (cellResolution == null) {
            return defaultValue;
        }
        Matcher cellResolutionMatcher = CELL_RESOLUTION.matcher(cellResolution);
        if (!cellResolutionMatcher.matches()) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("Ignoring malformed cell resolution: ");
            sb.append(cellResolution);
            Log.w(str, sb.toString());
            return defaultValue;
        }
        try {
            int columns = Integer.parseInt(cellResolutionMatcher.group(1));
            int rows = Integer.parseInt(cellResolutionMatcher.group(2));
            if (columns != 0 && rows != 0) {
                return new CellResolution(columns, rows);
            }
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Invalid cell resolution ");
            sb2.append(columns);
            sb2.append(StringUtils.SPACE);
            sb2.append(rows);
            throw new SubtitleDecoderException(sb2.toString());
        } catch (NumberFormatException e) {
            String str2 = TAG;
            StringBuilder sb3 = new StringBuilder();
            sb3.append("Ignoring malformed cell resolution: ");
            sb3.append(cellResolution);
            Log.w(str2, sb3.toString());
            return defaultValue;
        }
    }

    private Map<String, TtmlStyle> parseHeader(XmlPullParser xmlParser, Map<String, TtmlStyle> globalStyles, Map<String, TtmlRegion> globalRegions, CellResolution cellResolution) throws IOException, XmlPullParserException {
        do {
            xmlParser.next();
            if (XmlPullParserUtil.isStartTag(xmlParser, "style")) {
                String parentStyleId = XmlPullParserUtil.getAttributeValue(xmlParser, "style");
                TtmlStyle style = parseStyleAttributes(xmlParser, new TtmlStyle());
                if (parentStyleId != null) {
                    for (String id : parseStyleIds(parentStyleId)) {
                        style.chain((TtmlStyle) globalStyles.get(id));
                    }
                }
                if (style.getId() != null) {
                    globalStyles.put(style.getId(), style);
                }
            } else if (XmlPullParserUtil.isStartTag(xmlParser, "region")) {
                TtmlRegion ttmlRegion = parseRegionAttributes(xmlParser, cellResolution);
                if (ttmlRegion != null) {
                    globalRegions.put(ttmlRegion.id, ttmlRegion);
                }
            }
        } while (!XmlPullParserUtil.isEndTag(xmlParser, TtmlNode.TAG_HEAD));
        return globalStyles;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0086, code lost:
        if (r3.equals("after") == false) goto L_0x0093;
     */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x0098  */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x009f  */
    private TtmlRegion parseRegionAttributes(XmlPullParser xmlParser, CellResolution cellResolution) {
        int lineAnchor;
        float line;
        XmlPullParser xmlPullParser = xmlParser;
        String regionId = XmlPullParserUtil.getAttributeValue(xmlPullParser, TtmlNode.ATTR_ID);
        if (regionId == null) {
            return null;
        }
        String regionOrigin = XmlPullParserUtil.getAttributeValue(xmlPullParser, TtmlNode.ATTR_TTS_ORIGIN);
        if (regionOrigin != null) {
            Matcher originMatcher = PERCENTAGE_COORDINATES.matcher(regionOrigin);
            if (originMatcher.matches()) {
                char c = 1;
                try {
                    float position = Float.parseFloat(originMatcher.group(1)) / 100.0f;
                    float line2 = Float.parseFloat(originMatcher.group(2)) / 100.0f;
                    String regionExtent = XmlPullParserUtil.getAttributeValue(xmlPullParser, TtmlNode.ATTR_TTS_EXTENT);
                    if (regionExtent != null) {
                        Matcher extentMatcher = PERCENTAGE_COORDINATES.matcher(regionExtent);
                        if (extentMatcher.matches()) {
                            try {
                                float width = Float.parseFloat(extentMatcher.group(1)) / 100.0f;
                                float height = Float.parseFloat(extentMatcher.group(2)) / 100.0f;
                                String displayAlign = XmlPullParserUtil.getAttributeValue(xmlPullParser, TtmlNode.ATTR_TTS_DISPLAY_ALIGN);
                                if (displayAlign != null) {
                                    String lowerInvariant = Util.toLowerInvariant(displayAlign);
                                    int hashCode = lowerInvariant.hashCode();
                                    if (hashCode == -1364013995) {
                                        if (lowerInvariant.equals(TtmlNode.CENTER)) {
                                            c = 0;
                                            switch (c) {
                                                case 0:
                                                    lineAnchor = 1;
                                                    line = line2 + (height / 2.0f);
                                                    break;
                                                case 1:
                                                    lineAnchor = 2;
                                                    line = line2 + height;
                                                    break;
                                            }
                                        }
                                    } else if (hashCode == 92734940) {
                                    }
                                    c = 65535;
                                    switch (c) {
                                        case 0:
                                            break;
                                        case 1:
                                            break;
                                    }
                                }
                                lineAnchor = 0;
                                line = line2;
                                TtmlRegion ttmlRegion = new TtmlRegion(regionId, position, line, 0, lineAnchor, width, 1, 1.0f / ((float) cellResolution.rows));
                                return ttmlRegion;
                            } catch (NumberFormatException e) {
                                String str = TAG;
                                StringBuilder sb = new StringBuilder();
                                sb.append("Ignoring region with malformed extent: ");
                                sb.append(regionOrigin);
                                Log.w(str, sb.toString());
                                return null;
                            }
                        } else {
                            String str2 = TAG;
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append("Ignoring region with unsupported extent: ");
                            sb2.append(regionOrigin);
                            Log.w(str2, sb2.toString());
                            return null;
                        }
                    } else {
                        Log.w(TAG, "Ignoring region without an extent");
                        return null;
                    }
                } catch (NumberFormatException e2) {
                    String str3 = TAG;
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append("Ignoring region with malformed origin: ");
                    sb3.append(regionOrigin);
                    Log.w(str3, sb3.toString());
                    return null;
                }
            } else {
                String str4 = TAG;
                StringBuilder sb4 = new StringBuilder();
                sb4.append("Ignoring region with unsupported origin: ");
                sb4.append(regionOrigin);
                Log.w(str4, sb4.toString());
                return null;
            }
        } else {
            Log.w(TAG, "Ignoring region without an origin");
            return null;
        }
    }

    private String[] parseStyleIds(String parentStyleIds) {
        String parentStyleIds2 = parentStyleIds.trim();
        return parentStyleIds2.isEmpty() ? new String[0] : Util.split(parentStyleIds2, "\\s+");
    }

    /* JADX WARNING: Code restructure failed: missing block: B:48:0x00b5, code lost:
        if (r3.equals(com.google.android.exoplayer2.text.ttml.TtmlNode.UNDERLINE) != false) goto L_0x00c3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:73:0x0127, code lost:
        if (r3.equals(com.google.android.exoplayer2.text.ttml.TtmlNode.CENTER) != false) goto L_0x012b;
     */
    /* JADX WARNING: Removed duplicated region for block: B:117:0x020f A[SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x00c7  */
    /* JADX WARNING: Removed duplicated region for block: B:55:0x00d0  */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x00d9  */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x00e2  */
    private TtmlStyle parseStyleAttributes(XmlPullParser parser, TtmlStyle style) {
        char c;
        int attributeCount = parser.getAttributeCount();
        for (int i = 0; i < attributeCount; i++) {
            String attributeValue = parser.getAttributeValue(i);
            String attributeName = parser.getAttributeName(i);
            char c2 = 4;
            char c3 = 2;
            switch (attributeName.hashCode()) {
                case -1550943582:
                    if (attributeName.equals(TtmlNode.ATTR_TTS_FONT_STYLE)) {
                        c = 6;
                        break;
                    }
                case -1224696685:
                    if (attributeName.equals(TtmlNode.ATTR_TTS_FONT_FAMILY)) {
                        c = 3;
                        break;
                    }
                case -1065511464:
                    if (attributeName.equals(TtmlNode.ATTR_TTS_TEXT_ALIGN)) {
                        c = 7;
                        break;
                    }
                case -879295043:
                    if (attributeName.equals(TtmlNode.ATTR_TTS_TEXT_DECORATION)) {
                        c = 8;
                        break;
                    }
                case -734428249:
                    if (attributeName.equals(TtmlNode.ATTR_TTS_FONT_WEIGHT)) {
                        c = 5;
                        break;
                    }
                case 3355:
                    if (attributeName.equals(TtmlNode.ATTR_ID)) {
                        c = 0;
                        break;
                    }
                case 94842723:
                    if (attributeName.equals(TtmlNode.ATTR_TTS_COLOR)) {
                        c = 2;
                        break;
                    }
                case 365601008:
                    if (attributeName.equals(TtmlNode.ATTR_TTS_FONT_SIZE)) {
                        c = 4;
                        break;
                    }
                case 1287124693:
                    if (attributeName.equals(TtmlNode.ATTR_TTS_BACKGROUND_COLOR)) {
                        c = 1;
                        break;
                    }
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    if (!"style".equals(parser.getName())) {
                        break;
                    } else {
                        style = createIfNull(style).setId(attributeValue);
                        break;
                    }
                case 1:
                    style = createIfNull(style);
                    try {
                        style.setBackgroundColor(ColorParser.parseTtmlColor(attributeValue));
                        break;
                    } catch (IllegalArgumentException e) {
                        String str = TAG;
                        StringBuilder sb = new StringBuilder();
                        sb.append("Failed parsing background value: ");
                        sb.append(attributeValue);
                        Log.w(str, sb.toString());
                        break;
                    }
                case 2:
                    style = createIfNull(style);
                    try {
                        style.setFontColor(ColorParser.parseTtmlColor(attributeValue));
                        break;
                    } catch (IllegalArgumentException e2) {
                        String str2 = TAG;
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("Failed parsing color value: ");
                        sb2.append(attributeValue);
                        Log.w(str2, sb2.toString());
                        break;
                    }
                case 3:
                    style = createIfNull(style).setFontFamily(attributeValue);
                    break;
                case 4:
                    try {
                        style = createIfNull(style);
                        parseFontSize(attributeValue, style);
                        break;
                    } catch (SubtitleDecoderException e3) {
                        String str3 = TAG;
                        StringBuilder sb3 = new StringBuilder();
                        sb3.append("Failed parsing fontSize value: ");
                        sb3.append(attributeValue);
                        Log.w(str3, sb3.toString());
                        break;
                    }
                case 5:
                    style = createIfNull(style).setBold(TtmlNode.BOLD.equalsIgnoreCase(attributeValue));
                    break;
                case 6:
                    style = createIfNull(style).setItalic(TtmlNode.ITALIC.equalsIgnoreCase(attributeValue));
                    break;
                case 7:
                    String lowerInvariant = Util.toLowerInvariant(attributeValue);
                    switch (lowerInvariant.hashCode()) {
                        case -1364013995:
                            break;
                        case 100571:
                            if (lowerInvariant.equals("end")) {
                                c2 = 3;
                                break;
                            }
                        case 3317767:
                            if (lowerInvariant.equals(TtmlNode.LEFT)) {
                                c2 = 0;
                                break;
                            }
                        case 108511772:
                            if (lowerInvariant.equals(TtmlNode.RIGHT)) {
                                c2 = 2;
                                break;
                            }
                        case 109757538:
                            if (lowerInvariant.equals(TtmlNode.START)) {
                                c2 = 1;
                                break;
                            }
                        default:
                            c2 = 65535;
                            break;
                    }
                    switch (c2) {
                        case 0:
                            style = createIfNull(style).setTextAlign(Alignment.ALIGN_NORMAL);
                            break;
                        case 1:
                            style = createIfNull(style).setTextAlign(Alignment.ALIGN_NORMAL);
                            break;
                        case 2:
                            style = createIfNull(style).setTextAlign(Alignment.ALIGN_OPPOSITE);
                            break;
                        case 3:
                            style = createIfNull(style).setTextAlign(Alignment.ALIGN_OPPOSITE);
                            break;
                        case 4:
                            style = createIfNull(style).setTextAlign(Alignment.ALIGN_CENTER);
                            break;
                    }
                case 8:
                    String lowerInvariant2 = Util.toLowerInvariant(attributeValue);
                    int hashCode = lowerInvariant2.hashCode();
                    if (hashCode == -1461280213) {
                        if (lowerInvariant2.equals(TtmlNode.NO_UNDERLINE)) {
                            c3 = 3;
                            switch (c3) {
                                case 0:
                                    break;
                                case 1:
                                    break;
                                case 2:
                                    break;
                                case 3:
                                    break;
                            }
                        }
                    } else if (hashCode != -1026963764) {
                        if (hashCode == 913457136) {
                            if (lowerInvariant2.equals(TtmlNode.NO_LINETHROUGH)) {
                                c3 = 1;
                                switch (c3) {
                                    case 0:
                                        break;
                                    case 1:
                                        break;
                                    case 2:
                                        break;
                                    case 3:
                                        break;
                                }
                            }
                        } else if (hashCode == 1679736913 && lowerInvariant2.equals(TtmlNode.LINETHROUGH)) {
                            c3 = 0;
                            switch (c3) {
                                case 0:
                                    style = createIfNull(style).setLinethrough(true);
                                    break;
                                case 1:
                                    style = createIfNull(style).setLinethrough(false);
                                    break;
                                case 2:
                                    style = createIfNull(style).setUnderline(true);
                                    break;
                                case 3:
                                    style = createIfNull(style).setUnderline(false);
                                    break;
                            }
                        }
                    } else {
                        break;
                    }
                    c3 = 65535;
                    switch (c3) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            break;
                        case 3:
                            break;
                    }
            }
        }
        return style;
    }

    private TtmlStyle createIfNull(TtmlStyle style) {
        return style == null ? new TtmlStyle() : style;
    }

    private TtmlNode parseNode(XmlPullParser parser, TtmlNode parent, Map<String, TtmlRegion> regionMap, FrameAndTickRate frameAndTickRate) throws SubtitleDecoderException {
        long endTime;
        TtmlDecoder ttmlDecoder = this;
        XmlPullParser xmlPullParser = parser;
        TtmlNode ttmlNode = parent;
        FrameAndTickRate frameAndTickRate2 = frameAndTickRate;
        long duration = C.TIME_UNSET;
        long startTime = C.TIME_UNSET;
        long endTime2 = C.TIME_UNSET;
        String regionId = "";
        String[] styleIds = null;
        int attributeCount = parser.getAttributeCount();
        TtmlStyle style = ttmlDecoder.parseStyleAttributes(xmlPullParser, null);
        int i = 0;
        while (i < attributeCount) {
            String attr = xmlPullParser.getAttributeName(i);
            int attributeCount2 = attributeCount;
            String value = xmlPullParser.getAttributeValue(i);
            char c = 65535;
            switch (attr.hashCode()) {
                case -934795532:
                    if (attr.equals("region")) {
                        c = 4;
                        break;
                    }
                    break;
                case 99841:
                    if (attr.equals(ATTR_DURATION)) {
                        c = 2;
                        break;
                    }
                    break;
                case 100571:
                    if (attr.equals("end")) {
                        c = 1;
                        break;
                    }
                    break;
                case 93616297:
                    if (attr.equals(ATTR_BEGIN)) {
                        c = 0;
                        break;
                    }
                    break;
                case 109780401:
                    if (attr.equals("style")) {
                        c = 3;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    startTime = parseTimeExpression(value, frameAndTickRate2);
                    break;
                case 1:
                    endTime2 = parseTimeExpression(value, frameAndTickRate2);
                    break;
                case 2:
                    duration = parseTimeExpression(value, frameAndTickRate2);
                    break;
                case 3:
                    Map<String, TtmlRegion> map = regionMap;
                    String[] ids = ttmlDecoder.parseStyleIds(value);
                    if (ids.length <= 0) {
                        break;
                    } else {
                        styleIds = ids;
                        break;
                    }
                case 4:
                    if (!regionMap.containsKey(value)) {
                        break;
                    } else {
                        regionId = value;
                        break;
                    }
            }
            i++;
            attributeCount = attributeCount2;
            ttmlDecoder = this;
            xmlPullParser = parser;
        }
        if (!(ttmlNode == null || ttmlNode.startTimeUs == C.TIME_UNSET)) {
            if (startTime != C.TIME_UNSET) {
                startTime += ttmlNode.startTimeUs;
            }
            if (endTime2 != C.TIME_UNSET) {
                endTime2 += ttmlNode.startTimeUs;
            }
        }
        if (endTime2 == C.TIME_UNSET) {
            if (duration != C.TIME_UNSET) {
                endTime = startTime + duration;
            } else if (!(ttmlNode == null || ttmlNode.endTimeUs == C.TIME_UNSET)) {
                endTime = ttmlNode.endTimeUs;
            }
            return TtmlNode.buildNode(parser.getName(), startTime, endTime, style, styleIds, regionId);
        }
        endTime = endTime2;
        return TtmlNode.buildNode(parser.getName(), startTime, endTime, style, styleIds, regionId);
    }

    private static boolean isSupportedTag(String tag) {
        return tag.equals(TtmlNode.TAG_TT) || tag.equals(TtmlNode.TAG_HEAD) || tag.equals(TtmlNode.TAG_BODY) || tag.equals(TtmlNode.TAG_DIV) || tag.equals(TtmlNode.TAG_P) || tag.equals(TtmlNode.TAG_SPAN) || tag.equals(TtmlNode.TAG_BR) || tag.equals("style") || tag.equals(TtmlNode.TAG_STYLING) || tag.equals(TtmlNode.TAG_LAYOUT) || tag.equals("region") || tag.equals(TtmlNode.TAG_METADATA) || tag.equals(TtmlNode.TAG_SMPTE_IMAGE) || tag.equals(TtmlNode.TAG_SMPTE_DATA) || tag.equals(TtmlNode.TAG_SMPTE_INFORMATION);
    }

    private static void parseFontSize(String expression, TtmlStyle out) throws SubtitleDecoderException {
        Matcher matcher;
        String[] expressions = Util.split(expression, "\\s+");
        if (expressions.length == 1) {
            matcher = FONT_SIZE.matcher(expression);
        } else if (expressions.length == 2) {
            matcher = FONT_SIZE.matcher(expressions[1]);
            Log.w(TAG, "Multiple values in fontSize attribute. Picking the second value for vertical font size and ignoring the first.");
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Invalid number of entries for fontSize: ");
            sb.append(expressions.length);
            sb.append(".");
            throw new SubtitleDecoderException(sb.toString());
        }
        if (matcher.matches()) {
            String unit = matcher.group(3);
            char c = 65535;
            int hashCode = unit.hashCode();
            if (hashCode != 37) {
                if (hashCode != 3240) {
                    if (hashCode == 3592 && unit.equals("px")) {
                        c = 0;
                    }
                } else if (unit.equals("em")) {
                    c = 1;
                }
            } else if (unit.equals("%")) {
                c = 2;
            }
            switch (c) {
                case 0:
                    out.setFontSizeUnit(1);
                    break;
                case 1:
                    out.setFontSizeUnit(2);
                    break;
                case 2:
                    out.setFontSizeUnit(3);
                    break;
                default:
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("Invalid unit for fontSize: '");
                    sb2.append(unit);
                    sb2.append("'.");
                    throw new SubtitleDecoderException(sb2.toString());
            }
            out.setFontSize(Float.valueOf(matcher.group(1)).floatValue());
            return;
        }
        StringBuilder sb3 = new StringBuilder();
        sb3.append("Invalid expression for fontSize: '");
        sb3.append(expression);
        sb3.append("'.");
        throw new SubtitleDecoderException(sb3.toString());
    }

    /* JADX WARNING: Code restructure failed: missing block: B:27:0x00c4, code lost:
        if (r11.equals("t") != false) goto L_0x00fa;
     */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x00fe  */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x0106  */
    /* JADX WARNING: Removed duplicated region for block: B:47:0x010e  */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x0116  */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x011b  */
    private static long parseTimeExpression(String time, FrameAndTickRate frameAndTickRate) throws SubtitleDecoderException {
        String str = time;
        FrameAndTickRate frameAndTickRate2 = frameAndTickRate;
        Matcher matcher = CLOCK_TIME.matcher(str);
        char c = 5;
        if (matcher.matches()) {
            double durationSeconds = (double) (Long.parseLong(matcher.group(1)) * 3600);
            double parseLong = (double) (Long.parseLong(matcher.group(2)) * 60);
            Double.isNaN(durationSeconds);
            Double.isNaN(parseLong);
            double durationSeconds2 = durationSeconds + parseLong;
            double parseLong2 = (double) Long.parseLong(matcher.group(3));
            Double.isNaN(parseLong2);
            double durationSeconds3 = durationSeconds2 + parseLong2;
            String fraction = matcher.group(4);
            double d = 0.0d;
            double durationSeconds4 = durationSeconds3 + (fraction != null ? Double.parseDouble(fraction) : 0.0d);
            String frames = matcher.group(5);
            double durationSeconds5 = durationSeconds4 + (frames != null ? (double) (((float) Long.parseLong(frames)) / frameAndTickRate2.effectiveFrameRate) : 0.0d);
            String subframes = matcher.group(6);
            if (subframes != null) {
                double parseLong3 = (double) Long.parseLong(subframes);
                double d2 = (double) frameAndTickRate2.subFrameRate;
                Double.isNaN(parseLong3);
                Double.isNaN(d2);
                double d3 = parseLong3 / d2;
                double d4 = (double) frameAndTickRate2.effectiveFrameRate;
                Double.isNaN(d4);
                d = d3 / d4;
            }
            return (long) (1000000.0d * (durationSeconds5 + d));
        }
        Matcher matcher2 = OFFSET_TIME.matcher(str);
        if (matcher2.matches()) {
            double offsetSeconds = Double.parseDouble(matcher2.group(1));
            String unit = matcher2.group(2);
            int hashCode = unit.hashCode();
            if (hashCode != 102) {
                if (hashCode != 104) {
                    if (hashCode != 109) {
                        if (hashCode != 3494) {
                            switch (hashCode) {
                                case 115:
                                    if (unit.equals("s")) {
                                        c = 2;
                                        break;
                                    }
                                case 116:
                                    break;
                            }
                        } else if (unit.equals("ms")) {
                            c = 3;
                            switch (c) {
                                case 0:
                                    offsetSeconds *= 3600.0d;
                                    break;
                                case 1:
                                    offsetSeconds *= 60.0d;
                                    break;
                                case 3:
                                    offsetSeconds /= 1000.0d;
                                    break;
                                case 4:
                                    double d5 = (double) frameAndTickRate2.effectiveFrameRate;
                                    Double.isNaN(d5);
                                    offsetSeconds /= d5;
                                    break;
                                case 5:
                                    double d6 = (double) frameAndTickRate2.tickRate;
                                    Double.isNaN(d6);
                                    offsetSeconds /= d6;
                                    break;
                            }
                            return (long) (1000000.0d * offsetSeconds);
                        }
                    } else if (unit.equals("m")) {
                        c = 1;
                        switch (c) {
                            case 0:
                                break;
                            case 1:
                                break;
                            case 3:
                                break;
                            case 4:
                                break;
                            case 5:
                                break;
                        }
                        return (long) (1000000.0d * offsetSeconds);
                    }
                } else if (unit.equals("h")) {
                    c = 0;
                    switch (c) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 3:
                            break;
                        case 4:
                            break;
                        case 5:
                            break;
                    }
                    return (long) (1000000.0d * offsetSeconds);
                }
            } else if (unit.equals("f")) {
                c = 4;
                switch (c) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 3:
                        break;
                    case 4:
                        break;
                    case 5:
                        break;
                }
                return (long) (1000000.0d * offsetSeconds);
            }
            c = 65535;
            switch (c) {
                case 0:
                    break;
                case 1:
                    break;
                case 3:
                    break;
                case 4:
                    break;
                case 5:
                    break;
            }
            return (long) (1000000.0d * offsetSeconds);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Malformed time expression: ");
        sb.append(str);
        throw new SubtitleDecoderException(sb.toString());
    }
}
