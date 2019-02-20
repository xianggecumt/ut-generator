package com.github.xg.utgen.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yuxiangshi on 2017/9/4.
 */
public abstract class AbstractCodeGenerate {

    protected List<String> importClasses = new ArrayList<>();

    protected abstract void generate();

    protected String getClassName(Class clazz) {
        if (!clazz.getName().contains(".")) {
            return clazz.getTypeName();
        }
        String result = clazz.getTypeName();

        Class c = clazz;
        while (c.isArray()) {
            c = c.getComponentType();
        }

        Class outClass = c.getEnclosingClass();
        if (outClass != null) {
            String outClassName = outClass.getTypeName();
            return result.substring(0, outClassName.length()) + "." + result.substring(outClassName.length() + 1);
        }

        String className = c.getTypeName();
        String shortName = c.getSimpleName();

        boolean b = false;
        for (int i = 0; i < importClasses.size(); i++) {
            String importClass = importClasses.get(i);
            if (importClass.substring(importClass.lastIndexOf(".") + 1).equals(shortName) && !importClass.equals(className)) {
                b = true;
                break;
            }
        }
        if (b) {
            return result;
        }

        if (!importClasses.contains(className)) {
            importClasses.add(className);
        }
        return result.substring(result.lastIndexOf(".") + 1);
    }

}
