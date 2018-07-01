package org.k3a.utils;

import java.util.Properties;

/**
 * Created by HQ.heqing
 * on 2018/6/19  下午4:43
 */
public class SimplePlaceHolderResolver {

    //placeholder symbols
    private static final String[][] PLACE_HOLDER_CHARS = {{"${", "}"}, {"#{", "}"}};

    /**
     * resolve ${} #{}
     */
    public static String resolvePlaceHolder(Properties props, String key, String defaultValue) {
        String property = props.getProperty(key);
        if (property == null || property.equals(""))
            return defaultValue;

        int i, j = 0;
        while ((i = property.indexOf(PLACE_HOLDER_CHARS[0][0], j)) >= 0 && (j = property.indexOf(PLACE_HOLDER_CHARS[0][1], j + 1)) >= 0) {
            // create place Holder
            String holder = property.substring(i + PLACE_HOLDER_CHARS[0][0].length(), j);
            property = property.substring(0, i) + resolvePlaceHolder(props, holder, PLACE_HOLDER_CHARS[0][0] + holder + PLACE_HOLDER_CHARS[0][1]) + property.substring(j + PLACE_HOLDER_CHARS[0][1].length());
        }

        j = 0;
        if ((i = property.indexOf(PLACE_HOLDER_CHARS[1][0], j)) >= 0 && (j = property.indexOf(PLACE_HOLDER_CHARS[1][1], j + 1)) >= 0) {
            // create place Holder
            String holder = property.substring(i + PLACE_HOLDER_CHARS[1][0].length(), j);
            property = property.substring(0, i) + resolvePlaceHolder(props, holder, PLACE_HOLDER_CHARS[1][0] + holder + PLACE_HOLDER_CHARS[1][1]) + property.substring(j + PLACE_HOLDER_CHARS[1][1].length());
        }

        return property;
    }

}