# build.gradle.kts 配置详解

## 📋 文件概述

`build.gradle.kts` 是 Gradle 项目的**核心构建配置文件**,定义项目的插件、依赖、编译配置、构建任务等关键信息。分为**根目录**和**模块级**两种，各自承担不同职责。

---

## 🎯 根目录 vs 模块级

### 根目录 build.gradle.kts

**位置**: `/build.gradle.kts`

**作用**: 配置整个项目的通用设置，通常不包含具体业务逻辑。

**当前项目示例**:
```kotlin
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
```

**关键词解读**:
- `apply false`: 声明插件但不应用 (让子模块自己决定何时应用)
- 目的：统一版本管理，避免各模块重复声明

---

### 模块级 build.gradle.kts

**位置**: `/app/build.gradle.kts`

**作用**: 配置具体模块的构建细节，包含实际业务代码的编译配置。

**典型结构**:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.acstool"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.example.acstool"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
}
```

---

## 🔧 根目录 build.gradle.kts 详解

### 场景 1:多模块版本统一管理

**问题**: 各模块使用不同版本的插件

**❌ 错误做法**:
```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application") version "8.1.0"
    id("org.jetbrains.kotlin.android") version "1.9.0"
}

// lib-common/build.gradle.kts
plugins {
    id("com.android.library") version "8.0.0"  // ← 版本不一致
    id("org.jetbrains.kotlin.android") version "1.8.0"
}

// feature-login/build.gradle.kts
plugins {
    id("com.android.application") version "8.2.0"  // ← 又一个版本
    id("org.jetbrains.kotlin.android") version "2.0.0"
}
```

**问题分析**:
```
⚠️ 版本混乱:3 个模块使用 3 种 AGP 版本
⚠️ 维护困难：升级需要改多个文件
⚠️ 构建风险：可能出现兼容性问题
⚠️ 编译错误：不同版本 AGP 行为不一致
```

**✅ 正确做法**:
```kotlin
// 根目录 build.gradle.kts
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
}

// app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)  // ← 应用插件
    alias(libs.plugins.kotlin.android)
}

// lib-common/build.gradle.kts
plugins {
    alias(libs.plugins.android.library)  // ← 应用插件
    alias(libs.plugins.kotlin.android)
}
```

**版本统一管理**:
```toml
# gradle/libs.versions.toml
[versions]
agp = "8.1.0"
kotlin = "2.0.0"

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

**效果**:
```
✅ 所有模块使用 AGP 8.1.0
✅ 所有模块使用 Kotlin 2.0.0
✅ 升级时只需改 libs.versions.toml
✅ 版本一致性有保障
```

---

### 场景 2:配置子项目通用规则

**需求**: 所有模块使用相同的 Java 版本

**❌ 重复配置**:
```kotlin
// app/build.gradle.kts
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// lib-common/build.gradle.kts
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// feature-login/build.gradle.kts
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

**✅ 统一配置**:
```kotlin
// 根目录 build.gradle.kts
subprojects {
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
}

// 或者针对 Android 项目
allprojects {
    afterEvaluate {
        extensions.findByType<com.android.build.gradle.BaseExtension>()?.apply {
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
}
```

**效果**:
```
✅ 所有模块自动继承配置
✅ 不需要在每个模块重复声明
✅ 修改一处，全局生效
```

---

### 场景 3:配置 Checkstyle 等代码检查

**需求**: 所有模块执行代码质量检查

```kotlin
// 根目录 build.gradle.kts
plugins {
    id("checkstyle") apply false
}

subprojects {
    plugins.apply("checkstyle")
    
    configure<CheckstyleExtension> {
        toolVersion = "10.12.0"
        configFile = rootProject.file("config/checkstyle.xml")
    }
    
    tasks.named("check") {
        dependsOn(tasks.withType<Checkstyle>())
    }
}
```

**执行流程**:
```
./gradlew check
├─ :app:checkstyleMain
├─ :app:checkstyleTest
├─ :lib-common:checkstyleMain
├─ :lib-common:checkstyleTest
├─ :feature-login:checkstyleMain
└─ :feature-login:checkstyleTest

结果:
✅ 所有模块统一代码规范
✅ 一次命令检查全部
```

---

## 📦 模块级 build.gradle.kts 详解

### 场景 1:Android 应用配置

**完整示例**:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.acstool"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.example.acstool"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
```

---

### 场景 2:多 Flavor 配置 (多渠道打包)

**需求**: 为不同渠道生成不同的 APK

```kotlin
android {
    flavorDimensions += "channel"
    
    productFlavors {
        create("xiaomi") {
            dimension = "channel"
            applicationIdSuffix = ".xiaomi"
            buildConfigField("String", "CHANNEL", "\"xiaomi\"")
        }
        
        create("huawei") {
            dimension = "channel"
            applicationIdSuffix = ".huawei"
            buildConfigField("String", "CHANNEL", "\"huawei\"")
        }
        
        create("googleplay") {
            dimension = "channel"
            applicationIdSuffix = ".gp"
            buildConfigField("String", "CHANNEL", "\"googleplay\"")
        }
    }
    
    buildTypes {
        create("staging") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".staging"
        }
        
        getByName("release") {
            isMinifyEnabled = true
        }
    }
}
```

**生成的变体**:
```
./gradlew assembleDebug
├─ app-xiaomi-debug.apk
├─ app-huawei-debug.apk
└─ app-googleplay-debug.apk

./gradlew assembleRelease
├─ app-xiaomi-release.apk
├─ app-huawei-release.apk
└─ app-googleplay-release.apk

./gradlew assembleStaging
├─ app-xiaomi-staging.apk
├─ app-huawei-staging.apk
└─ app-googleplay-staging.apk
```

**实际使用**:
```kotlin
// MainActivity.kt
val channel = BuildConfig.CHANNEL
when (channel) {
    "xiaomi" -> {
        // 小米渠道特定逻辑
    }
    "huawei" -> {
        // 华为渠道特定逻辑
    }
}
```

---

### 场景 3:Signing 签名配置

**需求**: 配置 Release 版本签名

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../keystore/release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = "release-key"
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }
    
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro")
        }
    }
}
```

**安全实践**:
```bash
# ✅ 推荐：使用环境变量
export KEYSTORE_PASSWORD=secret123
export KEY_PASSWORD=secret456
./gradlew assembleRelease

