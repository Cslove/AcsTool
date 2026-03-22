# gradlew & gradlew.bat 配置详解

## 📋 文件概述

`gradlew` (Linux/Mac) 和 `gradlew.bat` (Windows) 是 Gradle Wrapper 的**可执行脚本**,用于在本地环境中启动 Gradle 构建过程。它们确保项目使用正确版本的 Gradle,无需手动安装。

---

## 🎯 核心作用

### 1. 版本锁定与一致性

#### 作用
确保所有开发者使用**完全相同版本**的 Gradle 进行构建，消除"在我机器上能编译"的问题。

#### 实际场景对比

**❌ 没有 Wrapper 时:**
```
团队成员环境:
👤 张三：Gradle 7.0 ✅ 构建成功
👤 李四：Gradle 8.0 ❌ 报错:API 不兼容
👤 王五：没装 Gradle ❌ 无法构建

痛苦流程:
1. 张三:"我用了 Gradle 7.0 的新 API"
2. 李四:"那我升级到 8.0" → 其他同事报错
3. 王五:"我要先装 Gradle...装哪个版本？"
4. CI/CD 服务器：又装了另一个版本
5. 结果：每个人构建结果不一致
```

**✅ 有 Wrapper 时:**
```
项目配置:
gradle/wrapper/gradle-wrapper.properties:
  distributionUrl=https\://services.gradle.org/distributions/gradle-8.0-bin.zip

执行流程:
./gradlew assembleDebug
├─ 检查本地是否有 Gradle 8.0
├─ 没有？→ 自动下载 Gradle 8.0
├─ 解压到 ~/.gradle/wrapper/dists/
├─ 使用 Gradle 8.0 执行构建
└─ ✅ 所有人使用相同版本

结果:
✅ 张三、李四、王五都用 Gradle 8.0
✅ CI/CD 也用 Gradle 8.0
✅ 构建结果一致
```

---

### 2. 零配置运行

#### 作用
新成员加入项目时，无需手动安装 Gradle，开箱即用。

#### 实际场景：新成员入职

**❌ 传统方式:**
```
👤 新成员小王入职第一天:

步骤 1:搜索"如何安装 Gradle"
步骤 2:打开 Gradle 官网
步骤 3:下载 gradle-8.0-bin.zip
步骤 4:解压到 /opt/gradle
步骤 5:编辑 ~/.zshrc
  export GRADLE_HOME=/opt/gradle
  export PATH=$GRADLE_HOME/bin:$PATH
步骤 6:source ~/.zshrc
步骤 7:gradle -v 验证
步骤 8:git clone 项目
步骤 9:cd 项目
步骤 10:gradle build → ❌ 版本不对！项目需要 7.5

总耗时:30 分钟
```

**✅ Wrapper 方式:**
```
👤 新成员小王入职第一天:

步骤 1:git clone 项目
步骤 2:cd 项目
步骤 3:./gradlew build
  ├─ 自动检测 Gradle 版本
  ├─ 自动下载对应版本
  └─ 自动执行构建

总耗时:3 分钟(大部分时间在下载)
```

---

## 🔧 工作原理

### 执行流程图解

```
用户执行：./gradlew assembleDebug
│
├─ 第 1 步：读取 gradlew 脚本
│   └─ 确定 Gradle 安装位置
│
├─ 第 2 步：检查 gradle/wrapper/gradle-wrapper.properties
│   └─ 读取 distributionUrl
│      └─ https://services.gradle.org/distributions/gradle-8.0-bin.zip
│
├─ 第 3 步：检查本地缓存
│   ├─ 路径:~/.gradle/wrapper/dists/gradle-8.0-bin/
│   ├─ 已存在？→ 直接使用
│   └─ 不存在？→ 执行第 4 步
│
├─ 第 4 步：下载 Gradle 发行版
│   ├─ 下载 gradle-8.0-bin.zip
│   ├─ 解压到缓存目录
│   └─ 标记为已安装
│
├─ 第 5 步：启动 Gradle Daemon
│   ├─ 设置 JAVA_HOME(如果有 foojay 插件)
│   ├─ 应用 JVM 参数(来自 gradle.properties)
│   └─ 启动 Gradle 守护进程
│
├─ 第 6 步：执行构建任务
│   ├─ 加载 settings.gradle.kts
│   ├─ 加载 build.gradle.kts
│   ├─ 解析依赖
│   ├─ 执行任务链
│   └─ 输出构建结果
│
└─ 第 7 步：返回退出码
    ├─ 成功 → exit 0
    └─ 失败 → exit 1
```

