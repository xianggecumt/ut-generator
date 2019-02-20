package com.github.xg.utgen.core;

import static com.github.xg.utgen.core.util.Formats.*;


/**
 * Created by yuxiangshi on 2018/1/5.
 */
public class AdditionalMockCode {

    static String clause;

    static {
        clause = BLANK_4 + "/**" + RT_1
                + BLANK_4 + " * Creates a new instance of a given class, with any instance fields left uninitialized," + RT_1
                + BLANK_4 + " * if the given class is abstract or an interface, then a concrete class is created, with" + RT_1
                + BLANK_4 + " * empty implementations for its methods." + RT_1
                + BLANK_4 + " */" + RT_1
                + BLANK_4 + "static <T> T getMockInstance(Class<? extends T> clazz) {" + RT_1
                + BLANK_8 + "return Deencapsulation.newUninitializedInstance(clazz);" + RT_1
                + BLANK_4 + "}" + RT_2

                + BLANK_4 + "//Sets the value of a non-accessible field on a given object." + RT_1
                + BLANK_4 + "static void setField(Object objectWithField, String fieldName, Object fieldValue) {" + RT_1
                + BLANK_8 + "Deencapsulation.setField(objectWithField, fieldName, fieldValue);" + RT_1
                + BLANK_4 + "}" + RT_2

                + BLANK_4 + "//Sets the value of a non-accessible static field on a given class." + RT_1
                + BLANK_4 + "static void setStaticField(Class<?> classWithStaticField, String fieldName, Object fieldValue) throws Exception {" + RT_1
                + BLANK_8 + "Field field = classWithStaticField.getDeclaredField(fieldName);" + RT_1
                + BLANK_8 + "field.setAccessible(true);" + RT_1
                + BLANK_8 + "if (Modifier.isFinal(field.getModifiers())) {" + RT_1
                + BLANK_8 + BLANK_4 + "Field modifiersField = Field.class.getDeclaredField(\"modifiers\");" + RT_1
                + BLANK_8 + BLANK_4 + "modifiersField.setAccessible(true);" + RT_1
                + BLANK_8 + BLANK_4 + "modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);" + RT_1
                + BLANK_8 + "}" + RT_1
                + BLANK_8 + "field.set(null, fieldValue);" + RT_1
                + BLANK_4 + "}" + RT_2

                + BLANK_4 + "//Gets the value of a non-accessible field from a given object." + RT_1
                + BLANK_4 + "static <T> T getField(Object objectWithField, String fieldName) {" + RT_1
                + BLANK_8 + "return Deencapsulation.getField(objectWithField, fieldName);" + RT_1
                + BLANK_4 + "}" + RT_2

                + BLANK_4 + "//Gets the value of a non-accessible static field defined in a given class." + RT_1
                + BLANK_4 + "static <T> T getStaticField(Class<?> classWithStaticField, String fieldName) {" + RT_1
                + BLANK_8 + "return Deencapsulation.getField(classWithStaticField, fieldName);" + RT_1
                + BLANK_4 + "}" + RT_2;

    }
}
