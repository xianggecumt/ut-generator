package com.github.xg.utgen.core;

import java.util.HashMap;

/**
 * Created by yuxiangshi on 2017/8/10.
 */

public class VariableNames {

    private HashMap<String, Integer> paramNameCnt = new HashMap<>();

    public void resetCnt() {
        paramNameCnt.clear();
    }

    public String get(String paramName) {
        return getName(paramName);
    }

    public String get(String paramName, boolean addCnt) {
        return getName(paramName, addCnt);
    }

    public String get(Class clazz) {
        String result = "";
        if (!clazz.isPrimitive()) {
            if ("String".equals(clazz.getSimpleName())) {
                return getName("str");
            } else if ("Object".equals(clazz.getSimpleName())) {
                return getName("obj");
            } else if ("Class".equals(clazz.getSimpleName())) {
                return getName("clazz");
            } else if ("Enum".equals(clazz.getSimpleName())) {
                return getName("anEnum");
            }

            String str = clazz.getSimpleName();
            String name = str.substring(0, 1).toLowerCase() + str.substring(1);

            if (str.equals("Boolean") || str.equals("Byte") ||
                    str.equals("Short") || str.equals("Long") ||
                    str.equals("Float") || str.equals("Double")) {
                name = "a" + str;
            }

            if (name.contains("[]")) {
                name = name.substring(0, name.indexOf('[')) + "s";
            }

            return getName(name);
        }
        switch (clazz.getName()) {
            case "int":
                result = "i";
                break;
            case "long":
                result = "l";
                break;
            case "short":
                result = "s";
                break;
            case "char":
                result = "c";
                break;
            case "float":
                result = "f";
                break;
            case "double":
                result = "d";
                break;
            case "boolean":
                result = "z";
                break;
            case "byte":
                result = "b";
                break;
        }
        return getName(result);
    }

    private String getCountSuffix(int cnt) {
        return cnt == 0 ? "" : "" + cnt;
    }

    private String getName(String name) {
        return getName(name, true);
    }

    private String getName(String name, boolean addCnt) {
        if (addCnt) {
            if (paramNameCnt.containsKey(name)) {
                paramNameCnt.put(name, paramNameCnt.get(name) + 1);
            } else {
                paramNameCnt.put(name, 0);
            }
        }

        if (paramNameCnt.containsKey(name)) {
            return name + getCountSuffix(paramNameCnt.get(name));
        }
        return name;
    }
}


