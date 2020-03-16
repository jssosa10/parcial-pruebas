package com.google.android.exoplayer2.extractor.mp4;

import android.support.annotation.Nullable;
import com.google.android.exoplayer2.metadata.Metadata.Entry;
import com.google.android.exoplayer2.metadata.id3.ApicFrame;
import com.google.android.exoplayer2.metadata.id3.CommentFrame;
import com.google.android.exoplayer2.metadata.id3.Id3Frame;
import com.google.android.exoplayer2.metadata.id3.InternalFrame;
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.ParsableByteArray;
import com.google.android.exoplayer2.util.Util;

final class MetadataUtil {
    private static final String LANGUAGE_UNDEFINED = "und";
    private static final int PICTURE_TYPE_FRONT_COVER = 3;
    private static final int SHORT_TYPE_ALBUM = Util.getIntegerCodeForString("alb");
    private static final int SHORT_TYPE_ARTIST = Util.getIntegerCodeForString("ART");
    private static final int SHORT_TYPE_COMMENT = Util.getIntegerCodeForString("cmt");
    private static final int SHORT_TYPE_COMPOSER_1 = Util.getIntegerCodeForString("com");
    private static final int SHORT_TYPE_COMPOSER_2 = Util.getIntegerCodeForString("wrt");
    private static final int SHORT_TYPE_ENCODER = Util.getIntegerCodeForString("too");
    private static final int SHORT_TYPE_GENRE = Util.getIntegerCodeForString("gen");
    private static final int SHORT_TYPE_LYRICS = Util.getIntegerCodeForString("lyr");
    private static final int SHORT_TYPE_NAME_1 = Util.getIntegerCodeForString("nam");
    private static final int SHORT_TYPE_NAME_2 = Util.getIntegerCodeForString("trk");
    private static final int SHORT_TYPE_YEAR = Util.getIntegerCodeForString("day");
    private static final String[] STANDARD_GENRES = {"Blues", "Classic Rock", "Country", "Dance", "Disco", "Funk", "Grunge", "Hip-Hop", "Jazz", "Metal", "New Age", "Oldies", "Other", "Pop", "R&B", "Rap", "Reggae", "Rock", "Techno", "Industrial", "Alternative", "Ska", "Death Metal", "Pranks", "Soundtrack", "Euro-Techno", "Ambient", "Trip-Hop", "Vocal", "Jazz+Funk", "Fusion", "Trance", "Classical", "Instrumental", "Acid", "House", "Game", "Sound Clip", "Gospel", "Noise", "AlternRock", "Bass", "Soul", "Punk", "Space", "Meditative", "Instrumental Pop", "Instrumental Rock", "Ethnic", "Gothic", "Darkwave", "Techno-Industrial", "Electronic", "Pop-Folk", "Eurodance", "Dream", "Southern Rock", "Comedy", "Cult", "Gangsta", "Top 40", "Christian Rap", "Pop/Funk", "Jungle", "Native American", "Cabaret", "New Wave", "Psychadelic", "Rave", "Showtunes", "Trailer", "Lo-Fi", "Tribal", "Acid Punk", "Acid Jazz", "Polka", "Retro", "Musical", "Rock & Roll", "Hard Rock", "Folk", "Folk-Rock", "National Folk", "Swing", "Fast Fusion", "Bebob", "Latin", "Revival", "Celtic", "Bluegrass", "Avantgarde", "Gothic Rock", "Progressive Rock", "Psychedelic Rock", "Symphonic Rock", "Slow Rock", "Big Band", "Chorus", "Easy Listening", "Acoustic", "Humour", "Speech", "Chanson", "Opera", "Chamber Music", "Sonata", "Symphony", "Booty Bass", "Primus", "Porn Groove", "Satire", "Slow Jam", "Club", "Tango", "Samba", "Folklore", "Ballad", "Power Ballad", "Rhythmic Soul", "Freestyle", "Duet", "Punk Rock", "Drum Solo", "A capella", "Euro-House", "Dance Hall", "Goa", "Drum & Bass", "Club-House", "Hardcore", "Terror", "Indie", "BritPop", "Negerpunk", "Polsk Punk", "Beat", "Christian Gangsta Rap", "Heavy Metal", "Black Metal", "Crossover", "Contemporary Christian", "Christian Rock", "Merengue", "Salsa", "Thrash Metal", "Anime", "Jpop", "Synthpop"};
    private static final String TAG = "MetadataUtil";
    private static final int TYPE_ALBUM_ARTIST = Util.getIntegerCodeForString("aART");
    private static final int TYPE_COMPILATION = Util.getIntegerCodeForString("cpil");
    private static final int TYPE_COVER_ART = Util.getIntegerCodeForString("covr");
    private static final int TYPE_DISK_NUMBER = Util.getIntegerCodeForString("disk");
    private static final int TYPE_GAPLESS_ALBUM = Util.getIntegerCodeForString("pgap");
    private static final int TYPE_GENRE = Util.getIntegerCodeForString("gnre");
    private static final int TYPE_GROUPING = Util.getIntegerCodeForString("grp");
    private static final int TYPE_INTERNAL = Util.getIntegerCodeForString(InternalFrame.ID);
    private static final int TYPE_RATING = Util.getIntegerCodeForString("rtng");
    private static final int TYPE_SORT_ALBUM = Util.getIntegerCodeForString("soal");
    private static final int TYPE_SORT_ALBUM_ARTIST = Util.getIntegerCodeForString("soaa");
    private static final int TYPE_SORT_ARTIST = Util.getIntegerCodeForString("soar");
    private static final int TYPE_SORT_COMPOSER = Util.getIntegerCodeForString("soco");
    private static final int TYPE_SORT_TRACK_NAME = Util.getIntegerCodeForString("sonm");
    private static final int TYPE_TEMPO = Util.getIntegerCodeForString("tmpo");
    private static final int TYPE_TRACK_NUMBER = Util.getIntegerCodeForString("trkn");
    private static final int TYPE_TV_SHOW = Util.getIntegerCodeForString("tvsh");
    private static final int TYPE_TV_SORT_SHOW = Util.getIntegerCodeForString("sosn");

