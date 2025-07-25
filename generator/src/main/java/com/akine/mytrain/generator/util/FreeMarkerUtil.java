package com.akine.mytrain.generator.util;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class FreeMarkerUtil {
    static String ftlPath = "generator\\src\\main\\java\\com\\akine\\mytrain\\generator\\ftl";

    static Template temp;

    /**
     * 读模版
     */
    public static void initConfig(String ftlName) throws Exception{
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setDirectoryForTemplateLoading(new File(ftlPath));
        cfg.setObjectWrapper(new DefaultObjectWrapper(Configuration.VERSION_2_3_32));
        temp = cfg.getTemplate(ftlName);
    }

    /**
     * 根据模板，生成文件
     */
    public static void generator(String fileName, Map<String, Object> map) throws IOException, TemplateException {
        FileWriter fw = new FileWriter(fileName);
        BufferedWriter bw = new BufferedWriter(fw);
        temp.process(map, bw);
        bw.flush();
        fw.close();
    }

}
