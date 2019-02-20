package com.github.xg.utgen.core;

import com.google.common.base.CharMatcher;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.io.InputStream;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by yuxiangshi on 2017/10/30.
 */
public class MethodHelper {

    static final String[] excludeModifiers = {"static", "abstract", "transient", "volatile"};

    private MethodHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean filterMethod(Method method) {
        if (method.getName().startsWith("lambda$")) {
            return false;
        }

        if (Modifier.isStatic(method.getModifiers()) && Pattern.matches("access\\$\\d+", method.getName())) {
            return false;
        }
        Class<?> declaringClass = method.getDeclaringClass();

        if (method.getTypeParameters().length > 0) {
            return true;
        }

        TypeVariable<? extends Class<?>>[] classTypeParameters = declaringClass.getTypeParameters();
        for (Type methodParamType : method.getGenericParameterTypes()) {
            for (TypeVariable<? extends Class<?>> classTypeParameter : classTypeParameters) {
                if (methodParamType.getTypeName().equals(classTypeParameter.getTypeName())) {
                    return true;
                }
            }
        }

        List<Type> genericSuperTypes = new ArrayList<>();

        for (Type type : declaringClass.getGenericInterfaces()) {
            genericSuperTypes.add(type);
        }
        genericSuperTypes.add(declaringClass.getGenericSuperclass());

        for (Type type : genericSuperTypes) {
            if (type instanceof ParameterizedTypeImpl) {
                final Class<?> rawType = ((ParameterizedTypeImpl) type).getRawType();
                TypeVariable<? extends Class<?>>[] interfaceTypeParameters = rawType.getTypeParameters();
                List<TypeVariable<? extends Class<?>>> interfaceTypeParametersList = Arrays.asList(interfaceTypeParameters);

                for (Method interfaceMethod : rawType.getDeclaredMethods()) {
                    boolean useInterfaceTypeParameter = false;
                    List<Type> interfaceMethodGenericTypes = new ArrayList<>(Arrays.asList(interfaceMethod.getGenericParameterTypes()));
                    interfaceMethodGenericTypes.add(interfaceMethod.getGenericReturnType());

                    for (Type interfaceMethodGenericType : interfaceMethodGenericTypes) {
                        for (TypeVariable<? extends Class<?>> typeVariable : interfaceTypeParametersList) {
                            if (typeVariable.getTypeName().equals(interfaceMethodGenericType.getTypeName())) {
                                useInterfaceTypeParameter = true;
                                break;
                            }
                        }
                        if (useInterfaceTypeParameter) {
                            break;
                        }
                    }

                    if (useInterfaceTypeParameter) {
                        if (methodEqualsIgnoringDeclaringClass(method, interfaceMethod)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    static boolean methodEqualsIgnoringDeclaringClass(Method m1, Method m2) {
        if (m1.getName().equals(m2.getName()) && m1.getReturnType().equals(m2.getReturnType())) {
            Class<?>[] params1 = m1.getParameterTypes();
            Class<?>[] params2 = m2.getParameterTypes();
            if (params1.length == params2.length) {
                for (int i = 0; i < params1.length; i++) {
                    if (params1[i] != params2[i]) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    static boolean isDeprecated(Executable executable) {
        Deprecated annotation = executable.getAnnotation(Deprecated.class);
        if (annotation != null) {
            return true;
        } else {
            return false;
        }
    }

    static <T extends Executable> String needTryCatch(T executable) {
        String hasException = null;
        boolean checkedException = false;
        boolean throwable = false;
        Class[] exceptions = executable.getExceptionTypes();
        if (exceptions.length > 0) {
            for (Class exception : exceptions) {
                if (!RuntimeException.class.isAssignableFrom(exception)) {
                    checkedException = true;

                    if (!Exception.class.isAssignableFrom(exception)) {
                        throwable = true;
                        break;
                    }
                }
            }
        }

        if (checkedException) {
            hasException = "Exception";
        }
        if (throwable) {
            hasException = "Throwable";
        }
        return hasException;
    }

    static CtBehavior convertToCt(Executable executable, CtClass ctClass) {
        Class[] parameterTypes = executable.getParameterTypes();

        CtClass[] params = new CtClass[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            params[i] = getCtClass(parameterTypes[i]);
        }
        CtBehavior behavior = null;
        try {
            if (executable instanceof Constructor) {
                behavior = ctClass.getDeclaredConstructor(params);
            } else {
                behavior = ctClass.getDeclaredMethod(executable.getName(), params);
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        return behavior;
    }

    public static CtClass getCtClass(Class<?> c) {
        ClassPool cp = ClassPool.getDefault();
        CtClass ctClass = null;

        InputStream in = ClassLoader.getSystemResourceAsStream(c.getName().replace('.', '\\') + ".class");
        if (in != null) {
            try {
                ctClass = cp.makeClass(in, false);
                ctClass.defrost();
            } catch (Exception e) {
            }
            if (ctClass != null) {
                return ctClass;
            }
        }

        try {
            ctClass = cp.getCtClass(c.getName());
            ctClass.defrost();
        } catch (Exception e) {
        }
        return Objects.requireNonNull(ctClass);
    }

    static String[] getExpectedVarNames(Executable method, Class<?> clazz) {
        String[] paramNames = new String[method.getParameterTypes().length];

        CtBehavior behavior = convertToCt(method, getCtClass(clazz));
        if (behavior == null) {
            return paramNames;
        }

        MethodInfo methodInfo = behavior.getMethodInfo();
        CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
        if (codeAttribute == null) {
            return paramNames;
        }
        LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute
                .getAttribute(LocalVariableAttribute.tag);
        if (attr == null) {
            return paramNames;
        }

        TreeMap<Integer, Integer> map = new TreeMap<>();
        for (int i = 0; i < attr.tableLength(); i++) {
            map.put(attr.index(i), i);
        }
        int index = 0;
        boolean isStaticMethod = javassist.Modifier.isStatic(behavior.getModifiers());
        boolean flag = false;
        for (Integer key : map.keySet()) {
            if (!isStaticMethod && !flag) {
                flag = true;
                continue;
            }

            if (index < paramNames.length) {
                paramNames[index++] = attr.variableName(map.get(key));
            } else {
                break;
            }
        }
        return paramNames;
    }

    static Constructor chooseConstructor(Constructor<?>[] constructors, Boolean preferConstructorWithParams) {
        Constructor ctor = null;
        Constructor ctor1 = null;
        Constructor ctorWithNoParams = null;
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterTypes().length == 0) {
                ctorWithNoParams = constructor;
            }
        }

        if (ctorWithNoParams != null && (preferConstructorWithParams == null || !preferConstructorWithParams)) {
            return ctorWithNoParams;
        }

        for (int i = 0; i < constructors.length; i++) {
            if (!MethodHelper.isDeprecated(constructors[i])) {
                ctor1 = constructors[i];
                if (constructors[i].getParameterTypes().length > 0) {
                    ctor = constructors[i];
                    break;
                }
            }
        }
        if (ctor == null) {
            ctor = ctor1;
        }
        return ctor;
    }

    static String filterMethodModifier(String modifier) {
        for (String s : excludeModifiers) {
            if (modifier.contains(s)) {
                modifier = modifier.replace(s, "");
            }
        }
        return CharMatcher.WHITESPACE.collapseFrom(modifier, ' ').trim();
    }

    public static String getMethodString(Method method) {
        if (method == null) {
            return "";
        }
        String methodString = method.toString();
        int index = methodString.lastIndexOf('(');
        int spaceIndex = -1;
        for (int i = index - 1; i >= 0; i--) {
            if (methodString.charAt(i) == ' ') {
                spaceIndex = i;
                break;
            }
        }
        try {
            String fullMethodName = methodString.substring(spaceIndex + 1, index);
            String shortMethodName = fullMethodName.substring(fullMethodName.lastIndexOf('.') + 1);
            return methodString.substring(0, spaceIndex + 1) + shortMethodName + methodString.substring(index);
        } catch (Exception ex) {
            return method.toString();
        }

    }
}
