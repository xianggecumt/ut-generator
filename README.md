# Unit Test Generation Tool

单元测试生成工具开源项目


## 架构

![Architecture](architecture.png)

## 特性

* 自动生成被测类所有声明方法的测试代码
* 自动生成被测类依赖的Mock代码，并与测试代码分离
* 支持增量（新增类和方法）生成
* 简单易用，生成速度快

## 源代码结构

* core子包下为UT插件的核心逻辑，包括项目分析、依赖分析和代码生成。
* ui子包下为idea插件执行时的对话框界面和交互式mock界面的代码。
* ut包下为idea插件执行的控制逻辑

## 功能说明
+ 单元测试代码自动生成：自动生成被测类的所有方法的Junit4测试代码骨架，并提供默认的测试数据。
+ Mock代码自动生成：能够提供可视化的界面展示被测类的所有依赖，根据用户选择去生成对应的mock代码，mock框架采用Jmockit。

## 安装使用说明
1. 用IDEA打开项目，执行项目构建操作，即可以在项目跟目录下得到插件UTGenerator.zip安装包
2. 在IDEA的插件管理中安装上一步得到的UTGenerator.zip即可
3. 在Project或Module的任意目录或文件上右击，选中第二项的Generate UT，弹出对话框，选择OK后开始自动生成代码