    private MetadataUtil() {
    }

    @Nullable
    public static Entry parseIlstElement(ParsableByteArray ilst) {
        int endPosition = ilst.readInt() + ilst.getPosition();
        int type = ilst.readInt();
        int typeTopByte = (type >> 24) & 255;
        if (typeTopByte == 169 || typeTopByte == 65533) {
            int shortType = 16777215 & type;
            if (shortType == SHORT_TYPE_COMMENT) {
                CommentFrame parseCommentAttribute = parseCommentAttribute(type, ilst);
                ilst.setPosition(endPosition);
                return parseCommentAttribute;
            }
            if (shortType != SHORT_TYPE_NAME_1) {
                if (shortType != SHORT_TYPE_NAME_2) {
                    if (shortType != SHORT_TYPE_COMPOSER_1) {
                        if (shortType != SHORT_TYPE_COMPOSER_2) {
                            if (shortType == SHORT_TYPE_YEAR) {
                                TextInformationFrame parseTextAttribute = parseTextAttribute(type, "TDRC", ilst);
                                ilst.setPosition(endPosition);
                                return parseTextAttribute;
                            } else if (shortType == SHORT_TYPE_ARTIST) {
                                TextInformationFrame parseTextAttribute2 = parseTextAttribute(type, "TPE1", ilst);
                                ilst.setPosition(endPosition);
                                return parseTextAttribute2;
                            } else if (shortType == SHORT_TYPE_ENCODER) {
                                TextInformationFrame parseTextAttribute3 = parseTextAttribute(type, "TSSE", ilst);
                                ilst.setPosition(endPosition);
                                return parseTextAttribute3;
                            } else if (shortType == SHORT_TYPE_ALBUM) {
                                TextInformationFrame parseTextAttribute4 = parseTextAttribute(type, "TALB", ilst);
                                ilst.setPosition(endPosition);
                                return parseTextAttribute4;
                            } else if (shortType == SHORT_TYPE_LYRICS) {
                                TextInformationFrame parseTextAttribute5 = parseTextAttribute(type, "USLT", ilst);
                                ilst.setPosition(endPosition);
                                return parseTextAttribute5;
                            } else if (shortType == SHORT_TYPE_GENRE) {
                                TextInformationFrame parseTextAttribute6 = parseTextAttribute(type, "TCON", ilst);
                                ilst.setPosition(endPosition);
                                return parseTextAttribute6;
                            } else if (shortType == TYPE_GROUPING) {
                                TextInformationFrame parseTextAttribute7 = parseTextAttribute(type, "TIT1", ilst);
                                ilst.setPosition(endPosition);
                                return parseTextAttribute7;
                            }
                        }
                    }
                    TextInformationFrame parseTextAttribute8 = parseTextAttribute(type, "TCOM", ilst);
                    ilst.setPosition(endPosition);
                    return parseTextAttribute8;
                }
            }
            TextInformationFrame parseTextAttribute9 = parseTextAttribute(type, "TIT2", ilst);
            ilst.setPosition(endPosition);
            return parseTextAttribute9;
        }
        try {
            if (type == TYPE_GENRE) {
                return parseStandardGenreAttribute(ilst);
            }
            if (type == TYPE_DISK_NUMBER) {
                TextInformationFrame parseIndexAndCountAttribute = parseIndexAndCountAttribute(type, "TPOS", ilst);
                ilst.setPosition(endPosition);
                return parseIndexAndCountAttribute;
            } else if (type == TYPE_TRACK_NUMBER) {
                TextInformationFrame parseIndexAndCountAttribute2 = parseIndexAndCountAttribute(type, "TRCK", ilst);
                ilst.setPosition(endPosition);
                return parseIndexAndCountAttribute2;
            } else if (type == TYPE_TEMPO) {
                Id3Frame parseUint8Attribute = parseUint8Attribute(type, "TBPM", ilst, true, false);
                ilst.setPosition(endPosition);
                return parseUint8Attribute;
            } else if (type == TYPE_COMPILATION) {
                Id3Frame parseUint8Attribute2 = parseUint8Attribute(type, "TCMP", ilst, true, true);
                ilst.setPosition(endPosition);
                return parseUint8Attribute2;
            } else if (type == TYPE_COVER_ART) {
                ApicFrame parseCoverArt = parseCoverArt(ilst);
                ilst.setPosition(endPosition);
                return parseCoverArt;
            } else if (type == TYPE_ALBUM_ARTIST) {
                TextInformationFrame parseTextAttribute10 = parseTextAttribute(type, "TPE2", ilst);
                ilst.setPosition(endPosition);
                return parseTextAttribute10;
            } else if (type == TYPE_SORT_TRACK_NAME) {
                TextInformationFrame parseTextAttribute11 = parseTextAttribute(type, "TSOT", ilst);
                ilst.setPosition(endPosition);
                return parseTextAttribute11;
            } else if (type == TYPE_SORT_ALBUM) {
                TextInformationFrame parseTextAttribute12 = parseTextAttribute(type, "TSO2", ilst);
                ilst.setPosition(endPosition);
                return parseTextAttribute12;
            } else if (type == TYPE_SORT_ARTIST) {
                TextInformationFrame parseTextAttribute13 = parseTextAttribute(type, "TSOA", ilst);
                ilst.setPosition(endPosition);
                return parseTextAttribute13;
            } else if (type == TYPE_SORT_ALBUM_ARTIST) {
                TextInformationFrame parseTextAttribute14 = parseTextAttribute(type, "TSOP", ilst);
                ilst.setPosition(endPosition);
                return parseTextAttribute14;
            } else if (type == TYPE_SORT_COMPOSER) {
                TextInformationFrame parseTextAttribute15 = parseTextAttribute(type, "TSOC", ilst);
                ilst.setPosition(endPosition);
                return parseTextAttribute15;
            } else if (type == TYPE_RATING) {
                Id3Frame parseUint8Attribute3 = parseUint8Attribute(type, "ITUNESADVISORY", ilst, false, false);
                ilst.setPosition(endPosition);
                return parseUint8Attribute3;
            } else if (type == TYPE_GAPLESS_ALBUM) {
                Id3Frame parseUint8Attribute4 = parseUint8Attribute(type, "ITUNESGAPLESS", ilst, false, true);
                ilst.setPosition(endPosition);
                return parseUint8Attribute4;
            } else if (type == TYPE_TV_SORT_SHOW) {
                TextInformationFrame parseTextAttribute16 = parseTextAttribute(type, "TVSHOWSORT", ilst);
                ilst.setPosition(endPosition);
                return parseTextAttribute16;
            } else if (type == TYPE_TV_SHOW) {
                TextInformationFrame parseTextAttribute17 = parseTextAttribute(type, "TVSHOW", ilst);
                ilst.setPosition(endPosition);
                return parseTextAttribute17;
            } else if (type == TYPE_INTERNAL) {
                Id3Frame parseInternalAttribute = parseInternalAttribute(ilst, endPosition);
                ilst.setPosition(endPosition);
                return parseInternalAttribute;
            }
        } finally {
            ilst.setPosition(endPosition);
        }
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("Skipped unknown metadata entry: ");
        sb.append(Atom.getAtomTypeString(type));
        Log.d(str, sb.toString());
        ilst.setPosition(endPosition);
        return null;
    }

