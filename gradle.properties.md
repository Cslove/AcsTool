# gradle.properties 配置详解

## 📋 文件概述

`gradle.properties` 是 Gradle 的**全局配置文件**,用于设置 JVM 参数、启用守护进程、配置代理等影响构建行为的关键选项。这些配置会应用到所有 Gradle 构建任务中。

---

## 🎯 核心配置项

### 1. org.gradle.jvmargs - JVM 参数配置

#### 作用
设置 Gradle 守护进程的 JVM 启动参数，直接影响构建性能和稳定性。

#### 实际场景对比

**❌ 默认配置 (小内存):**
```properties
# 没有配置或默认值
org.gradle.jvmargs=-Xmx512m
```

**实际效果**:
```
小型项目 (< 5 个模块):
├─ 编译 Kotlin 代码
├─ 内存使用:400MB
└─ ✅ 构建成功

中型项目 (10-20 个模块):
├─ 编译 Kotlin + 处理资源
├─ 内存使用:600MB
├─ ❌ Java heap space OutOfMemoryError
└─ ❌ 构建失败

大型项目 (> 50 个模块):
├─ 编译 Kotlin + 处理资源 + 打包
├─ 内存使用:512MB
├─ ❌ GC 频繁触发
├─ ❌ 构建极慢 (大量时间花在 GC)
└─ ❌ 可能 OOM
```

**✅ 优化配置 (充足内存):**
```properties
# 推荐配置
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
```

**实际效果**:
```
中型项目 (10-20 个模块):
├─ 编译 Kotlin 代码
├─ 内存使用:1.2GB
├─ GC 次数少
└─ ✅ 快速构建成功

大型项目 (> 50 个模块):
├─ 编译 Kotlin + 处理资源 + 打包
├─ 内存使用:1.8GB
├─ GC 正常
└─ ✅ 稳定构建成功
```

---

#### 参数详解

```properties
org.gradle.jvmargs=-Xmx2048m -Xms512m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8
```

| 参数 | 含义 | 推荐值 | 作用 |
|------|------|--------|------|
| `-Xmx2048m` | 最大堆内存 | 2048m-4096m | 防止 OOM |
| `-Xms512m` | 初始堆内存 | 512m-1024m | 减少动态扩容 |
| `-XX:MaxMetaspaceSize` | 最大元空间 | 512m-1024m | 存储类元数据 |
| `-Dfile.encoding` | 文件编码 | UTF-8 | 避免中文乱码 |

---

#### 场景：内存不足排查

**现象**:
```
> Task :app:compileDebugKotlin FAILED
Execution failed for task ':app:compileDebugKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner
   > Internal compiler error. See log for more details
     Caused by: java.lang.OutOfMemoryError: Java heap space
```

**排查步骤**:

```bash
# 1. 查看当前配置
cat gradle.properties

# 2. 监控内存使用
./gradlew build --profile

# 3. 生成 Heap Dump(如果 OOM)
org.gradle.jvmargs=-Xmx2048m -XX:+HeapDumpOnOutOfMemoryError

# 4. 调整参数
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m
```

**调优流程**:
```
初始配置:
org.gradle.jvmargs=-Xmx2048m

构建失败 (OOM):
↓
增加到:
org.gradle.jvmargs=-Xmx3072m

构建成功，但 GC 频繁:
↓
优化:
org.gradle.jvmargs=-Xmx4096m -Xms1024m

结果:
✅ 构建稳定
✅ 速度提升 30%
```

---

### 2. org.gradle.daemon - 守护进程

#### 作用
启用 Gradle 守护进程，避免每次构建都启动 JVM，显著提升构建速度。

#### 实际场景对比

**❌ 禁用 Daemon:**
```properties
org.gradle.daemon=false
```

