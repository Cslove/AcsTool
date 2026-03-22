# settings.gradle.kts 配置详解

## 📋 文件概述

`settings.gradle.kts` 是 Gradle 项目的**设置脚本**，在构建过程的初始化阶段执行，负责定义项目结构、配置插件仓库、管理依赖下载源等核心功能。

---

## 🎯 核心配置模块

### 1. pluginManagement - 插件仓库管理

#### 作用
指定 Gradle 插件的下载来源，解决"插件从哪里来"的问题。

#### 配置示例
```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
```

#### 实际场景对比

**❌ 没有配置时：**
```
场景：你在 build.gradle.kts 中声明了 Android 插件
plugins {
    id("com.android.application") version "8.1.0"
}

结果：
Gradle: "我去哪里找 com.android.application 插件？"
❌ 构建失败：Plugin not found
```

**✅ 有配置时：**
```
执行流程：
1. Gradle 读取 pluginManagement.repositories
2. 看到 google() 仓库 → "去 Google 仓库找找"
3. 找到 com.android.application:8.1.0
4. 下载并应用插件
5. ✅ 构建成功
```

#### content 内容过滤机制

**设计意图**：加速依赖解析，避免每个插件都遍历所有仓库。

**底层实现机制**：
```kotlin
google {
    content {
        includeGroupByRegex("com\\.android.*")  // com.android.* 只从 google 下载
        includeGroupByRegex("com\\.google.*")  // com.google.* 只从 google 下载
        includeGroupByRegex("androidx.*")      // androidx.* 只从 google 下载
    }
}
```

**执行流程示例**：
```
需求 1：需要 com.android.application:8.1.0
├─ 检查 google 仓库的 content 规则
├─ 匹配到 "com\\.android.*" 模式 ✅
├─ 直接从 google 仓库下载（不检查其他仓库）
└─ ⚡ 快速完成

需求 2：需要 junit:junit:4.13.2
├─ 检查 google 仓库的 content 规则
├─ 不匹配任何模式 ❌
├─ google 仓库说："这不是我的范围"
├─ 转向 mavenCentral 仓库
└─ 从 mavenCentral 下载
```

**常见 Group ID 分类**：
| Group ID | 来源 | 典型依赖 |
|----------|------|----------|
| `com.android.*` | google 仓库 | `com.android.application`, `com.android.library` |
| `com.google.*` | google 仓库 | `com.google.gms.google-services` |
| `androidx.*` | google 仓库 | `androidx.core:core-ktx`, `androidx.appcompat` |
| `org.jetbrains.kotlin.*` | mavenCentral | Kotlin 相关插件 |
| `com.squareup.*` | mavenCentral | Retrofit, OkHttp 等 |

---

### 2. plugins 块 - 工具链插件

#### 作用
应用 Gradle 工具链管理插件，实现 JDK 自动下载和版本统一。

#### 配置示例
```kotlin
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
```

#### 实际场景：团队 JDK 版本不一致问题

**问题重现**：
```
团队成员环境：
👤 张三：JDK 17 ✅ 构建成功
👤 李四：JDK 11 ❌ 报错：Unsupported class file major version 61
👤 王五：没装 JDK ❌ 报错：No Java home
```

**解决方案**：
```kotlin
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
```

**执行流程**：
```
👤 李四运行构建：
├─ Gradle 检测到需要 JDK 17
├─ 检查本地环境 → 只有 JDK 11
├─ Foojay 插件介入："我去 Adoptium 官网下载 JDK 17"
├─ ⬇️ 自动下载 JDK 17
├─ 🔧 自动配置 JAVA_HOME
├─ ✅ 使用 JDK 17 构建成功

👤 王五运行构建：
├─ Gradle 检测到需要 JDK 17
├─ 检查本地环境 → 没有 JDK
├─ Foojay 插件："我下载并安装 JDK 17"
├─ ⬇️ 下载 → 解压 → 配置环境变量
├─ ✅ 使用 JDK 17 构建成功
```

**没有插件时的痛苦流程**：
```
❌ 李四手动操作：
   1. 打开 Oracle/Adoptium 官网
   2. 下载 JDK 17
   3. 解压到 /Library/Java/JavaVirtualMachines
   4. 编辑 ~/.zshrc 或 ~/.bash_profile
   5. 添加 export JAVA_HOME=/path/to/jdk-17
   6. source ~/.zshrc
   7. java -version 验证
   8. 重新运行构建

❌ 王五可能还会下载错误版本（JDK 11 vs JDK 17）
```

**状态同步机制**：
- **设计意图**：确保团队所有成员使用相同 JDK 版本
- **实际效果**：消除"在我机器上能跑"的问题
- **底层机制**：通过 toolchain descriptor 自动解析和安装 JDK

