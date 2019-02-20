package com.github.xg.utgen.core.dependency;

import com.github.xg.utgen.core.util.StrKit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Created by yuxiangshi on 2017/10/23.
 */
public class ExcludeMockStrategy {

    static List<String> packages = new ArrayList<>();

    static List<String> methodPrefixes = new ArrayList<>();

    static List<String> methodSuffixes = new ArrayList<>();

    static List<String> packageMethodPrefixes = new ArrayList<>();

    static List<String> packageMethodSuffixes = new ArrayList<>();

    static List<String> onlyIncludeMockPackageMethods = new ArrayList<>();

    static {
        packages.add("java.");
        packages.add("sun.");
        packages.add("javax.");
        packages.add("org.xml");
        packages.add("org.w3c");
        packages.add("org.omg.");
        packages.add("sunw.");
        packages.add("org.jcp.");
        packages.add("org.ietf.");
        packages.add("daikon.");
        packages.add("com.google.common");
        packages.add("org.exsyst");
        packages.add("org.joda.time");
    }

    public static boolean filterClass(String className) {
        if (className.contains("[]")) {
            return false;
        }
        for (String pack : packages) {
            if (className.startsWith(pack)) {
                return false;
            }
        }
        //anonymous class
        if(className.contains("$") && Pattern.matches(".*\\$\\d+", className)){
            return false;
        }
        return true;
    }

    public static boolean filterMethod(String className, String methodName) {
        if (Pattern.matches("access\\$\\d+", methodName)) {
            return false;
        }

        if (onlyIncludeMockPackageMethods.size() > 0) {
            for (String packageMethod : onlyIncludeMockPackageMethods) {
                String[] split = packageMethod.split(":");
                if (split.length == 2) {
                    if (className.startsWith(split[0]) && methodName.equals(split[1])) {
                        return true;
                    }
                } else {
                    if (className.startsWith(packageMethod)) {
                        return true;
                    }
                }
            }
            return false;
        }

        for (String methodPrefix : methodPrefixes) {
            if (methodName.startsWith(methodPrefix)) {
                return false;
            }
        }
        for (String methodSuffix : methodSuffixes) {
            if (methodName.endsWith(methodSuffix)) {
                return false;
            }
        }
        for (String packageMethodPrefix : packageMethodPrefixes) {
            String packName = packageMethodPrefix.split(":")[0];
            String methName = packageMethodPrefix.split(":")[1];
            if (className.startsWith(packName) && methodName.startsWith(methName)) {
                return false;
            }
        }
        for (String packageMethodSuffix : packageMethodSuffixes) {
            String packName = packageMethodSuffix.split(":")[0];
            String methName = packageMethodSuffix.split(":")[1];
            if (className.startsWith(packName) && methodName.endsWith(methName)) {
                return false;
            }
        }
        return true;
    }

    public static void addPackages(String[] packs) {
        if (Objects.isNull(packs)) {
            return;
        }
        for (String pack : packs) {
            if (pack != null) {
                packages.add(pack);
            }
        }
    }

    public static void addMethodPrefix(String[] prefixes) {
        if (Objects.isNull(prefixes)) {
            return;
        }
        for (String prefix : prefixes) {
            if (prefix != null) {
                methodPrefixes.add(prefix);
            }
        }
    }

    public static void addMethodSuffix(String[] suffixes) {
        if (Objects.isNull(suffixes)) {
            return;
        }
        for (String suffix : suffixes) {
            if (suffix != null) {
                methodSuffixes.add(suffix);
            }
        }
    }

    public static void addPackageMethodPrefix(String[] prefixes) {
        if (Objects.isNull(prefixes)) {
            return;
        }
        for (String prefix : prefixes) {
            if (prefix == null || prefix.split(":").length != 2) {
                continue;
            }
            packageMethodPrefixes.add(prefix);
        }
    }

    public static void addPackageMethodSuffix(String[] suffixes) {
        if (Objects.isNull(suffixes)) {
            return;
        }
        for (String suffix : suffixes) {
            if (suffix == null || suffix.split(":").length != 2) {
                continue;
            }
            packageMethodSuffixes.add(suffix);
        }
    }

    public static void addOnlyIncludeMockPackageMethods(String[] methods) {
        if (Objects.isNull(methods)) {
            return;
        }
        for (String method : methods) {
            if (!StrKit.blank(method)) {
                onlyIncludeMockPackageMethods.add(method);
            }
        }
    }
}
