package com.github.xg.utgen.core.util;


import java.io.*;

/**
 * Created by yuxiangshi on 2017/8/9.
 */
public class IoUtil {

    public static String readFile(String path) {
        StringBuilder sb = new StringBuilder();
        File file = new File(path);
        InputStreamReader ir = null;
        BufferedReader br = null;
        try {
            ir = new InputStreamReader(new FileInputStream(file), "UTF-8");
            br = new BufferedReader(ir);
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\r\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ir != null) {
                try {
                    ir.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();
    }

    public static void writeFile(String fileName, String content) {
        if (fileName.contains("\\")) {
            File f = new File(fileName.substring(0, fileName.lastIndexOf("\\")));
            f.mkdirs();
        }
        File f = new File(fileName);

        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        OutputStreamWriter osw = null;
        BufferedWriter bw = null;
        try {
            osw = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
            bw = new BufferedWriter(osw);
            bw.write(content);
            bw.flush();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (osw != null) {
                try {
                    osw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

}
