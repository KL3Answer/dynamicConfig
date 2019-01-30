package org.k3a.utils;

import java.util.Map;
import java.util.Properties;
import java.util.Stack;

/**
 * Created by HQ.heqing
 * on 2018/6/19  下午4:43
 */
public class SimplePlaceHolderResolver {

    public static final SimplePlaceHolderResolver DEFAULT = new SimplePlaceHolderResolver("${", "}");

    private final String startChars;
    private final String endChars;

    public SimplePlaceHolderResolver(String startChars, String endChars) {
        this.startChars = startChars;
        this.endChars = endChars;
    }

    /**
     * resolve placeHolder
     */
    public String resolvePropertiesPlaceHolder(Properties properties, final String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        final int[] pair = get1stPair(startChars, endChars, key);
        //return origin key when placeHolder pair not found
        if (pair[0] == -1 || pair[1] == -1) {
            return key;
        }

        final String pre = key.substring(0, pair[0]);
        final String replacement = resolvePropertiesPlaceHolder(properties, key.substring(pair[0] + startChars.length(), pair[1]));
        final String mid = properties.getProperty(replacement);
        if (mid == null || mid.isEmpty()) {
            throw new IllegalArgumentException("malformed placeHolder value:" + startChars + replacement + endChars);
        }

        return pre + mid + resolvePropertiesPlaceHolder(properties, key.substring(pair[1] + endChars.length()));
    }

    /**
     * resolve placeHolder
     */
    public String resolvePlaceHolder(Map<String, String> map, final String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        final int[] pair = get1stPair(startChars, endChars, key);
        //return origin key when placeHolder pair not found
        if (pair[0] == -1 || pair[1] == -1) {
            return key;
        }

        final String pre = key.substring(0, pair[0]);
        final String replacement = resolvePlaceHolder(map, key.substring(pair[0] + startChars.length(), pair[1]));
        final String mid = map.get(replacement);
        if (mid == null || mid.isEmpty()) {
            throw new IllegalArgumentException("malformed placeHolder value:" + startChars + replacement + endChars);
        }

        return pre + mid + resolvePlaceHolder(map, key.substring(pair[1] + endChars.length()));
    }

    /**
     * find first pair
     */
    public static int[] get1stPair(final String start, final String end, final String text) {
        int[] arr = {-1, -1};
        final Stack<Integer> startIndex = new Stack<>();

        for (int i = 0; i <= text.length(); i++) {
            if (i - start.length() >= 0 && text.substring(i - start.length(), i).equals(start)) {
                startIndex.push(i - start.length());
            } else if (i - end.length() >= 0 && text.substring(i - end.length(), i).equals(end)) {
                if (startIndex.size() > 0) {
                    final Integer pop = startIndex.pop();
                    if (pop != null) {
                        if (startIndex.empty()) {
                            return new int[]{pop, i - end.length()};
                        } else {
                            arr = new int[]{pop, i - end.length()};
                        }
                    }
                }
            }
        }

        return arr;
    }


}