---

### 3. dependencyResolutionManagement - 依赖仓库管理

#### 作用
统一管理所有模块的依赖下载源，确保依赖来源一致性和安全性。

#### 配置示例
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
```

#### 场景 1：统一配置依赖仓库

**❌ 错误做法 - 各模块重复配置**：
```kotlin
// app/build.gradle.kts
repositories {
    google()
    mavenCentral()
}

// lib-common/build.gradle.kts
repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }  // ← 私自添加仓库
}

// feature-login/build.gradle.kts
repositories {
    google()
    mavenCentral()
    maven { url = uri("https://untrusted-repo.com") }  // ← 危险仓库
}
```

**问题分析**：
```
⚠️ 代码重复：每个模块都要写一遍 repositories
⚠️ 维护困难：修改仓库配置要改多个文件
⚠️ 安全隐患：某个模块可能添加了不可信的仓库
⚠️ 版本混乱：同一个库可能从不同仓库下载，版本不一致
```

**✅ 正确做法 - 统一管理**：
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// app/build.gradle.kts - 不需要再写 repositories
// lib-common/build.gradle.kts - 不需要再写 repositories
// feature-login/build.gradle.kts - 不需要再写 repositories
```

**实际效果**：
```
✅ 所有模块统一使用这里的仓库配置
✅ app 模块想自己加仓库？
   ❌ 编译失败：Repositories are not allowed
✅ 保证所有模块依赖来源一致
✅ 消除安全隐患
```

#### 场景 2：公司私有仓库

**背景**：公司内部 SDK 需要从私有 Maven 仓库下载

**配置示例**：
```kotlin
dependencyResolutionManagement {
    repositories {
        google()       // Android 相关
        mavenCentral() // 公共开源库
        mavenLocal()   // 本地 Maven 仓库（可选）
        
        // 公司私有仓库
        maven {
            url = uri("https://maven.company.com/releases")
            credentials {
                username = findProperty("MAVEN_USERNAME") ?: "guest"
                password = findProperty("MAVEN_PASSWORD") ?: ""
            }
        }
        
        // JitPack 仓库（用于 GitHub 项目）
        maven { 
            url = uri("https://jitpack.io") 
        }
    }
}
```

**依赖下载流程**：
```kotlin
// app/build.gradle.kts
dependencies {
    implementation("com.company:internal-sdk:1.0.0")    // ← 公司 SDK
    implementation("androidx.core:core-ktx:1.12.0")     // ← Android 库
    implementation("com.squareup.retrofit2:retrofit:2.9.0") // ← Retrofit
}
```

**Gradle 解析过程**：
```
需求 1：com.company:internal-sdk:1.0.0
├─ 检查 google 仓库 → ❌ 未找到
├─ 检查 mavenCentral → ❌ 未找到
├─ 检查 company 仓库 → ✅ 找到
├─ 使用用户名密码验证
└─ 下载成功

需求 2：androidx.core:core-ktx:1.12.0
├─ 检查 google 仓库 → ✅ 找到（content 规则匹配）
├─ 直接下载（不检查其他仓库）
└─ ⚡ 快速完成

需求 3：com.squareup.retrofit2:retrofit:2.9.0
├─ 检查 google 仓库 → ❌ 不匹配 content 规则
├─ 检查 mavenCentral → ✅ 找到
└─ 下载成功
```

#### 场景 3：FAIL_ON_PROJECT_REPOS 强制模式

**模式说明**：
| 模式 | 行为 | 使用场景 |
|------|------|----------|
| `FAIL_ON_PROJECT_REPOS` | 禁止模块级仓库声明 | 企业级项目，强调一致性 |
| `PREFER_SETTINGS` | 优先使用 settings 配置，但允许模块补充 | 过渡期项目 |
| `FAIL_ON_OVERLAP` | 仅禁止重复声明 | 宽松管理 |

**实际案例对比**：

**没有 FAIL_ON_PROJECT_REPOS**：
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

// app/build.gradle.kts
repositories {
    maven { url = uri("https://untrusted-repo.com") }  // ← 可以添加
}
```

**风险**：
```
⚠️ 开发者 A 添加了不安全仓库
⚠️ 开发者 B 的模块从该仓库下载了被篡改的库
⚠️ 安全漏洞引入项目
```

**启用 FAIL_ON_PROJECT_REPOS**：
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// app/build.gradle.kts
repositories {
    maven { url = uri("https://untrusted-repo.com") }  
    // ❌ 编译立即失败！
}
```