**执行流程**:
```
第一次构建:
./gradlew assembleDebug
├─ 启动 JVM (5s)
├─ 加载 Gradle (3s)
├─ 执行构建 (60s)
└─ 总计:68s

第二次构建 (代码微调):
./gradlew assembleDebug
├─ 启动 JVM (5s)  ← 重复启动
├─ 加载 Gradle (3s)
├─ 执行构建 (15s)  ← 增量编译
└─ 总计:23s

第三次构建:
./gradlew assembleDebug
├─ 启动 JVM (5s)
├─ 加载 Gradle (3s)
├─ 执行构建 (10s)
└─ 总计:18s

问题:
⚠️ 每次都重新启动 JVM
⚠️ 浪费时间在启动上
⚠️ 无法利用缓存
```

**✅ 启用 Daemon:**
```properties
org.gradle.daemon=true  # 默认就是 true，可以不写
```

**执行流程**:
```
第一次构建:
./gradlew assembleDebug
├─ 启动 JVM (5s)
├─ 加载 Gradle (3s)
├─ 执行构建 (60s)
├─ Daemon 保持运行
└─ 总计:68s

第二次构建 (代码微调):
./gradlew assembleDebug
├─ 复用 Daemon (0s)  ← 无需重启
├─ 执行构建 (15s)
└─ 总计:15s

第三次构建:
./gradlew assembleDebug
├─ 复用 Daemon (0s)
├─ 执行构建 (10s)
└─ 总计:10s

优势:
✅ 后续构建速度提升 50-70%
✅ 利用构建缓存
✅ 保持编译上下文
```

---

#### Daemon 生命周期管理

```bash
# 查看 Daemon 状态
./gradlew --status

# 示例输出:
PID     STATUS  EXPIRATION
12345   IDLE    2024-03-22T20:00:00.000+0800
67890   STOPPED after running out of memory

# 停止所有 Daemon
./gradlew --stop

# 强制停止特定 Daemon
kill -9 12345
```

**Daemon 过期机制**:
```
默认情况:
├─ 3 小时无活动 → 自动停止
├─ 内存不足 → 自动回收
└─ Gradle 版本变化 → 创建新 Daemon

配置过期时间:
org.gradle.daemon.idletimeout=10800000  # 3 小时 (毫秒)
```

---

### 3. org.gradle.parallel - 并行构建

#### 作用
在多模块项目中，并行执行相互独立的构建任务，充分利用多核 CPU。

#### 实际场景

**项目结构**:
```
AcsTool/
├── app/
├── lib-common/
├── lib-network/
├── lib-database/
└── feature-login/
```

**❌ 串行构建:**
```properties
org.gradle.parallel=false
```

**执行流程**:
```
./gradlew assembleDebug
├─ 编译 lib-common (10s)
├─ 编译 lib-network (8s)
├─ 编译 lib-database (12s)
├─ 编译 feature-login (15s)
└─ 编译 app (20s)
   总计:65s

CPU 使用率:25% (单核满载)
```

**✅ 并行构建:**
```properties
org.gradle.parallel=true
```

**执行流程**:
```
./gradlew assembleDebug
├─ 编译 lib-common (10s) ─────┐
├─ 编译 lib-network (8s) ────┤
├─ 编译 lib-database (12s) ──┤ 并行执行
├─ 编译 feature-login (15s) ─┤
└─ 编译 app (20s) ───────────┘
   总计:~30s (取决于依赖关系)

CPU 使用率:80-100% (多核满载)
加速比:2.2x
```

---

#### 并行构建的限制

**依赖关系影响**:
```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":lib-common"))
    implementation(project(":lib-network"))  // 依赖 lib-common
    implementation(project(":lib-database"))
}

// lib-network/build.gradle.kts
dependencies {
    implementation(project(":lib-common"))  // 必须等 lib-common 先编译
}
```

**实际执行**:
```
第一阶段 (并行):
├─ lib-common (10s) ──┐
├─ lib-database (12s) ┤ 同时进行

第二阶段 (等待依赖):
└─ lib-network (8s)  必须等 lib-common 完成

第三阶段:
└─ app (20s)

总时间:10s + 8s + 20s = 38s
而非:10s + 8s + 12s + 20s = 50s
```

---

### 4. kotlin.code.style - Kotlin 代码风格