    @Nullable
    private static TextInformationFrame parseTextAttribute(int type, String id, ParsableByteArray data) {
        int atomSize = data.readInt();
        if (data.readInt() == Atom.TYPE_data) {
            data.skipBytes(8);
            return new TextInformationFrame(id, null, data.readNullTerminatedString(atomSize - 16));
        }
        String value = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("Failed to parse text attribute: ");
        sb.append(Atom.getAtomTypeString(type));
        Log.w(value, sb.toString());
        return null;
    }

    @Nullable
    private static CommentFrame parseCommentAttribute(int type, ParsableByteArray data) {
        int atomSize = data.readInt();
        if (data.readInt() == Atom.TYPE_data) {
            data.skipBytes(8);
            String value = data.readNullTerminatedString(atomSize - 16);
            return new CommentFrame("und", value, value);
        }
        String value2 = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("Failed to parse comment attribute: ");
        sb.append(Atom.getAtomTypeString(type));
        Log.w(value2, sb.toString());
        return null;
    }

    @Nullable
    private static Id3Frame parseUint8Attribute(int type, String id, ParsableByteArray data, boolean isTextInformationFrame, boolean isBoolean) {
        Id3Frame id3Frame;
        int value = parseUint8AttributeValue(data);
        if (isBoolean) {
            value = Math.min(1, value);
        }
        if (value >= 0) {
            if (isTextInformationFrame) {
                id3Frame = new TextInformationFrame(id, null, Integer.toString(value));
            } else {
                id3Frame = new CommentFrame("und", id, Integer.toString(value));
            }
            return id3Frame;
        }
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("Failed to parse uint8 attribute: ");
        sb.append(Atom.getAtomTypeString(type));
        Log.w(str, sb.toString());
        return null;
    }

