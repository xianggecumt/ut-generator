package com.github.xg.utgen.core.model;

/**
 * Created by yuxiangshi on 2017/9/8.
 */
public class ClassInfo {

    private String classFullName;

    private String packageName;

    private String classPath;

    private String classSourcePath;

    public String getClassFullName() {
        return classFullName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassPath() {
        return classPath;
    }

    public String getClassSourcePath() {
        return classSourcePath;
    }

    public ClassInfo(String classFullName, String classPath, String classSourcePath) {
        this.classFullName = classFullName;
        this.classPath = classPath;
        this.classSourcePath = classSourcePath;
        this.packageName = classFullName.substring(0, classFullName.lastIndexOf('.'));
    }
}
