package com.github.xg.utgen.core.util;

/**
 * Created by yuxiangshi on 2018/1/4.
 */
public class StrKit {

    public static boolean blank(final String str) {
        int strLen;
        if ((str == null) || ((strLen = str.length()) == 0)) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static String CapsFirst(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