    @Nullable
    private static TextInformationFrame parseIndexAndCountAttribute(int type, String attributeName, ParsableByteArray data) {
        int atomSize = data.readInt();
        if (data.readInt() == Atom.TYPE_data && atomSize >= 22) {
            data.skipBytes(10);
            int index = data.readUnsignedShort();
            if (index > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("");
                sb.append(index);
                String value = sb.toString();
                int count = data.readUnsignedShort();
                if (count > 0) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append(value);
                    sb2.append("/");
                    sb2.append(count);
                    value = sb2.toString();
                }
                return new TextInformationFrame(attributeName, null, value);
            }
        }
        String str = TAG;
        StringBuilder sb3 = new StringBuilder();
        sb3.append("Failed to parse index/count attribute: ");
        sb3.append(Atom.getAtomTypeString(type));
        Log.w(str, sb3.toString());
        return null;
    }

    /* JADX WARNING: Removed duplicated region for block: B:7:0x0014  */
    /* JADX WARNING: Removed duplicated region for block: B:9:0x001c  */
    @Nullable
    private static TextInformationFrame parseStandardGenreAttribute(ParsableByteArray data) {
        String genreString;
        int genreCode = parseUint8AttributeValue(data);
        if (genreCode > 0) {
            String[] strArr = STANDARD_GENRES;
            if (genreCode <= strArr.length) {
                genreString = strArr[genreCode - 1];
                if (genreString == null) {
                    return new TextInformationFrame("TCON", null, genreString);
                }
                Log.w(TAG, "Failed to parse standard genre code");
                return null;
            }
        }
        genreString = null;
        if (genreString == null) {
        }
    }

    @Nullable
    private static ApicFrame parseCoverArt(ParsableByteArray data) {
        int atomSize = data.readInt();
        if (data.readInt() == Atom.TYPE_data) {
            int flags = Atom.parseFullAtomFlags(data.readInt());
            String mimeType = flags == 13 ? "image/jpeg" : flags == 14 ? "image/png" : null;
            if (mimeType == null) {
                String str = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("Unrecognized cover art flags: ");
                sb.append(flags);
                Log.w(str, sb.toString());
                return null;
            }
            data.skipBytes(4);
            byte[] pictureData = new byte[(atomSize - 16)];
            data.readBytes(pictureData, 0, pictureData.length);
            return new ApicFrame(mimeType, null, 3, pictureData);
        }
        Log.w(TAG, "Failed to parse cover art attribute");
        return null;
    }

    @Nullable
    private static Id3Frame parseInternalAttribute(ParsableByteArray data, int endPosition) {
        String domain = null;
        String name = null;
        int dataAtomPosition = -1;
        int dataAtomSize = -1;
        while (data.getPosition() < endPosition) {
            int atomPosition = data.getPosition();
            int atomSize = data.readInt();
            int atomType = data.readInt();
            data.skipBytes(4);
            if (atomType == Atom.TYPE_mean) {
                domain = data.readNullTerminatedString(atomSize - 12);
            } else if (atomType == Atom.TYPE_name) {
                name = data.readNullTerminatedString(atomSize - 12);
            } else {
                if (atomType == Atom.TYPE_data) {
                    dataAtomPosition = atomPosition;
                    dataAtomSize = atomSize;
                }
                data.skipBytes(atomSize - 12);
            }
        }
        if (domain == null || name == null || dataAtomPosition == -1) {
            return null;
        }
        data.setPosition(dataAtomPosition);
        data.skipBytes(16);
        return new InternalFrame(domain, name, data.readNullTerminatedString(dataAtomSize - 16));
    }

    private static int parseUint8AttributeValue(ParsableByteArray data) {
        data.skipBytes(4);
        if (data.readInt() == Atom.TYPE_data) {
            data.skipBytes(8);
            return data.readUnsignedByte();
        }
        Log.w(TAG, "Failed to parse uint8 attribute value");
        return -1;
    }
}