**错误信息**：
```
❌ Build failed with exception:
   Repositories were declared in both settings and project.
   This is not supported when repositories are declared in settings 
   with FAIL_ON_PROJECT_REPOS mode.

✅ 强制所有模块使用统一的仓库配置
✅ 从源头杜绝安全问题
```

---

### 4. rootProject.name 和 include - 项目结构定义

#### 作用
定义项目名称和包含的模块，构建多模块项目的基础架构。

#### 配置示例
```kotlin
rootProject.name = "AcsTool"
include(":app")
```

#### 场景 1：单模块 → 多模块演进

**初始阶段（单模块）**：
```
项目结构：
AcsTool/
├── app/
│   ├── src/
│   └── build.gradle.kts
└── settings.gradle.kts

配置：
rootProject.name = "AcsTool"
include(":app")
```

**发展阶段（多模块）**：
```
项目结构：
AcsTool/
├── app/              # 主应用模块
├── lib-common/       # 公共库模块
├── feature-login/    # 登录功能模块
├── feature-home/     # 首页功能模块
└── sdk/              # SDK 模块

配置：
rootProject.name = "AcsTool"
include(":app")
include(":lib-common")
include(":feature-login")
include(":feature-home")
include(":sdk")
```

**模块间依赖关系**：
```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":lib-common"))
    implementation(project(":feature-login"))
    implementation(project(":feature-home"))
    implementation(project(":sdk"))
    
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
}

// feature-login/build.gradle.kts
dependencies {
    implementation(project(":lib-common"))  // 登录模块依赖公共库
    implementation(project(":sdk"))         // 登录模块依赖 SDK
}
```

**Gradle 构建流程**：
```
执行 ./gradlew assembleDebug
├─ 读取 settings.gradle.kts
├─ 识别项目结构：
│   ├─ rootProject: AcsTool
│   └─ 子模块：app, lib-common, feature-login, feature-home, sdk
├─ 构建依赖图：
│   app → feature-login → lib-common
│   app → feature-home → lib-common
│   app → sdk
│   feature-login → sdk
├─ 按依赖顺序构建：
│   1. 编译 lib-common
│   2. 编译 sdk
│   3. 编译 feature-login
│   4. 编译 feature-home
│   5. 编译 app
└─ 打包 APK ✅
```

#### 场景 2：自定义模块路径

**实际问题**：
```
SDK 代码在 external/alibaba-sdk 目录
想让它在项目中叫 :sdk（名字简洁）
```

**解决方案**：
```kotlin
include(":sdk")
project(":sdk").projectDir = file("external/alibaba-sdk")
```

**目录结构**：
```
AcsTool/
├── app/
├── external/
│   └── alibaba-sdk/    # 实际物理路径
│       ├── src/
│       └── build.gradle.kts
└── settings.gradle.kts
```

**实际使用**：
```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":sdk"))  // ← 引用逻辑名 :sdk
    // 不是 :external-alibaba-sdk
}
```

**好处**：
```
✅ 模块名称简洁（:sdk vs :alibaba-sdk）
✅ 物理位置灵活（代码可放在任何目录）
✅ 便于组织大型项目（如 monorepo 结构）
✅ 方便集成第三方源码
```

**更多应用场景**：
```kotlin
// 场景 1：集成本地库
include(":local-lib")
project(":local-lib").projectDir = file("../shared/local-lib")

// 场景 2：Git 子模块
include(":submodule-sdk")
project(":submodule-sdk").projectDir = file("submodules/sdk")

// 场景 3：动态包含所有 feature 模块
val featureModules = file("features").listFiles()
    ?.filter { it.isDirectory && File(it, "build.gradle.kts").exists() }
    ?: emptyList()

featureModules.forEach { dir ->
    include(":feature-${dir.name}")
    project(":feature-${dir.name}").projectDir = dir
}
```

---

## 🔄 完整构建流程解析

### 执行流程图解

