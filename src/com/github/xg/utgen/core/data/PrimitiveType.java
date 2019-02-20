package com.github.xg.utgen.core.data;

/**
 * Created by yuxiangshi on 2017/8/10.
 */
public class PrimitiveType {

    public static String get(Class clazz) {
        String result = "null";
        if (!clazz.isPrimitive()) {
            if ("String".equals(clazz.getSimpleName())) {
                return "\"hello\"";
            }
            return result;
        }
        switch (clazz.getName()) {
            case "int":
            case "short":
            case "byte":
                result = "0";
                break;
            case "long":
                result = "0L";
                break;
            case "char":
                result = "'a'";
                break;
            case "float":
                result = "0.0f";
                break;
            case "double":
                result = "0.0d";
                break;
            case "boolean":
                result = "false";
                break;
            default:
                break;
        }

        return result;
    }
}
