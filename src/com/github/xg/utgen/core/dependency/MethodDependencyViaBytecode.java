package com.github.xg.utgen.core.dependency;

import com.github.xg.utgen.core.MethodHelper;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by yuxiangshi on 2017/9/26.
 */
public class MethodDependencyViaBytecode implements MethodDependency {

    private Class clazz;

    public MethodDependencyViaBytecode(Class clazz) {
        this.clazz = clazz;
    }

    @Override
    public Map<Method, Map<Class, List<Method>>> getDependencies() throws Exception {
        Map<Method, Map<Class, List<Method>>> result = new HashMap<>();

        CtClass ctClass;
        try {
            ctClass = MethodHelper.getCtClass(clazz);
            ctClass.defrost();
        } catch (Exception e) {
            e.printStackTrace();
            return result;
        }

        for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
            if (Pattern.matches("access\\$\\d+", ctMethod.getName())) {
                continue;
            }
            Method jdkMethod = getMethod(clazz, ctMethod);
            if (!MethodHelper.filterMethod(jdkMethod)) {
                continue;
            }

            //ArrayListMultimap cause out of order
            Multimap<Class, Method> multiMap = LinkedHashMultimap.create();
            Map<Class, List<Method>> map = new LinkedHashMap<>();

            try {
                try {
                    ctClass.defrost();
                } catch (Exception e) {
                }
                ctMethod.instrument(new ExprEditor() {
                    @Override
                    public void edit(MethodCall m) {
                        if (!ExcludeMockStrategy.filterClass(m.getClassName())) {
                            return;
                        }
                        try {
                            if (!ExcludeMockStrategy.filterMethod(m.getClassName(), m.getMethod().getName())) {
                                return;
                            }
                        } catch (NotFoundException e) {
                            e.printStackTrace();
                        }
                        Class callClass;
                        try {
                            callClass = Class.forName(m.getClassName(), false, ClassLoader.getSystemClassLoader());

                            //if private, return
                            if (Modifier.isPrivate(callClass.getModifiers())) {
                                return;
                            }
                            Method method = getMethod(callClass, m.getMethod());
                            //add in version 1.5.3
                            if (!ExcludeMockStrategy.filterClass(method.getDeclaringClass().getName())) {
                                return;
                            }

                            if (!multiMap.containsValue(method) || !multiMap.containsKey(callClass)) {
                                multiMap.put(callClass, method);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (CannotCompileException e) {
                e.printStackTrace();
            }

            Map<Class, Collection<Method>> classCollectionMap = multiMap.asMap();
            for (Class aClass : classCollectionMap.keySet()) {
                map.put(aClass, new ArrayList<>(classCollectionMap.get(aClass)));
            }
            result.put(jdkMethod, map);
        }
        return result;
    }

    private Class convert(CtClass ctClass) throws Exception {
        if (ctClass.isPrimitive()) {
            switch ((((CtPrimitiveType) ctClass)).getDescriptor()) {
                case 'Z':
                    return boolean.class;
                case 'C':
                    return char.class;
                case 'B':
                    return byte.class;
                case 'S':
                    return short.class;
                case 'I':
                    return int.class;
                case 'J':
                    return long.class;
                case 'F':
                    return float.class;
                case 'D':
                    return double.class;
            }
        }

        try {
            return ctClass.toClass();
        } catch (Exception e) {
            //e.printStackTrace();
        }

        String className = "";
        CtClass componentType = ctClass;
        int dimension = 0;
        while (componentType.isArray()) {
            componentType = componentType.getComponentType();
            dimension++;
        }
        for (int i = 0; i < dimension; i++) {
            className += "[";
            if (i == dimension - 1 && !componentType.isPrimitive()) {
                className += "L";
            }
        }
        if (componentType.isPrimitive()) {
            className += ((CtPrimitiveType) componentType).getDescriptor();
        } else {
            className += componentType.getName();
        }
        if (dimension > 0 && !componentType.isPrimitive()) {
            className += ";";
        }

        Class cls = Class.forName(className, false, ClassLoader.getSystemClassLoader());
        return Objects.requireNonNull(cls);
    }


    private Method getMethod(Class clazz, CtMethod ctMethod) throws Exception {
        Set<Method> methods = new HashSet<>();
        methods.addAll(Arrays.asList(clazz.getMethods()));
        methods.addAll(Arrays.asList(clazz.getDeclaredMethods()));

        Method method = getMethod0(ctMethod, methods);
        if (method != null) {
            return method;
        }

        Class<?> superclass = clazz.getSuperclass();
        //super class protected method
        List<Method> protectedSuperMethods = new ArrayList<>();
        if (superclass != null) {
            for (Method m : superclass.getDeclaredMethods()) {
                if (Modifier.isProtected(m.getModifiers())) {
                    protectedSuperMethods.add(m);
                }
            }
        }
        Method method1 = getMethod0(ctMethod, protectedSuperMethods);
        if (method1 != null) {
            return method1;
        }

        //super class package-private method
        List<Method> packageAccessSuperMethods = new ArrayList<>();
        if (superclass != null) {
            for (Method m : superclass.getDeclaredMethods()) {
                if (!Modifier.isPrivate(m.getModifiers())) {
                    packageAccessSuperMethods.add(m);
                }
            }
        }
        Method method2 = getMethod0(ctMethod, packageAccessSuperMethods);
        if (method2 != null) {
            return method2;
        }
        throw new NotFoundException(String.valueOf(ctMethod));
    }

    private Method getMethod0(CtMethod ctMethod, Collection<Method> methods) throws Exception {
        Class returnCls = null;
        if (!ctMethod.getReturnType().getSimpleName().equals("void")) {
            try {
                returnCls = convert(ctMethod.getReturnType());
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        for (Method method : methods) {
            if (method.getName().equals(ctMethod.getName())) {
                if (returnCls != null) {
                    if (!returnCls.getName().equals(method.getReturnType().getName())) {
                        continue;
                    }
                }
                Class<?>[] parameterTypes = method.getParameterTypes();
                CtClass[] parameterTypes1 = ctMethod.getParameterTypes();
                int i = parameterTypes.length;
                int i1 = parameterTypes1.length;

                if (i == i1) {
                    boolean b = true;
                    for (int j = 0; j < i; j++) {
                        if (!parameterTypes[j].getName().equals(convert(parameterTypes1[j]).getName())) {
                            b = false;
                        }
                    }
                    if (b) {
                        return method;
                    }
                }
            }
        }
        return null;
    }

}
