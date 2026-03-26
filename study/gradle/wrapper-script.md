# gradlew 脚本详解

## 前端类比

`gradlew` 类似于前端的 `npm` 或 `yarn`，是项目的构建入口脚本。

## 文件说明

- `gradlew` - Linux/Mac 可执行脚本
- `gradlew.bat` - Windows 批处理脚本

## 核心作用

### 版本锁定

确保所有开发者使用**完全相同版本**的 Gradle。

**对比**：
```
没有 Wrapper:
张三: Gradle 7.0 ✅
李四: Gradle 8.0 ❌ API 不兼容
王五: 没装 Gradle ❌

有 Wrapper:
所有人: Gradle 8.0 ✅
```

## 工作原理

```
执行 ./gradlew build
│
├─ 读取 gradlew 脚本
├─ 检查 gradle-wrapper.properties
│   └─ distributionUrl=gradle-8.0-bin.zip
├─ 检查本地缓存
│   └─ ~/.gradle/wrapper/dists/
├─ 下载并解压 Gradle
├─ 启动 Gradle Daemon
└─ 执行构建
```

## 常用命令

### 基本命令

```bash
# 查看版本
./gradlew --version

# 构建项目
./gradlew build

# 清理构建
./gradlew clean

# 编译 Debug APK
./gradlew assembleDebug

# 编译 Release APK
./gradlew assembleRelease
```

### 升级 Gradle

```bash
# 方法 1: 使用命令
./gradlew wrapper --gradle-version 8.5

# 方法 2: 手动修改
# 编辑 gradle/wrapper/gradle-wrapper.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
```

### Daemon 管理

```bash
# 查看 Daemon 状态
./gradlew --status

# 停止所有 Daemon
./gradlew --stop
```

## CI/CD 集成

### GitHub Actions

```yaml
- name: Make gradlew executable
  run: chmod +x ./gradlew

- name: Build with Gradle
  run: ./gradlew build
```

**优势**：
- ✅ 无需预装 Gradle
- ✅ 版本与本地一致
- ✅ 配置简单

## 常见问题

### Q1: 权限不足

```bash
chmod +x ./gradlew
```

### Q2: 下载失败

```properties
# 配置代理
systemProp.http.proxyHost=proxy.company.com
systemProp.http.proxyPort=8080

# 或使用国内镜像
distributionUrl=https\://mirrors.cloud.tencent.com/distributions/gradle-8.0-bin.zip
```

### Q3: 版本冲突

查看 AGP 与 Gradle 兼容性：
- AGP 8.1.x 需要 Gradle 8.0+
- AGP 8.0.x 需要 Gradle 8.0+

## 最佳实践

### 1. 提交 Wrapper 文件

```bash
git add gradlew
git add gradlew.bat
git add gradle/wrapper/
```

### 2. 定期升级

```bash
./gradlew wrapper --gradle-version 8.5
./gradlew test
```

### 3. 配置缓存目录

```bash
export GRADLE_USER_HOME=/Volumes/ExternalDrive/.gradle
```

## 性能优化

### 启用 Daemon

```properties
# gradle.properties
org.gradle.daemon=true
```

### 配置缓存

```properties
org.gradle.caching=true
org.gradle.parallel=true
```

### JVM 内存

```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
```

## 扩展阅读

- [Gradle Wrapper 文档](https://docs.gradle.org/current/userguide/gradle_wrapper.html)
- [Gradle 版本发布](https://gradle.org/releases/)
