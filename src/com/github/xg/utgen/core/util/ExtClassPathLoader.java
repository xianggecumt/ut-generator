package com.github.xg.utgen.core.util;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Created by yuxiangshi on 2017/8/15.
 */
public final class ExtClassPathLoader {

    private static Method addMethod;

    private static URLClassLoader classloader = (URLClassLoader) ClassLoader.getSystemClassLoader();

    static {
        try {
            addMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static void addClassPath(String path) {
        File file = new File(path);
        try {
            addMethod.invoke(classloader, file.toURI().toURL());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
