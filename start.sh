#!/bin/bash

echo "=========================================="
echo "  FastGPT 文档解析服务 - 启动脚本"
echo "=========================================="
echo ""

# 检查 Java 版本
echo "检查 Java 环境..."
if ! command -v java &> /dev/null; then
    echo "❌ 未找到 Java，请先安装 JDK 17+"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}')
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java 版本过低，需要 JDK 17+，当前版本：$(java -version 2>&1 | head -n 1)"
    exit 1
fi

echo "✅ Java 版本检查通过"
echo ""

# 检查配置文件
echo "检查配置文件..."
if [ ! -f "src/main/resources/application.yml" ]; then
    echo "❌ 未找到配置文件 application.yml"
    echo "   请复制 application-example.yml 为 application.yml 并填入配置信息"
    echo "   cp src/main/resources/application-example.yml src/main/resources/application.yml"
    exit 1
fi

echo "✅ 配置文件检查通过"
echo ""

# 检查 Maven
echo "检查 Maven 环境..."
if ! command -v mvn &> /dev/null; then
    echo "❌ 未找到 Maven，请先安装 Maven 3.8+"
    exit 1
fi

echo "✅ Maven 环境检查通过"
echo ""

# 编译项目
echo "编译项目..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ 编译失败，请检查错误信息"
    exit 1
fi

echo "✅ 编译成功"
echo ""

# 启动服务
echo "启动服务..."
echo ""
java -jar target/docparser-1.0.0.jar