#### 作用
指定 Kotlin 代码格式化规则，确保团队代码风格一致。

#### 配置选项

```properties
# 官方推荐风格 (推荐)
kotlin.code.style=official

# 旧版风格 (不推荐)
kotlin.code.style=obsolete
```

**实际效果对比**:

**official 风格**:
```kotlin
// 数据类
data class User(
    val name: String,
    val age: Int
)

// 函数参数对齐
fun processUser(
    name: String,
    age: Int,
    email: String
) {
    // ...
}

// import 分组
import android.app.Activity
import android.os.Bundle

import androidx.compose.material3.Text

import com.example.acstool.R
```

**obsolete 风格**:
```kotlin
// 数据类 (紧凑格式)
data class User(val name: String, val age: Int)

// 函数参数 (单行)
fun processUser(name: String, age: Int, email: String) {
    // ...
}

// import(无分组)
import android.app.Activity
import android.os.Bundle
import androidx.compose.material3.Text
import com.example.acstool.R
```

---

## 🌐 网络相关配置

### 场景 1:配置 HTTP 代理

**背景**: 公司内网需要通过代理访问外网

```properties
# HTTP 代理
systemProp.http.proxyHost=proxy.company.com
systemProp.http.proxyPort=8080

# HTTPS 代理
systemProp.https.proxyHost=proxy.company.com
systemProp.https.proxyPort=8080

# 代理认证 (如果需要)
systemProp.http.proxyUser=zhangsan
systemProp.http.proxyPassword=password123

# 不使用代理的主机列表
systemProp.http.nonProxyHosts=localhost|127.0.0.1|*.company.com
```

**执行流程**:
```
需求：下载 androidx.core:core-ktx:1.12.0

无代理配置:
├─ 直接连接 repo.maven.apache.org
├─ ❌ 连接超时 (公司防火墙拦截)
└─ ❌ 构建失败

有代理配置:
├─ 通过 proxy.company.com:8080
├─ 代理服务器转发请求
├─ ✅ 下载成功
└─ ✅ 构建成功
```

---

### 场景 2:配置 Maven 镜像

**背景**: 国内访问 Maven Central 慢

```properties
# 使用阿里云镜像
# 在 settings.gradle.kts 中配置
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
        }
    }
}
```

**速度对比**:
```
直连 Maven Central(美国):
├─ 延迟:200-300ms
├─ 下载速度:50KB/s
└─ 首次构建:120s

阿里云镜像 (国内):
├─ 延迟:20-30ms
├─ 下载速度:5MB/s
└─ 首次构建:30s

加速比:4x
```

---

## 🔧 高级配置

### 1. 构建缓存优化

```properties
# 启用构建缓存
org.gradle.caching=true

# 配置缓存目录
org.gradle.caching.dir=/Volumes/SSD/.gradle/caches

# 缓存清理策略
org.gradle.caching.cleanup=true
```

**效果**:
```
第一次构建:
./gradlew assembleDebug  # 120s
├─ 编译所有模块
└─ 缓存任务结果

第二次构建 (删除 app/build):
./gradlew assembleDebug  # 45s
├─ 从缓存恢复 lib-common
├─ 从缓存恢复 lib-network
└─ 只编译 app 模块

第三次构建 (切换分支):
./gradlew assembleDebug  # 30s
├─ 大部分任务命中缓存
└─ 几乎瞬间完成
```

---

### 2. 配置扫描 (Build Scan)

```properties
# 启用 Build Scan
org.gradle.scan.enabled=true

# 自动上传 (可选)
org.gradle.scan.upload.enabled=true
```

**使用方式**:
```bash
# 执行构建并生成报告
./gradlew build --scan

# 输出:
Build Scan published:
https://scans.gradle.com/s/abc123def456
```

**报告内容**:
- ✅ 每个任务的执行时间
- ✅ 依赖树可视化
- ✅ 性能瓶颈分析
- ✅ 缓存命中率
- ✅ 可分享的 URL

---

### 3. 日志级别配置