```
用户执行：./gradlew assembleDebug
│
├─ 第 1 步：Gradle 启动
│   └─ 查找 settings.gradle.kts
│
├─ 第 2 步：初始化阶段
│   ├─ 读取 pluginManagement.repositories
│   │   └─ 配置插件下载源（google, mavenCentral, gradlePluginPortal）
│   ├─ 应用 foojay 插件
│   │   └─ 检查并自动下载所需 JDK 版本
│   └─ 读取 dependencyResolutionManagement
│       └─ 配置依赖仓库和模式（FAIL_ON_PROJECT_REPOS）
│
├─ 第 3 步：项目结构加载
│   ├─ 设置 rootProject.name = "AcsTool"
│   ├─ 解析 include(":app")
│   └─ 建立模块依赖关系图
│
├─ 第 4 步：加载模块构建脚本
│   ├─ 加载 app/build.gradle.kts
│   ├─ 解析 plugins 块（应用 Android 插件、Kotlin 插件等）
│   └─ 解析 dependencies 块（声明依赖关系）
│
├─ 第 5 步：依赖解析
│   ├─ androidx.appcompat → google 仓库（content 规则匹配）
│   ├─ retrofit2 → mavenCentral 仓库
│   └─ project(":lib-common") → 本地模块（直接引用）
│
├─ 第 6 步：任务执行
│   ├─ mergeDebugResources
│   ├─ processDebugManifest
│   ├─ compileDebugKotlin
│   ├─ compileDebugJavaWithJavac
│   ├─ packageDebugResources
│   └─ ... (更多构建任务)
│
└─ 第 7 步：构建完成
    └─ 输出：app/build/outputs/apk/debug/app-debug.apk
```

### 状态同步机制

**设计意图**：确保整个项目在一致的构建环境中执行。

**同步维度**：

1. **JDK 版本同步**
   ```
   通过 foojay 插件：
   - 检测项目需要的 JDK 版本
   - 检查本地环境
   - 自动下载并配置（如果缺失）
   - 结果：所有人使用相同 JDK
   ```

2. **仓库配置同步**
   ```
   通过 dependencyResolutionManagement：
   - 统一声明所有仓库
   - 禁止模块私自添加
   - 结果：所有模块依赖来源一致
   ```

3. **插件版本同步**
   ```
   通过 pluginManagement + libs.versions.toml：
   - 集中管理插件版本
   - 避免版本冲突
   - 结果：构建行为可预测
   ```

---

## 📚 常见配置模式

### 1. 基础配置（当前项目）

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AcsTool"
include(":app")
```

**适用场景**：标准 Android 单模块项目

### 2. 企业级配置（含私有仓库）

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        
        // 公司私有插件仓库
        maven {
            url = uri("https://maven.company.com/plugins")
            credentials {
                username = findProperty("PLUGIN_USERNAME") ?: ""
                password = findProperty("PLUGIN_PASSWORD") ?: ""
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        
        // 公司私有依赖仓库
        maven {
            url = uri("https://maven.company.com/releases")
            credentials {
                username = findProperty("MAVEN_USERNAME") ?: ""
                password = findProperty("MAVEN_PASSWORD") ?: ""
            }
        }
        
        // JitPack（GitHub 项目）
        maven { url = uri("https://jitpack.io") }
        
        // 本地仓库（开发调试用）
        mavenLocal()
    }
}

rootProject.name = "AcsTool"
include(":app")
include(":lib-common")
include(":feature:login")
include(":feature:home")
```

**适用场景**：企业多模块项目，含私有依赖

### 3. 动态模块配置

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AcsTool"

// 静态模块
include(":app")

// 动态包含所有 feature 模块
val featureDir = file("features")
if (featureDir.exists()) {
    featureDir.listFiles()
        ?.filter { it.isDirectory }
        ?.forEach { feature ->
            val moduleName = ":feature:${feature.name}"
            include(moduleName)
            project(moduleName).projectDir = feature
        }
}

// 动态包含所有 library 模块
val libDir = file("libraries")
if (libDir.exists()) {
    libDir.listFiles()
        ?.filter { it.isDirectory && File(it, "build.gradle.kts").exists() }
        ?.forEach { lib ->
            val moduleName = ":lib:${lib.name}"
            include(moduleName)
            project(moduleName).projectDir = lib
        }
}
```

**适用场景**：大型项目，模块数量经常变化

### 4. Monorepo 配置

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AcsTool"

// 主应用
include(":app")
project(":app").projectDir = file("apps/main")

// 共享库
include(":core:network")
project(":core:network").projectDir = file("core/network")

include(":core:ui")
project(":core:ui").projectDir = file("core/ui")

include(":core:data")
project(":core:data").file("core/data")

// 功能模块
include(":features:auth")
project(":features:auth").projectDir = file("features/auth")

include(":features:profile")
project(":features:profile").projectDir = file("features/profile")

// 外部依赖（Git 子模块）
include(":third-party:analytics")
project(":third-party:analytics").projectDir = file("third-party/analytics")
```

**适用场景**：超大型项目，多个应用共享组件

---

## 💡 最佳实践总结

### 1. 仓库配置优化

```kotlin
// ✅ 推荐：使用 content 过滤加速解析
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// ❌ 不推荐：没有 content 过滤
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

**性能差异**：
```
有 content 过滤：
- 解析 com.android.application
- 直接查询 google 仓库（跳过其他仓库）
- 耗时：~100ms

