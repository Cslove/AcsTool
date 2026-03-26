# build.gradle.kts 配置详解

## 前端类比

`build.gradle.kts` 类似于前端的 `package.json` + `webpack.config.js` 的组合，负责定义项目依赖和构建规则。

## 文件位置

- **根目录** `build.gradle.kts` - 全局配置，所有模块共享
- **模块目录** `build.gradle.kts` - 模块特定配置（如 `app/build.gradle.kts`）

## 根目录配置

### 插件管理

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
```

**作用**：统一管理所有模块的插件版本

**对比**：
```kotlin
// ❌ 错误：各模块版本不一致
// app/build.gradle.kts
id("com.android.application") version "8.0.0"

// lib/build.gradle.kts
id("com.android.application") version "8.2.0"

// ✅ 正确：版本统一
// 根目录统一声明
plugins {
    alias(libs.plugins.android.application) apply false
}

// 模块直接应用
plugins {
    alias(libs.plugins.android.application)
}
```

### 子项目通用配置

```kotlin
subprojects {
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
}
```

**效果**：所有模块自动使用 Java 17

## 模块级配置

### Android 应用配置

```kotlin
android {
    namespace = "com.example.acstool"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.example.acstool"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
}
```

### 多 Flavor 配置

```kotlin
android {
    flavorDimensions += "channel"
    
    productFlavors {
        create("xiaomi") {
            dimension = "channel"
            applicationIdSuffix = ".xiaomi"
        }
        
        create("huawei") {
            dimension = "channel"
            applicationIdSuffix = ".huawei"
        }
    }
}
```

**生成的 APK**：
```
app-xiaomi-release.apk
app-huawei-release.apk
```

### 依赖配置

```kotlin
dependencies {
    // 编译和运行时都需要
    implementation("androidx.core:core-ktx:1.12.0")
    
    // 仅编译时需要
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    
    // 本地单元测试
    testImplementation("junit:junit:4.13.2")
    
    // Android 设备测试
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    
    // 依赖其他模块
    implementation(project(":lib-common"))
}
```

### 使用 Version Catalog

```kotlin
// ❌ 硬编码版本
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
}

// ✅ 使用 Version Catalog
dependencies {
    implementation(libs.androidx.core.ktx)
}
```

## 构建类型

### Debug vs Release

```kotlin
buildTypes {
    debug {
        isDebuggable = true
        isMinifyEnabled = false
    }
    
    release {
        isDebuggable = false
        isMinifyEnabled = true
        proguardFiles("proguard-rules.pro")
    }
}
```

**对比**：

| 特性 | Debug | Release |
|------|-------|---------|
| 可调试 | ✅ | ❌ |
| 代码混淆 | ❌ | ✅ |
| 签名 | Debug Key | Release Key |
| 体积 | 大 | 小 |

## 最佳实践

### 1. 使用 Version Catalog

```toml
# gradle/libs.versions.toml
[versions]
coreKtx = "1.12.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
```

### 2. 分离配置到 buildSrc

大型项目推荐使用 `buildSrc` 管理通用配置。

### 3. 条件化配置

```kotlin
if (project.hasProperty("ci")) {
    buildTypes.create("ci") {
        isMinifyEnabled = false
    }
}
```

## 常见问题

### Q1: 依赖冲突

```kotlin
implementation("com.squareup.retrofit2:retrofit:2.9.0") {
    exclude(group = "com.google.guava", module = "guava")
}
```

### Q2: 插件版本不兼容

检查 AGP 与 Gradle 版本兼容性：
- AGP 8.1.x 需要 Gradle 8.0+
- AGP 8.0.x 需要 Gradle 8.0+

## 扩展阅读

- [Gradle DSL 参考](https://docs.gradle.org/current/dsl/index.html)
- [Android 构建配置](https://developer.android.com/build)