```properties
# 默认日志级别
org.gradle.logging.level=lifecycle

# 可选值:error, warn, lifecycle, info, debug
```

**效果对比**:

**lifecycle(默认)**:
```
> Task :app:compileDebugKotlin
> Task :app:processDebugResources
> Task :app:packageDebug
BUILD SUCCESSFUL in 45s
```

**debug**:
```
Starting process 'Gradle build daemon'
Daemon started with PID 12345
Executing task ':app:compileDebugKotlin'
Input files: [/path/to/File.kt, ...]
Output files: [/path/to/File.class, ...]
Task ':app:compileDebugKotlin' completed in 3.2s
BUILD SUCCESSFUL in 45s
```

---

## 🎯 实际场景配置模板

### 1. 开发环境配置 (推荐)

```properties
# JVM 参数 (根据内存调整)
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8

# 启用守护进程
org.gradle.daemon=true

# 启用并行构建
org.gradle.parallel=true

# 启用构建缓存
org.gradle.caching=true

# Kotlin 代码风格
kotlin.code.style=official

# Android 特定配置
android.useAndroidX=true
android.enableJetifier=true
```

**适用场景**: 日常开发、调试

---

### 2. CI/CD配置

```properties
# CI 环境通常内存充足
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8

# 禁用 Daemon(CI 每次都是全新环境)
org.gradle.daemon=false

# 启用并行
org.gradle.parallel=true

# 启用缓存 (如果有共享缓存目录)
org.gradle.caching=true
org.gradle.caching.dir=/mnt/shared/gradle-cache

# 减少日志 (节省存储空间)
org.gradle.logging.level=warn
```

**适用场景**: Jenkins、GitHub Actions、GitLab CI

---

### 3. 低配电脑配置

```properties
# 限制内存使用
org.gradle.jvmargs=-Xmx1024m -Dfile.encoding=UTF-8

# 禁用并行 (减少 CPU 压力)
org.gradle.parallel=false

# 仍然启用 Daemon
org.gradle.daemon=true

# 禁用构建缓存 (节省磁盘空间)
org.gradle.caching=false
```

**适用场景**: 4GB 内存、双核 CPU

---

### 4. 企业内网配置

```properties
# JVM 参数
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8

# HTTP 代理
systemProp.http.proxyHost=proxy.company.com
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=proxy.company.com
systemProp.https.proxyPort=8080

# 私有仓库凭证
MAVEN_USERNAME=zhangsan
MAVEN_PASSWORD=your_password

# 跳过 SSL 验证 (仅内网，不推荐)
# systemProp.javax.net.ssl.trustStore=/path/to/truststore.jks
```

---

## 📊 配置文件位置优先级

### 多层级配置

```
配置来源 (优先级从高到低):

1. IDE 设置 (最高优先级)
   └─ Android Studio: Settings > Build > Gradle

2. 项目级 gradle.properties
   └─ /Users/eleme/AndroidStudioProjects/AcsTool/gradle.properties

3. 用户级 gradle.properties
   └─ ~/.gradle/gradle.properties

4. Gradle 安装目录 gradle.properties
   └─ $GRADLE_HOME/gradle.properties(最低优先级)
```

**实际案例**:

```properties
# ~/.gradle/gradle.properties(全局配置)
org.gradle.jvmargs=-Xmx2048m
org.gradle.daemon=true

# 项目级 gradle.properties(覆盖全局)
org.gradle.jvmargs=-Xmx4096m  # ← 这个项目需要更多内存
kotlin.code.style=official
```

**生效配置**:
```
JVM 参数:-Xmx4096m(项目级优先)
Daemon: true(继承全局)
Code Style: official(项目级独有)
```

---

## 🔍 常见问题排查

### Q1: 修改配置不生效

**现象**:
```
修改了 gradle.properties
再次构建，发现没变化
```

**原因**:
- Gradle Daemon 还在使用旧配置

**解决方案**:
```bash
# 停止 Daemon
./gradlew --stop

# 或者重启 Android Studio

# 验证配置
./gradlew properties | grep org.gradle.jvmargs
```