无 content 过滤：
- 解析 com.android.application
- 依次查询 google → mavenCentral → gradlePluginPortal
- 耗时：~500ms
```

### 2. 敏感信息管理

```kotlin
// ✅ 推荐：通过 gradle.properties 获取
maven {
    url = uri("https://maven.company.com/releases")
    credentials {
        username = findProperty("MAVEN_USERNAME") ?: ""
        password = findProperty("MAVEN_PASSWORD") ?: ""
    }
}

// 在 ~/.gradle/gradle.properties 中配置：
// MAVEN_USERNAME=zhangsan
// MAVEN_PASSWORD=your_password

// ❌ 不推荐：硬编码凭证
maven {
    url = uri("https://maven.company.com/releases")
    credentials {
        username = "zhangsan"
        password = "password123"  // ⚠️ 泄露风险！
    }
}
```

### 3. 模块化命名规范

```kotlin
// ✅ 推荐：清晰的层级结构
include(":app")
include(":lib-common")
include(":lib-network")
include(":feature:login")
include(":feature:home")
include(":feature:profile")
include(":core:network")
include(":core:ui")
include(":core:data")

// ❌ 不推荐：扁平化命名
include(":app")
include(":common")
include(":network")
include(":login")
include(":home")
// 模块多了以后难以管理
```

### 4. 版本控制

```kotlin
// ✅ 推荐：明确指定插件版本
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// ❌ 不推荐：使用最新版本（可能不稳定）
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "latest"
}
```

---

## 🎯 配置对比表

| 配置项 | 作用 | 是否必需 | 默认值 | 推荐配置 |
|--------|------|----------|--------|----------|
| `pluginManagement.repositories` | 插件下载源 | 是 | 无 | google + mavenCentral + gradlePluginPortal |
| `content` 过滤 | 加速插件解析 | 否 | 无过滤 | 使用正则表达式过滤 |
| `foojay 插件` | JDK 自动管理 | 推荐 | 无 | version "1.0.0" |
| `dependencyResolutionManagement` | 依赖仓库管理 | 是 | 无 | 统一配置所有仓库 |
| `repositoriesMode` | 仓库模式控制 | 否 | PREFER_SETTINGS | FAIL_ON_PROJECT_REPOS |
| `rootProject.name` | 项目名称 | 是 | 目录名 | 有意义的项目名 |
| `include()` | 包含模块 | 是 | 空 | 至少包含 :app |

---

## 🔍 常见问题排查

### Q1: 插件下载失败

**现象**：
```
Could not resolve com.android.application:8.1.0
```

**排查步骤**：
1. 检查 `pluginManagement.repositories` 是否包含 `google()`
2. 检查网络连接（可能需要配置代理）
3. 清除缓存：`./gradlew --refresh-dependencies`

**解决方案**：
```kotlin
pluginManagement {
    repositories {
        google()  // ← 确保有这个
        mavenCentral()
        gradlePluginPortal()
    }
}
```

### Q2: 依赖冲突

**现象**：
```
Conflict found for androidx.core:core-ktx
```

**排查步骤**：
1. 检查是否启用了 `FAIL_ON_PROJECT_REPOS`
2. 查看依赖树：`./gradlew app:dependencies`
3. 统一版本号

**解决方案**：
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
```

### Q3: 模块找不到

**现象**：
```
Project ':feature-login' not found in root project 'AcsTool'
```

**排查步骤**：
1. 检查 `include()` 是否声明了该模块
2. 检查模块目录是否存在 `build.gradle.kts`
3. 检查路径是否正确

**解决方案**：
```kotlin
include(":feature-login")  // ← 确保声明
project(":feature-login").projectDir = file("feature-login")  // ← 路径正确
```

### Q4: JDK 版本不一致

**现象**：
```
Unsupported class file major version 61
```

**解决方案**：
```kotlin
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
```

然后在 `gradle.properties` 中指定：
```properties
org.gradle.java.home=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
```

---

## 📖 扩展阅读

- [Gradle 官方文档 - Settings Plugin](https://docs.gradle.org/current/dsl/org.gradle.api.initialization.Settings.html)
- [Gradle 依赖管理](https://docs.gradle.org/current/userguide/dependency_management.html)
- [Foojay 工具链插件](https://github.com/gradle/foojay-toolchains)
- [Android Gradle 插件发布说明](https://developer.android.com/build/releases/gradle-plugin)

---

## 📝 版本历史

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| 1.0 | 2024-03-22 | 初始版本，基于 AcsTool 项目配置 |

---

**文档维护者**: AcsTool Team  
**最后更新**: 2024-03-22