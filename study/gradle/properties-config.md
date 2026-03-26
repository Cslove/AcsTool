# gradle.properties 配置详解

## 前端类比

`gradle.properties` 类似于前端的 `.env` 文件，用于配置全局环境变量和构建参数。

## 文件位置

- **项目级** `gradle.properties` - 项目特定配置
- **用户级** `~/.gradle/gradle.properties` - 全局用户配置

## 核心配置项

### 1. JVM 参数配置

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
```

| 参数 | 作用 | 推荐值 |
|------|------|--------|
| `-Xmx2048m` | 最大堆内存 | 2048m-4096m |
| `-Xms512m` | 初始堆内存 | 512m-1024m |
| `-Dfile.encoding` | 文件编码 | UTF-8 |

**实际效果**：
```
小内存配置 (-Xmx512m):
├─ 小型项目 ✅
├─ 大型项目 ❌ OOM

大内存配置 (-Xmx2048m):
├─ 小型项目 ✅
├─ 大型项目 ✅
```

### 2. 守护进程

```properties
org.gradle.daemon=true
```

**作用**：避免每次构建都启动 JVM，显著提升速度

**对比**：
```
禁用 Daemon:
第一次构建: 68s (启动 JVM 8s + 构建 60s)
第二次构建: 23s (启动 JVM 8s + 构建 15s)

启用 Daemon:
第一次构建: 68s (启动 JVM 8s + 构建 60s)
第二次构建: 15s (复用 Daemon)
```

### 3. 并行构建

```properties
org.gradle.parallel=true
```

**作用**：多模块项目并行执行任务

**效果**：
```
串行构建: 65s
并行构建: ~30s (加速比 2.2x)
```

### 4. 构建缓存

```properties
org.gradle.caching=true
```

**效果**：
```
第一次构建: 120s (全量编译)
第二次构建: 45s (从缓存恢复)
```

## 网络配置

### HTTP 代理

```properties
systemProp.http.proxyHost=proxy.company.com
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=proxy.company.com
systemProp.https.proxyPort=8080
```

### Maven 镜像

在 `settings.gradle.kts` 中配置：

```kotlin
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
        }
    }
}
```

**速度对比**：
```
Maven Central: 120s
阿里云镜像: 30s (加速比 4x)
```

## 配置模板

### 开发环境

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
kotlin.code.style=official
android.useAndroidX=true
```

### CI/CD 环境

```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
org.gradle.daemon=false
org.gradle.parallel=true
org.gradle.logging.level=warn
```

### 低配电脑

```properties
org.gradle.jvmargs=-Xmx1024m -Dfile.encoding=UTF-8
org.gradle.parallel=false
org.gradle.daemon=true
```

## 配置优先级

```
1. IDE 设置 (最高)
2. 项目级 gradle.properties
3. 用户级 gradle.properties
4. Gradle 安装目录 (最低)
```

## 常见问题

### Q1: 修改配置不生效

```bash
./gradlew --stop
```

### Q2: 内存溢出

```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m
```

### Q3: 代理配置无效

```bash
# 测试代理
curl -x proxy.company.com:8080 https://repo.maven.apache.org
```

## 最佳实践

1. **提交项目级配置**，忽略用户级配置
2. **根据项目规模调整内存**
3. **启用 Daemon 和并行构建**提升速度

## 扩展阅读

- [Gradle 构建环境](https://docs.gradle.org/current/userguide/build_environment.html)
- [Gradle 性能优化](https://docs.gradle.org/current/userguide/performance.html)
