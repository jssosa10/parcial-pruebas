package org.apache.commons.lang3;

import java.util.Random;

public class RandomStringUtils {
    private static final Random RANDOM = new Random();

    public static String random(int count) {
        return random(count, false, false);
    }

    public static String randomAscii(int count) {
        return random(count, 32, 127, false, false);
    }

    public static String randomAscii(int minLengthInclusive, int maxLengthExclusive) {
        return randomAscii(RandomUtils.nextInt(minLengthInclusive, maxLengthExclusive));
    }

    public static String randomAlphabetic(int count) {
        return random(count, true, false);
    }

    public static String randomAlphabetic(int minLengthInclusive, int maxLengthExclusive) {
        return randomAlphabetic(RandomUtils.nextInt(minLengthInclusive, maxLengthExclusive));
    }

    public static String randomAlphanumeric(int count) {
        return random(count, true, true);
    }

    public static String randomAlphanumeric(int minLengthInclusive, int maxLengthExclusive) {
        return randomAlphanumeric(RandomUtils.nextInt(minLengthInclusive, maxLengthExclusive));
    }

    public static String randomGraph(int count) {
        return random(count, 33, 126, false, false);
    }

    public static String randomGraph(int minLengthInclusive, int maxLengthExclusive) {
        return randomGraph(RandomUtils.nextInt(minLengthInclusive, maxLengthExclusive));
    }

    public static String randomNumeric(int count) {
        return random(count, false, true);
    }

    public static String randomNumeric(int minLengthInclusive, int maxLengthExclusive) {
        return randomNumeric(RandomUtils.nextInt(minLengthInclusive, maxLengthExclusive));
    }

    public static String randomPrint(int count) {
        return random(count, 32, 126, false, false);
    }

    public static String randomPrint(int minLengthInclusive, int maxLengthExclusive) {
        return randomPrint(RandomUtils.nextInt(minLengthInclusive, maxLengthExclusive));
    }

    public static String random(int count, boolean letters, boolean numbers) {
        return random(count, 0, 0, letters, numbers);
    }

    public static String random(int count, int start, int end, boolean letters, boolean numbers) {
        return random(count, start, end, letters, numbers, null, RANDOM);
    }

    public static String random(int count, int start, int end, boolean letters, boolean numbers, char... chars) {
        return random(count, start, end, letters, numbers, chars, RANDOM);
    }

    public static String random(int codePoint, int start, int end, boolean letters, boolean numbers, char[] chars, Random random) {
        int count;
        if (codePoint == 0) {
            return "";
        }
        if (codePoint < 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("Requested random string length ");
            sb.append(codePoint);
            sb.append(" is less than 0.");
            throw new IllegalArgumentException(sb.toString());
        } else if (chars == 0 || chars.length != 0) {
            if (start == 0 && end == 0) {
                if (chars != 0) {
                    end = chars.length;
                } else if (letters || numbers) {
                    end = 123;
                    start = 32;
                } else {
                    end = 1114111;
                }
            } else if (end <= start) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Parameter end (");
                sb2.append(end);
                sb2.append(") must be greater than start (");
                sb2.append(start);
                sb2.append(")");
                throw new IllegalArgumentException(sb2.toString());
            }
            if (chars != 0 || ((!numbers || end > 48) && (!letters || end > 65))) {
                StringBuilder builder = new StringBuilder(codePoint);
                int gap = end - start;
                while (true) {
                    int count2 = codePoint - 1;
                    if (codePoint == 0) {
                        return builder.toString();
                    }
                    if (chars == 0) {
                        count = random.nextInt(gap) + start;
                        int type = Character.getType(count);
                        if (type != 0) {
                            switch (type) {
                                case 18:
                                case 19:
                                    break;
                            }
                        }
                        codePoint = count2 + 1;
                    } else {
                        count = chars[random.nextInt(gap) + start];
                    }
                    int numberOfChars = Character.charCount(count);
                    if (count2 == 0 && numberOfChars > 1) {
                        codePoint = count2 + 1;
                    } else if ((!letters || !Character.isLetter(count)) && ((!numbers || !Character.isDigit(count)) && (letters || numbers))) {
                        codePoint = count2 + 1;
                    } else {
                        builder.appendCodePoint(count);
                        if (numberOfChars == 2) {
                            codePoint = count2 - 1;
                        } else {
                            codePoint = count2;
                        }
                    }
                }
            } else {
                StringBuilder sb3 = new StringBuilder();
                sb3.append("Parameter end (");
                sb3.append(end);
                sb3.append(") must be greater then (");
                sb3.append(48);
                sb3.append(") for generating digits ");
                sb3.append("or greater then (");
                sb3.append(65);
                sb3.append(") for generating letters.");
                throw new IllegalArgumentException(sb3.toString());
            }
        } else {
            throw new IllegalArgumentException("The chars array must not be empty");
        }
    }

    public static String random(int count, String chars) {
        if (chars != null) {
            return random(count, chars.toCharArray());
        }
        return random(count, 0, 0, false, false, null, RANDOM);
    }

    public static String random(int count, char... chars) {
        if (chars == null) {
            return random(count, 0, 0, false, false, null, RANDOM);
        }
        return random(count, 0, chars.length, false, false, chars, RANDOM);
    }
}
