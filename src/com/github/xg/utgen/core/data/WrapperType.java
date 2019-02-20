package com.github.xg.utgen.core.data;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yuxiangshi on 2018/2/5.
 */
public class WrapperType {
    static List<Class<?>> types = new ArrayList<>();

    static {
        types.add(Integer.class);
        types.add(Short.class);
        types.add(Byte.class);
        types.add(Long.class);
        types.add(Character.class);
        types.add(Float.class);
        types.add(Double.class);
        types.add(Boolean.class);
        types.add(BigDecimal.class);
        types.add(BigInteger.class);
        types.add(Class.class);
    }

    public static boolean contains(Class<?> clazz) {
        return types.contains(clazz);
    }

    public static String getValue(Class clazz) {
        if (Integer.class.equals(clazz)) {
            return "0";
        }
        if (Short.class.equals(clazz)) {
            return "0";
        }
        if (Byte.class.equals(clazz)) {
            return "0";
        }

        if (Long.class.equals(clazz)) {
            return "0L";
        }
        if (Character.class.equals(clazz)) {
            return "'a'";
        }
        if (Float.class.equals(clazz)) {
            return "0.0f";
        }

        if (Double.class.equals(clazz)) {
            return "0.0d";
        }
        if (Boolean.class.equals(clazz)) {
            return "false";
        }

        if (BigDecimal.class.equals(clazz)) {
            return "BigDecimal.ZERO";
        }
        if (BigInteger.class.equals(clazz)) {
            return "BigInteger.ZERO";
        }
        if (Class.class.equals(clazz)) {
            return "null";
        }

        throw new RuntimeException("Unsupported type" + clazz.getName());
    }

}
