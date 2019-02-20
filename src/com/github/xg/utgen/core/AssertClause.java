package com.github.xg.utgen.core;

import com.github.xg.utgen.core.data.PrimitiveType;

/**
 * Created by yuxiangshi on 2017/8/11.
 */
public class AssertClause {

    private static final String ASSERT = "assertEquals(%s, %s);";

    public static String get(Class clazz, String variableName) {

        String expectedValue = PrimitiveType.get(clazz);
        if(expectedValue.equals("null")){
            return "assertNotNull("+variableName+");";
        }
        if(clazz.getSimpleName().equals("boolean")||clazz.getSimpleName().equals("Boolean")){
            return "assertFalse("+variableName+");";
        }else if(clazz.isArray()){
            return "assertArrayEquals("+expectedValue+", "+variableName+");";
        }
        return String.format(ASSERT, expectedValue, variableName);
    }
}
