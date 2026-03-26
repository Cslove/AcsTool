# settings.gradle.kts 配置详解

## 前端类比

`settings.gradle.kts` 类似于前端的 `tsconfig.json` 或 `package.json` 的顶层配置，定义项目结构和仓库。

## 核心作用

### 1. 定义项目结构

```kotlin
rootProject.name = "AcsTool"
include(":app")
include(":lib-common")
include(":lib-network")
```

**效果**：
```
AcsTool/
├── app/
├── lib-common/
└── lib-network/
```

### 2. 配置仓库

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

**对比**：
```kotlin
// ❌ 旧写法（已废弃）
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

// ✅ 新写法（推荐）
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

## 配置详解

### 项目名称

```kotlin
rootProject.name = "AcsTool"
```

**作用**：定义项目根目录名称

### 包含模块

```kotlin
include(":app")
include(":lib-common")

// 嵌套模块
include(":feature:login")
include(":feature:profile")
```

**效果**：
```
AcsTool/
├── app/
├── lib-common/
└── feature/
    ├── login/
    └── profile/
```

### 仓库配置

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }
}
```

**仓库说明**：

| 仓库 | 内容 |
|------|------|
| `google()` | Android 官方库 |
| `mavenCentral()` | Java/Kotlin 通用库 |
| `jitpack.io` | GitHub 项目库 |

### 插件管理

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

## 国内镜像

```kotlin
dependencyResolutionManagement {
    repositories {
        // 阿里云镜像
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
        }
    }
}
```

**速度对比**：
```
官方仓库: 120s
阿里云镜像: 30s
```

## 最佳实践

### 1. 统一仓库配置

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
```

**作用**：禁止模块单独配置仓库，确保一致性

### 2. 使用国内镜像

```kotlin
repositories {
    maven { url = uri("https://maven.aliyun.com/repository/google") }
    maven { url = uri("https://maven.aliyun.com/repository/public") }
}
```

### 3. 合理组织模块

```kotlin
include(":app")
include(":lib:common")
include(":lib:network")
include(":feature:login")
include(":feature:profile")
```

## 常见问题

### Q1: 依赖下载慢

使用国内镜像或配置代理。

### Q2: 模块找不到

检查 `include` 配置是否正确。

### Q3: 仓库冲突

确保使用 `FAIL_ON_PROJECT_REPOS` 模式。

## 扩展阅读

- [Gradle 设置文档](https://docs.gradle.org/current/userguide/declaring_repositories.html)
- [Android 构建配置](https://developer.android.com/build)