---

### Q2: 内存溢出

**现象**:
```
> Task :app:compileDebugKotlin FAILED
Execution failed for task ':app:compileDebugKotlin'.
> Internal compiler error. See log for more details
  Caused by: java.lang.OutOfMemoryError: Java heap space
```

**解决方案**:
```properties
# 逐步增加内存
org.gradle.jvmargs=-Xmx2048m  # ← 不够
org.gradle.jvmargs=-Xmx3072m  # ← 还不够
org.gradle.jvmargs=-Xmx4096m  # ✅ 够了

# 添加元空间
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m
```

---

### Q3: 代理配置无效

**现象**:
```
Could not resolve androidx.core:core-ktx:1.12.0
Could not GET 'https://repo.maven.apache.org/maven2/...'
Connect timed out
```

**排查**:
```bash
# 1. 测试代理连通性
curl -x proxy.company.com:8080 https://repo.maven.apache.org

# 2. 检查代理配置
cat gradle.properties | grep proxy

# 3. 尝试直接连接 (判断是否代理问题)
# 临时注释代理配置
# systemProp.http.proxyHost=proxy.company.com
```

---

## 💡 最佳实践

### 1. 版本控制策略

```bash
# ✅ 推荐：提交项目级 gradle.properties
git add gradle.properties

# ❌ 不推荐：提交用户级配置
# ~/.gradle/gradle.properties 不应该提交

# 例外：敏感信息不要提交
# MAVEN_PASSWORD=xxx  ← 不要提交!
```

---

### 2. 内存配置指南

```properties
# 根据项目规模配置

# 小型项目 (< 5 个模块)
org.gradle.jvmargs=-Xmx1024m

# 中型项目 (10-20 个模块)
org.gradle.jvmargs=-Xmx2048m

# 大型项目 (> 50 个模块)
org.gradle.jvmargs=-Xmx4096m

# 超大型项目 (Monorepo)
org.gradle.jvmargs=-Xmx8192m -XX:MaxMetaspaceSize=2048m
```

---

### 3. 性能调优 checklist

```properties
# ✅ 必选项
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
kotlin.code.style=official

# ⚠️ 可选项 (根据情况)
org.gradle.configureondemand=true  # 按需配置 (大型项目)
org.gradle.vfs.watch=true          # 文件系统监控
```

---

## 🔄 与其他文件的配合

### 与 gradlew 的配合

```
执行 ./gradlew build 时:
├─ gradlew 脚本启动
├─ 读取 gradle-wrapper.properties(确定 Gradle 版本)
├─ 读取 gradle.properties(获取 JVM 参数)
│   └─ org.gradle.jvmargs=-Xmx2048m
├─ 使用参数启动 JVM
│   └─ java -Xmx2048m -jar gradle-wrapper.jar
└─ 执行构建
```

### 与 settings.gradle.kts 的配合

```
settings.gradle.kts 配置仓库:
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

gradle.properties 配置代理:
systemProp.http.proxyHost=proxy.company.com

组合效果:
├─ 从 google() 下载依赖
├─ 通过代理服务器访问
└─ ✅ 下载成功
```

---

## 📚 总结对比表

| 配置项 | 默认值 | 推荐值 | 影响范围 |
|--------|--------|--------|----------|
| `org.gradle.jvmargs` | -Xmx512m | -Xmx2048m | 内存、稳定性 |
| `org.gradle.daemon` | true | true | 构建速度 |
| `org.gradle.parallel` | false | true | 多模块构建 |
| `org.gradle.caching` | false | true | 增量构建 |
| `kotlin.code.style` | obsolete | official | 代码格式 |

---

## 📖 扩展阅读

- [Gradle 官方文档 - 配置属性](https://docs.gradle.org/current/userguide/build_environment.html)
- [Gradle 性能优化指南](https://docs.gradle.org/current/userguide/performance.html)
- [Kotlin 编码规范](https://kotlinlang.org/docs/coding-conventions.html)

---

**文档维护者**: AcsTool Team  
**最后更新**: 2024-03-22