---

## 📁 文件结构分析

### gradlew (Linux/Mac)

```bash
#!/bin/sh
# POSIX 兼容的 shell 脚本

# 关键变量
APP_HOME=$( cd -P "${APP_HOME:-./}" > /dev/null && printf '%s\n' "$PWD" )
CLASSPATH=""
JAVACMD=java  # 或使用 JAVA_HOME 指定的 java

# 默认 JVM 参数
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# 执行 Gradle
exec "$JAVACMD" \
  -Dorg.gradle.appname=$APP_BASE_NAME \
  -classpath "$CLASSPATH" \
  -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
  "$@"
```

**关键点**:
- 最终调用 `gradle-wrapper.jar`(真正的引导程序)
- 传递所有命令行参数(`"$@"`)
- 使用 `-jar` 模式运行 JAR 文件

### gradlew.bat (Windows)

```batch
@echo off
REM Windows 批处理脚本

REM 设置变量
set APP_HOME=%DIRNAME%
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

REM 查找 Java
if defined JAVA_HOME goto findJavaFromJavaHome
set JAVA_EXE=java.exe
if exist "%JAVA_EXE%" goto execute

REM 执行 Gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% ^
  -Dorg.gradle.appname=%APP_BASE_NAME% ^
  -classpath "%CLASSPATH%" ^
  -jar "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" ^
  %*
```

**关键点**:
- Windows 路径处理 (反斜杠)
- 使用 `%*` 传递所有参数
- `.bat` 扩展名确保 CMD 识别

---

## 🎯 实际应用场景

### 场景 1:升级 Gradle 版本

**需求**: 项目需要从 Gradle 8.0 升级到 8.5

**操作步骤**:

```bash
# 方法 1:手动修改配置文件
# 编辑 gradle/wrapper/gradle-wrapper.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip

# 方法 2:使用 Gradle 命令 (推荐)
./gradlew wrapper --gradle-version 8.5

# 验证升级
./gradlew --version
```

**执行流程**:
```
1. 修改配置文件
2. 下次运行 ./gradlew 时:
   ├─ 检测到新版本 8.5
   ├─ 自动下载 gradle-8.5-bin.zip
   ├─ 解压到缓存目录
   └─ 使用新版本构建
3. 团队其他成员 pull 代码后:
   ├─ 自动下载 8.5
   └─ 无缝切换
```

**状态同步**:
```
Git 提交记录:
commit abc123
  gradle/wrapper/gradle-wrapper.properties | 2 +-
  1 file changed, 1 insertion(+), 1 deletion(-)

团队成员执行:
git pull
./gradlew build  # 自动使用新版本
```

---

### 场景 2:离线构建

**背景**: 公司内网无法访问外网

**问题**:
```
❌ 首次运行 ./gradlew build
   下载 gradle-wrapper.jar → 失败
   下载 gradle-8.0-bin.zip → 失败
```

**解决方案**:

**方案 A:提前缓存**
```bash
# 在有网的机器上执行
./gradlew build  # 下载所有依赖

# 复制缓存目录
cp -r ~/.gradle/wrapper /shared/internal-mirror/

# 在内网机器上配置
export GRADLE_USER_HOME=/shared/internal-mirror/.gradle
./gradlew build  # 使用本地缓存
```

**方案 B:配置镜像**
```properties
# gradle/wrapper/gradle-wrapper.properties
distributionUrl=https\://mirrors.cloud.tencent.com/distributions/gradle-8.0-bin.zip
```

---

### 场景 3:CI/CD集成

**GitHub Actions 示例**:

```yaml
name: Android CI

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
    
    - name: Make gradlew executable
      run: chmod +x ./gradlew
    
    - name: Build with Gradle
      run: ./gradlew build
      
    - name: Run tests
      run: ./gradlew test
```

**执行流程**:
```
GitHub Actions Runner:
├─ checkout 代码 (包含 gradlew)
├─ 安装 JDK 17
├─ chmod +x ./gradlew(赋予执行权限)
├─ ./gradlew build
│   ├─ 检测 Gradle 版本
│   ├─ 下载 Gradle 8.0
│   └─ 执行构建
└─ ✅ 构建成功
```

