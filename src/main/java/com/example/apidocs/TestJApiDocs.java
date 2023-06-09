package com.example.apidocs;

import io.github.yedaxia.apidocs.Docs;
import io.github.yedaxia.apidocs.DocsConfig;
import io.github.yedaxia.apidocs.plugin.markdown.MarkdownDocPlugin;

import java.util.Properties;

/**
 * @Description:
 * @ClassName: TestJApiDocs
 * @Author: 刘苏义
 * @Date: 2023年06月09日8:49
 * @Version: 1.0
 **/
public class TestJApiDocs {
    public static void main(String[] args) {

        DocsConfig config = new DocsConfig();// 获取项目路径
        Properties props = System.getProperties(); //获得系统属性集
        String userDir = props.getProperty("user.dir");    // 用户的当前工作目录
        config.setProjectPath(userDir); // 项目根目录
        config.setProjectName("demo20221203-springboot-mybatis"); // 项目名称
        config.setApiVersion("V1.0"); // 声明该API的版本
        config.setDocsPath(userDir + "/src/main/resources/static/apidoc"); // 生成API 文档所在目录
        config.setAutoGenerate(Boolean.TRUE); // 配置自动生成
        config.addPlugin(new MarkdownDocPlugin()); // 使用 MD 插件，额外生成 MD 格式的接口文档
        Docs.buildHtmlDocs(config); // 执行生成文档
    }
}
