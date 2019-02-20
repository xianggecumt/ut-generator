package com.github.xg.utgen.core.util;

/**
 * Created by yuxiangshi on 2017/9/18.
 */
public class RandomValue {

    public static String get(Class<?> clazz){
        return null;
    }

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
}
