package com.github.xg.utgen.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static com.github.xg.utgen.core.util.Formats.RT_1;
/**
 * Created by yuxiangshi on 2017/9/12.
 */
public class ImportCodeHelper {

    private String code;

    private String importCode;

    public ImportCodeHelper(final String code) {
        this.code = code;
        //wrong
        importCode = code.substring(0,code.indexOf('{'));
    }

    public static String toString(List<String> importClasses,String oldContent){
        StringBuilder sb = new StringBuilder();
        String[] imports = new String[importClasses.size()];
        imports = importClasses.toArray(imports);
        Arrays.sort(imports);
        for (int i = 0; i < imports.length; i++) {
            String s = imports[i];
            if(oldContent.contains(s+";")){
                continue;
            }
            sb.append("import " + s + ";" + RT_1);
        }
        return sb.toString();
    }


    public List<String> getImports(){
        List<String> list = new ArrayList<>();
        String[] strings = importCode.split(";");
        for (int i = 0; i < strings.length; i++) {
            if(strings[i].trim().contains("import")){
                String importString = strings[i].substring(strings[i].indexOf("import")+6).trim();
                if(importString.contains("static")){

                }
                list.add(importString);
            }
        }
        return list;
    }

    public String addImport(List<String> imports){
        String packageCode = "";
        int i = importCode.indexOf("package");
        if(i!=-1){
            for (int j = i; j < importCode.length(); j++) {
                if(importCode.charAt(j)==';'){
                    packageCode=importCode.substring(i,j);
                }
            }
        }

        final List<String> orignImports = getImports();
        List<String> mergeImports = new ArrayList<>(orignImports);

        for (String s : imports) {
            if(!orignImports.contains(s)){
                mergeImports.add(s);
            }
        }

        String body = "";
        for (int j = code.lastIndexOf("import"); j < code.length(); j++) {
            if(code.charAt(j)==';'){
                body = code.substring(j+1);
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(packageCode+";"+ RT_1);
        for (int j = 0; j < mergeImports.size(); j++) {
            sb.append("import "+mergeImports.get(j)+";"+RT_1);
        }
        sb.append(body);
        return sb.toString();
    }

}
