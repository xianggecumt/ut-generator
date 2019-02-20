package com.github.xg.utgen.core.dependency;

import com.github.xg.utgen.core.util.IoUtil;
import com.github.xg.utgen.core.util.RandomValue;
import com.google.common.base.Joiner;
import java.util.*;

/**
 * Created by yuxiangshi on 2017/8/16.
 */
public class MavenDependencyParser {

    List<String> dependencyFiles;

    Set<String> set = new HashSet<>();

    private String repoPath;

    public MavenDependencyParser(List<String> dependencyFiles,String repoPath) {
        this.dependencyFiles = dependencyFiles;
        this.repoPath = repoPath;
    }

    public static void main(String[] args) {
        String[] strings = "org.apache.httpcomponents".split("\\.");
        System.out.println(strings);
    }

    public List<String> getDependencyPaths() {
        List<String> jarClassPaths = new ArrayList<>();
        dependencyFiles.forEach(d -> {
            String s = null;
            try {
                s = IoUtil.readFile(d);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (s != null) {
                String[] lines = s.split("\r\n");
                if(lines.length<3){
                    return;
                }
                for (int i = 2; i < lines.length; i++) {
                    set.add(lines[i].trim());
                }
            }
        });

        for (String element : set) {
            if ("none".equals(element)) {
                continue;
            }
            String[] strings = element.split(":");
            String groupId = strings[0];
            String artifactid="";
            try{
                artifactid = strings[1];
            }catch (Exception e){
                e.printStackTrace();
            }
            String version = "";
            String jdk = "";
            if (strings.length == 6) {
                jdk = strings[3];
                version = strings[4];
            } else if (strings.length == 5) {
                version = strings[3];
            } else {
                throw new RuntimeException("unhanded maven dependency format");
            }
            jarClassPaths.add(repoPath + "\\" + Joiner.on("\\").join(Arrays.asList(groupId.split("\\."))) + "\\" + artifactid + "\\" + version + "\\" + artifactid + "-" + version + (RandomValue.blank(jdk) ? "" : "-" + jdk)+".jar");
        }
        return jarClassPaths;
    }

}
