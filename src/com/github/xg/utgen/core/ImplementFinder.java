package com.github.xg.utgen.core;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

/**
 * Created by yuxiangshi on 2017/8/28.
 */
public class ImplementFinder {

    private Map<String, List<String>> srcPathClassesMap;

    public ImplementFinder(Map<String, List<String>> srcPathClassesMap) {
        this.srcPathClassesMap = srcPathClassesMap;
    }

    public Class find(Class abstractClass) {
        String packageName = abstractClass.getPackage().getName();
        for (String key : srcPathClassesMap.keySet()) {
            for (String className : srcPathClassesMap.get(key)) {
                try {
                    if (!packageName.equals(getPackageName(className))) {
                        continue;
                    }
                    Class clazz = Class.forName(className, false, ClassLoader.getSystemClassLoader());
                    if (!Modifier.isPublic(clazz.getModifiers()) || clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                        continue;
                    }
                    Class[] interfaces = clazz.getInterfaces();
                    for (int i = 0; i < interfaces.length; i++) {
                        if (interfaces[i].equals(abstractClass)) {
                            return clazz;
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private String getPackageName(String className) {
        String packageName = "";
        int dotIndex = className.lastIndexOf('.');
        if (dotIndex > 0) {
            packageName = className.substring(0, dotIndex);
        }
        return packageName;
    }

}
