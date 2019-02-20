package com.github.xg.utgen.core;

import com.github.xg.utgen.core.util.StrKit;
import static com.github.xg.utgen.core.util.Formats.*;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.regex.Pattern;

/**
 * Created by yuxiangshi on 2018/1/5.
 */
public class ConstructorMockCode {
    static String staticInitTemplate;

    static {
        staticInitTemplate = BLANK_4 + "/**" + RT_1
                + BLANK_4 + " * Mock class initializer, includes not only any \"static\" blocks in the class, but also" + RT_1
                + BLANK_4 + " * any assignments to static fields, should be called in {@link %s#setUpClass()}." + RT_1
                + BLANK_4 + " */" + RT_1
                + BLANK_4 + "static void mockClassInitializer() {" + RT_1
                + BLANK_8 + "new MockUp<%s>() {" + RT_1
                + BLANK_8 + BLANK_4 + "@Mock" + RT_1
                + BLANK_8 + BLANK_4 + "void $clinit() {" + RT_1
                + BLANK_8 + BLANK_8 + "//Do nothing here (usually)" + RT_1
                + BLANK_8 + BLANK_4 + "}" + RT_1
                + BLANK_8 + "};" + RT_1
                + BLANK_4 + "}" + RT_2;
    }

    private final Class<?> clazz;

    private final MockCodeGenerate mockCodeGenerate;

    private StringBuilder sb = new StringBuilder();

    boolean existMapType = false;

    public ConstructorMockCode(Class<?> clazz, MockCodeGenerate mockCodeGenerate) {
        this.clazz = clazz;
        this.mockCodeGenerate = mockCodeGenerate;
    }

    public String get() {
        if (clazz.isEnum()) {
            return "";
        }
        ClassPool cp = ClassPool.getDefault();
        CtClass ctClass = null;
        try {
            ctClass = cp.getCtClass(clazz.getName());
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        if (ctClass != null && ctClass.getClassInitializer() != null) {
            sb.append(getStaticPart());
        }

        Constructor<?>[] declaredConstructors = clazz.getDeclaredConstructors();
        if (declaredConstructors != null && declaredConstructors.length > 0) {
            sb.append(BLANK_4 + "//Mock constructor" + (declaredConstructors.length > 1 ? "s" : "") + " of test class " + clazz.getSimpleName()
                    + ", should be called before instantiation." + RT_1);
            sb.append(BLANK_4 + "static class ConstructorMock {" + RT_1);
            for (Constructor<?> constructor : declaredConstructors) {
                String methodName = "mock";
                String constructorParams = "";
                Class<?>[] parameterTypes = constructor.getParameterTypes();

                if (!filter(parameterTypes)) {
                    continue;
                }
                String[] expectedVarNames = MethodHelper.getExpectedVarNames(constructor, clazz);

                for (int i = 0; i < parameterTypes.length; i++) {
                    if ("java.util.Map".equals(parameterTypes[i].getName())) {
                        existMapType = true;
                    }
                    String typeName = mockCodeGenerate.getClassName(parameterTypes[i]);
                    methodName += handleTypeName(typeName);

                    String paramName = expectedVarNames[i];
                    if (paramName == null) {
                        paramName = "param" + i;
                    }
                    constructorParams += (i == 0) ? typeName + " " + paramName : ", " + typeName + " " + paramName;
                }

                sb.append(BLANK_8 + "static void " + methodName + "() {" + RT_1);
                sb.append(BLANK_8 + BLANK_4 + "new MockUp<" + clazz.getSimpleName() + ">() {" + RT_1);
                sb.append(BLANK_8 + BLANK_8 + "@Mock" + RT_1);
                sb.append(BLANK_8 + BLANK_8 + MethodHelper.filterMethodModifier(Modifier.toString(constructor.getModifiers())) + " void $init(" + constructorParams + ") {" + RT_2);
                sb.append(BLANK_8 + BLANK_8 + "}" + RT_1);
                sb.append(BLANK_8 + BLANK_4 + "};" + RT_1);
                sb.append(BLANK_8 + "}" + RT_2);
            }
            sb.append(BLANK_4 + "}" + RT_2);
        }
        return sb.toString();
    }

    private String handleTypeName(String typeName) {
        String result = typeName.contains(".") ? typeName.substring(typeName.lastIndexOf('.') + 1) : typeName;
        if (result.contains("[]")) {
            result = result.replace("[]", "s");
        }
        return StrKit.CapsFirst(result);
    }

    private String getStaticPart() {
        return String.format(staticInitTemplate, clazz.getSimpleName() + MockCodeGenerate.suffix.replace("Mock", "Test"), clazz.getSimpleName());
    }

    private boolean filter(Class<?>[] parameterTypes) {
        for (Class<?> parameterType : parameterTypes) {
            String className = parameterType.getName();
            if(className.contains("$") && Pattern.matches(".*\\$\\d+", className)){
                return false;
            }
        }
        return true;
    }

}