**优势**:
- ✅ 不需要在 CI 环境预装 Gradle
- ✅ 版本与本地开发一致
- ✅ 配置简单，只需 JDK

---

### 场景 4:多版本 Gradle 管理

**问题**: 同时维护多个项目，每个项目 Gradle 版本不同

```
项目 A:Gradle 7.0
项目 B:Gradle 8.0
项目 C:Gradle 8.5
```

**错误做法**:
```bash
# 全局安装一个版本
export GRADLE_HOME=/opt/gradle-8.0

# 切换到项目 A
cd project-a
gradle build  # ❌ 版本不对!

# 手动切换
export GRADLE_HOME=/opt/gradle-7.0
cd project-a
gradle build  # ✅

# 切换到项目 B
cd project-b
gradle build  # ❌ 又要改!
```

**正确做法**:
```bash
# 每个项目都有自己的 gradlew
cd project-a
./gradlew build  # ✅ 自动使用 Gradle 7.0

cd project-b
./gradlew build  # ✅ 自动使用 Gradle 8.0

cd project-c
./gradlew build  # ✅ 自动使用 Gradle 8.5
```

**底层机制**:
```
每个项目的 gradle/wrapper/gradle-wrapper.properties 独立配置:

project-a:
  distributionUrl=.../gradle-7.0-bin.zip
  ↓
  使用 ~/.gradle/wrapper/dists/gradle-7.0-bin/

project-b:
  distributionUrl=.../gradle-8.0-bin.zip
  ↓
  使用 ~/.gradle/wrapper/dists/gradle-8.0-bin/

project-c:
  distributionUrl=.../gradle-8.5-bin.zip
  ↓
  使用 ~/.gradle/wrapper/dists/gradle-8.5-bin/

结果：互不干扰，自动切换
```

---

## 🔍 常见问题排查

### Q1: gradlew 权限不足

**现象**:
```
bash: ./gradlew: Permission denied
```

**解决方案**:
```bash
chmod +x ./gradlew
./gradlew build
```

**根本原因**:
- Git 在 Windows 上 clone 的代码
- Mac/Linux 系统需要执行权限
- `gradlew` 是文本文件，不是二进制

---

### Q2: Wrapper 下载失败

**现象**:
```
Could not download gradle-8.0-bin.zip
```

**排查步骤**:
1. 检查网络连接
2. 检查防火墙设置
3. 配置代理或镜像

**解决方案**:
```properties
# 方案 1:配置 HTTP 代理
# gradle.properties
systemProp.http.proxyHost=proxy.company.com
systemProp.http.proxyPort=8080

# 方案 2:使用国内镜像
# gradle/wrapper/gradle-wrapper.properties
distributionUrl=https\://mirrors.cloud.tencent.com/distributions/gradle-8.0-bin.zip

# 方案 3:手动下载并放置
# 下载 gradle-8.0-bin.zip
# 移动到 ~/.gradle/wrapper/dists/gradle-8.0-bin/
```

---

### Q3: 版本冲突警告

**现象**:
```
Warning: Gradle 8.0 is incompatible with AGP 7.0
```

**解决方案**:
```kotlin
// 查看当前 AGP 版本
// app/build.gradle.kts
plugins {
    id("com.android.application") version "8.1.0"  // ← 查看版本
}

// 访问 https://developer.android.com/build/releases/gradle-plugin
// 查找兼容的 Gradle 版本

// 升级 Wrapper
./gradlew wrapper --gradle-version 8.0
```

**兼容性对照表**:
| Android Gradle Plugin | Gradle 版本 |
|-----------------------|-------------|
| 8.1.x                 | 8.0+        |
| 8.0.x                 | 8.0+        |
| 7.4.x                 | 7.5+        |
| 7.3.x                 | 7.4+        |

---

## 💡 最佳实践

### 1. 将 gradlew 纳入版本控制

```bash
# ✅ 推荐：提交所有 wrapper 文件
git add gradlew
git add gradlew.bat
git add gradle/wrapper/gradle-wrapper.jar
git add gradle/wrapper/gradle-wrapper.properties

# ❌ 不推荐：忽略 wrapper 文件
# .gitignore
gradlew          # ← 不要忽略!
gradlew.bat      # ← 不要忽略!
gradle/wrapper/  # ← 不要忽略!
```

