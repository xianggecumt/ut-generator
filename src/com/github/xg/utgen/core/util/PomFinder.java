package com.github.xg.utgen.core.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by yuxiangshi on 2017/8/14.
 */
public class PomFinder {
    List<String> allPomPaths = new ArrayList<>();
    List<String> allPomPathsWithSrc = new ArrayList<>();

    public List<String> getAllPomFilesWithSrc(String strPath) {
        File dir = new File(strPath);
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                String fileName = files[i].getName();
                if (files[i].isDirectory()) {
                    getAllPomFilesWithSrc(files[i].getAbsolutePath());
                } else if ("pom.xml".equals(fileName) && containsSrc(files)) {
                    allPomPathsWithSrc.add(files[i].getParent());
                }
            }
        }
        return allPomPathsWithSrc;
    }

    public List<String> getAllPomFiles(String strPath) {
        File dir = new File(strPath);
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                String fileName = files[i].getName();
                if (files[i].isDirectory()) {
                    getAllPomFiles(files[i].getAbsolutePath());
                } else if ("pom.xml".equals(fileName)) {

                    allPomPaths.add(files[i].getParent());
                }
            }
        }
        return allPomPaths;
    }

    public static String getParentPomPath(List<String> paths) {

        if (paths == null || paths.size() == 0) {
            return "";
        }
        boolean ifFind = false;
        String path = paths.get(0);

        for (int i = 1; i < paths.size(); i++) {
            if (path.split("\\\\").length > paths.get(i).split("\\\\").length) {
                ifFind = true;
                path = paths.get(i);
            }
        }
        if (ifFind) {
            return path;
        }
        return null;
    }

    List<String> files = new ArrayList<>();

    public List<String> findFiles(File file, String suffix) {

        if (file.isDirectory()) {
            Arrays.stream(file.listFiles()).forEach(e -> {
                findFiles(e, suffix);
            });
        } else {
            if (file.getName().contains(suffix)) {
                files.add(file.getPath());
            }
        }
        return files;
    }
    
    public boolean containsSrc(File[] files) {
        boolean flag = false;
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory() && "src".equals(files[i].getName())) {
                flag = true;
                break;
            }
        }
        return flag;
    }
}