# ❌ 不推荐：硬编码密码
signingConfigs {
    create("release") {
        storePassword = "secret123"  # ← 泄露风险!
        keyPassword = "secret456"    # ← 不要这样做!
    }
}
```

---

### 场景 4:依赖配置最佳实践

#### 依赖配置方式对比

**❌ 硬编码版本号**:
```kotlin
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}
```

**问题**:
```
⚠️ 版本号分散，难以统一管理
⚠️ 升级需要逐个修改
⚠️ 可能出现版本冲突
```

**✅ 使用 Version Catalog**:
```kotlin
// gradle/libs.versions.toml
[versions]
coreKtx = "1.12.0"
appcompat = "1.6.1"
material = "1.11.0"
lifecycle = "2.7.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }

// app/build.gradle.kts
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
}
```

**优势**:
```
✅ 版本号集中管理
✅ IDE 自动补全提示
✅ 升级只需改 toml 文件
✅ 避免拼写错误
```

---

#### 依赖配置类型详解

```kotlin
dependencies {
    // ========== 编译时依赖 ==========
    
    // 添加到编译 classpath，也打包到 APK
    implementation("androidx.core:core-ktx:1.12.0")
    
    // 仅编译时需要，运行时不需要 (适用于接口、抽象类)
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    
    // ========== 运行时依赖 ==========
    
    // 不添加到编译 classpath，只在运行时可用
    runtimeOnly("androidx.core:core-ktx:1.12.0")
    
    // ========== 测试依赖 ==========
    
    // 本地单元测试 (JUnit、Mockito 等)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
    
    // 仅编译时需要的测试依赖
    testCompileOnly("org.mockito:mockito-core:5.8.0")
    
    // Android Instrumentation 测试
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    
    // ========== 模块依赖 ==========
    
    // 依赖其他模块
    implementation(project(":lib-common"))
    implementation(project(":lib-network"))
    
    // ========== 特殊配置 ==========
    
    // 排除传递依赖
    implementation("com.squareup.retrofit2:retrofit:2.9.0") {
        exclude(group = "com.google.code.gson", module = "gson")
    }
    
    // 强制使用特定版本 (解决冲突)
    implementation("com.google.guava:guava:32.1.3-jre") {
        because = "Fix security vulnerability CVE-2023-2976"
    }
}
```

---

### 场景 5:ProGuard/R8 混淆配置

```kotlin
android {
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            shrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

**proguard-rules.pro 示例**:
```proguard
# 保留模型类
-keep class com.example.acstool.model.** { *; }

# 保留 Retrofit 接口
-keep,allowobfuscation,allowshrinking interface com.example.acstool.api.**

# 保留 Gson 序列化字段
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 保留 Kotlin 协程
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# 保留 Compose
-keep class androidx.compose.** { *; }
```

**效果对比**:
```
未混淆 APK:
├─ app-release.apk (25MB)
├─ 包含所有类名、方法名
└─ 容易被反编译

混淆后 APK:
├─ app-release.apk (18MB)
├─ 类名变为 a.b.c
├─ 方法名变为 a.b.c
└─ 反编译难度大幅提升

体积减少:28%
```

---

## 🔄 构建类型详解

### Debug vs Release

```kotlin
android {
    buildTypes {
        // Debug 构建 (默认配置)
        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
        
        // Release 构建
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro")
        }
        
        // 自定义 Staging 构建 (用于测试环境)
        create("staging") {
            initWith(getByName("debug"))  // 继承 debug 配置
            applicationIdSuffix = ".staging"
            buildConfigField("String", "BASE_URL", "\"https://staging-api.example.com\"")
        }
    }
}
```

**构建变体**:
```
./gradlew assembleDebug
└─ app-debug.apk
   ├─ 可调试
   ├─ 未混淆
   └─ 包名:com.example.acstool.debug

./gradlew assembleRelease
└─ app-release.apk
   ├─ 不可调试
   ├─ 已混淆
   └─ 包名:com.example.acstool

./gradlew assembleStaging
└─ app-staging.apk
   ├─ 可调试
   ├─ 未混淆
   ├─ 包名:com.example.acstool.staging
   └─ API 地址:staging 环境
```

---

## 💡 最佳实践

### 1. 使用 Version Catalog

```toml
# gradle/libs.versions.toml
[versions]
agp = "8.1.0"
kotlin = "2.0.0"
coreKtx = "1.12.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }

[bundles]
androidx-basic = ["androidx.core-ktx", "androidx.appcompat"]

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
```

```kotlin
// app/build.gradle.kts
dependencies {
    // 使用 bundle
    implementation(libs.bundles.androidx.basic)
    
    // 单个依赖
    implementation(libs.androidx.core.ktx)
}
```

---

### 2. 分离配置到 buildSrc

**大型项目推荐**:
```
项目结构:
├── buildSrc/
│   └── src/main/kotlin/
│       ├── Dependencies.kt
│       └── Config.kt
├── app/
│   └── build.gradle.kts
└── build.gradle.kts
```

```kotlin
// buildSrc/src/main/kotlin/Dependencies.kt
object Versions {
    const val coreKtx = "1.12.0"
    const val appcompat = "1.6.1"
}

object Libs {
    val coreKtx = "androidx.core:core-ktx:${Versions.coreKtx}"
    val appcompat = "androidx.appcompat:appcompat:${Versions.appcompat}"
}

// app/build.gradle.kts
dependencies {
    implementation(Libs.coreKtx)
    implementation(Libs.appcompat)
}
```

---

### 3. 条件化配置

```kotlin
android {
    buildTypes {
        if (project.hasProperty("ci")) {
            // CI 环境特殊配置
            create("ci") {
                isMinifyEnabled = false
                isDebuggable = true
            }
        }
    }
}

// 使用方式
./gradlew assembleCi -Pci
```

---

## 🔍 常见问题排查

### Q1: 依赖冲突

**现象**:
```
Duplicate class found:
com.google.common.util.concurrent.ListenableFuture
```

**解决方案**:
```kotlin
dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.9.0") {
        // 排除冲突的依赖
        exclude(group = "com.google.guava", module = "guava")
    }
    
    // 显式声明正确版本
    implementation("com.google.guava:guava:32.1.3-jre")
}
```

---

### Q2: 插件版本不兼容

**现象**:
```
Incompatible because AGP 8.1.0 requires Gradle 8.0+
```

**解决方案**:
```bash
# 查看当前 AGP 版本
cat app/build.gradle.kts | grep "com.android.application"

# 查看 Gradle 版本
./gradlew --version

# 升级 Gradle
./gradlew wrapper --gradle-version 8.0
```

---

### Q3: 构建速度慢

**优化措施**:
```kotlin
// 启用构建缓存
android {
    buildCache {
        enabled = true
    }
}

// 配置编译选项
compileOptions {
    incremental = true
}

// Kotlin 增量编译
kotlinOptions {
    incremental = true
}
```

---

## 📊 配置对比表

| 配置项 | 根目录 | 模块级 |
|--------|--------|--------|
| 插件声明 | ✅ 统一管理 | ✅ 具体应用 |
| 依赖配置 | ❌ 不直接配置 | ✅ 主要位置 |
| Android 配置 | ❌ 不适用 | ✅ 核心配置 |
| 子项目通用配置 | ✅ 适用 | ❌ 不适用 |

---

## 📖 扩展阅读

- [Gradle 官方文档 - DSL 参考](https://docs.gradle.org/current/dsl/index.html)
- [Android Gradle 插件文档](https://developer.android.com/build)
- [Version Catalog 最佳实践](https://docs.gradle.org/current/userguide/platforms.html)

---

**文档维护者**: AcsTool Team  
**最后更新**: 2024-03-22