**原因**:
- ✅ 确保团队成员使用相同版本
- ✅ CI/CD 可以直接运行
- ✅ 新项目可以快速初始化

---

### 2. 定期升级 Gradle 版本

**升级流程**:
```bash
# 1. 查看当前版本
./gradlew --version

# 2. 查看最新版本
# 访问 https://gradle.org/releases/

# 3. 升级 wrapper
./gradlew wrapper --gradle-version 8.5

# 4. 验证升级
./gradlew --version

# 5. 运行测试
./gradlew test

# 6. 提交变更
git add gradle/wrapper/gradle-wrapper.properties
git commit -m "Upgrade Gradle to 8.5"
```

**注意事项**:
- 查看 Release Notes，了解破坏性变更
- 先在分支上测试，再合并到主分支
- 通知团队成员更新

---

### 3. 配置 Gradle 缓存目录

```bash
# 默认缓存位置
~/.gradle/wrapper/dists/     # Gradle 发行版
~/.gradle/caches/            # 依赖缓存

# 自定义缓存位置 (磁盘空间不足时)
export GRADLE_USER_HOME=/Volumes/ExternalDrive/.gradle

# 或在 gradle.properties 中配置
org.gradle.caching=true
org.gradle.daemon=true
```

---

## 📊 性能优化

### 1. 启用 Gradle Daemon

```properties
# gradle.properties
org.gradle.daemon=true
```

**效果**:
```
首次构建:
./gradlew build  # 启动 Daemon，较慢 (~30s)

第二次构建:
./gradlew build  # 复用 Daemon，快速 (~5s)

原理:
- Daemon 是后台进程
- 保持 JVM 常驻内存
- 避免每次重新启动
```

---

### 2. 配置构建缓存

```properties
# gradle.properties
org.gradle.caching=true
org.gradle.parallel=true
```

**效果**:
```
任务缓存机制:
├─ 检查输入是否变化
├─ 未变化？→ 使用缓存结果
└─ 已变化？→ 重新执行

示例:
第一次:./gradlew assembleDebug  # 全量编译 (120s)
第二次:./gradlew assembleDebug  # 增量编译 (15s)
         ↑ 只编译变化的模块
```

---

### 3. JVM 内存优化

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError
```

**参数说明**:
| 参数 | 作用 | 推荐值 |
|------|------|--------|
| `-Xmx` | 最大堆内存 | 4g (大项目) |
| `-XX:MaxMetaspaceSize` | 元空间大小 | 1g |
| `-XX:+HeapDumpOnOutOfMemoryError` | OOM 时导出堆 | 调试用 |

---

## 🔄 与其他文件的关系

### 与 gradle.properties 的配合

```
执行 ./gradlew build 时:
├─ 读取 gradlew 脚本
├─ 读取 gradle-wrapper.properties (确定 Gradle 版本)
├─ 读取 gradle.properties (获取 JVM 参数)
│   └─ org.gradle.jvmargs=-Xmx2048m
├─ 启动 Gradle Daemon
└─ 执行构建
```

### 与 settings.gradle.kts 的配合

```
Gradle 启动流程:
1. gradlew 启动 Gradle
2. Gradle 读取 settings.gradle.kts
   ├─ 配置 pluginManagement.repositories
   ├─ 配置 dependencyResolutionManagement
   └─ 加载项目结构
3. Gradle 读取 build.gradle.kts
4. 执行构建
```

---

## 📚 总结对比表

| 特性 | 有 Wrapper | 无 Wrapper |
|------|-----------|-----------|
| 版本一致性 | ✅ 自动保证 | ❌ 依赖人工 |
| 新成员上手 | ✅ 开箱即用 | ❌ 需要安装 |
| CI/CD配置 | ✅ 简单 | ❌ 复杂 |
| 多项目管理 | ✅ 自动隔离 | ❌ 容易冲突 |
| 离线构建 | ⚠️ 需提前缓存 | ⚠️ 需手动配置 |

---

## 📖 扩展阅读

- [Gradle Wrapper 官方文档](https://docs.gradle.org/current/userguide/gradle_wrapper.html)
- [Gradle 版本发布](https://gradle.org/releases/)
- [Android Gradle 插件兼容性](https://developer.android.com/build/releases/gradle-plugin)

---

**文档维护者**: AcsTool Team  
**最后更新**: 2024-03-22