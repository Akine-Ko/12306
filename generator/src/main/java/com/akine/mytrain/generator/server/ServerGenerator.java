package com.akine.mytrain.generator.server;

import com.akine.mytrain.generator.util.DBUtil;
import com.akine.mytrain.generator.util.Field;
import com.akine.mytrain.generator.util.FreeMarkerUtil;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.*;

public class ServerGenerator {
    static String serverPath = "[module]/src/main/java/com/akine/mytrain/[module]/";
    static String pomPath = "generator\\pom.xml";

    public static void main(String[] args) throws Exception {

        //获取mybatis-generator
        String generatorPath = getGeneratorPath();

        String module = generatorPath.replace("src/main/resources/generator-config-", "").replace(".xml", "");
        System.out.println("module:" + module);
        serverPath = serverPath.replace("[module]", module);

        //读取table节点
        Document document = new SAXReader().read("generator/" + generatorPath);
        Node table = document.selectSingleNode("//table");
        System.out.println(table);
        Node tableName = table.selectSingleNode("@tableName");
        Node domainObjectName = table.selectSingleNode("@domainObjectName");
        System.out.println(tableName.getText() + "/" + domainObjectName.getText());

        //为DBUtil设置数据源
        Node connectionURL = document.selectSingleNode("//@connectionURL");
        Node userId = document.selectSingleNode("//@userId");
        Node password = document.selectSingleNode("//@password");
        System.out.println("url:" + connectionURL.getText());
        System.out.println("user:" + userId.getText());
        System.out.println("password:" + password.getText());
        DBUtil.url = connectionURL.getText();
        DBUtil.user = userId.getText();
        DBUtil.password = password.getText();


        String Domain = domainObjectName.getText();
        String domain = Domain.substring(0, 1).toLowerCase() + Domain.substring(1);
        //给url用
        String do_main = tableName.getText().replaceAll("_", "-");

        String tableNameCn = DBUtil.getTableComment(tableName.getText());
        List<Field> fieldList = DBUtil.getColumnByTableName(tableName.getText());
        Set<String> typeSet = getJavaTypes(fieldList);

        //组装参数
        Map<String, Object> param = new HashMap<>();
        param.put("Domain", Domain);
        param.put("domain", domain);
        param.put("do_main", do_main);
        param.put("tableNameCn", tableNameCn);
        param.put("typeSet", typeSet);
        param.put("fieldList", fieldList);
        System.out.println("组装参数:" + param);

        gen(Domain, param, "service", "service");
        gen(Domain, param, "controller", "controller");
        gen(Domain, param, "req", "saveReq");
    }

    private static void gen(String Domain, Map<String, Object> param, String packageName, String target) throws Exception {
        FreeMarkerUtil.initConfig(target + ".ftl");
        String toPath = serverPath + packageName + "/";
        new File(toPath).mkdirs();
        String Target = target.substring(0, 1).toUpperCase() + target.substring(1);
        String fileName = toPath + Domain + Target + ".java";
        System.out.println("开始生成:" + fileName);
        FreeMarkerUtil.generator(fileName, param);
    }

    private static String getGeneratorPath() throws DocumentException {
        SAXReader saxReader = new SAXReader();
        Map<String,String> map = new HashMap<>();
        map.put("pom", "http://maven.apache.org/POM/4.0.0");
        saxReader.getDocumentFactory().setXPathNamespaceURIs(map);
        Document document = saxReader.read(pomPath);
        Node node = document.selectSingleNode("//pom:configurationFile");
        System.out.println(node.getText());
        return node.getText();
    }

    /**
     * 获取所有的Java类型，使用Set去重
     */
    private static Set<String> getJavaTypes(List<Field> fieldList){

        Set<String> set = new HashSet<>();
        for (Field field : fieldList) {
            set.add(field.getJavaType());
        }

        return set;
    }

}
