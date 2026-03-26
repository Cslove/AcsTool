# Gradle DSL 语法详解

## 前端类比

Gradle DSL 类似于前端的配置文件语法，如 `webpack.config.js` 或 `vite.config.js`。

## 核心概念

### 1. 带接收者的 Lambda

```kotlin
// 前端写法
webpack({
  mode: 'development',
  entry: './src/index.js'
})

// Gradle 写法
android {
    namespace = "com.example"
    compileSdk = 34
}
```

**关键点**：
- `{ ... }` 内的 `this` 指向 `android` 块的配置对象
- 可以直接访问属性，无需前缀

### 2. 作用域切换

每一层花括号都在切换 `this` 的指向：

```kotlin
android {                    // this: AndroidExtension
    namespace = "..."        // AndroidExtension.namespace
    
    defaultConfig {          // this: defaultConfig
        applicationId = "..."  // defaultConfig.applicationId
    }
}
```

### 3. 省略括号

当函数只有一个 lambda 参数时，可以省略括号：

```kotlin
// 完整写法
dependencies({
    implementation("xxx")
})

// 标准写法（省略括号）
dependencies {
    implementation("xxx")
}
```

## 实战示例

### 解析 build.gradle.kts

```kotlin
android {
    namespace = "com.example.acstool"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.example.acstool"
        minSdk = 26
        targetSdk = 34
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}
```

**层级结构**：
```
android (AndroidExtension)
├── namespace
├── compileSdk
├── defaultConfig (BaseAppModuleExtension)
│   ├── applicationId
│   ├── minSdk
│   └── targetSdk
└── buildTypes
    └── release (BuildType)
        └── isMinifyEnabled
```

## 手写一个迷你 DSL

```kotlin
// 1. 定义配置类
data class ServerConfig(
    var host: String = "localhost",
    var port: Int = 80
)

// 2. 定义 DSL 入口函数
fun server(configure: ServerConfig.() -> Unit): ServerConfig {
    return ServerConfig().apply(configure)
}

// 3. 使用 DSL
val config = server {
    host = "192.168.1.100"
    port = 8080
}
```

## 关键要点

| 概念 | 说明 |
|------|------|
| 带接收者的 Lambda | Lambda 内的 `this` 指向特定对象 |
| 作用域切换 | 每层花括号改变 `this` 的指向 |
| 省略括号 | 单 lambda 参数时可省略 () |
| 声明式语法 | 描述"是什么"而非"怎么做" |

## 调试技巧

```kotlin
android {
    println("当前类型：${this::class.simpleName}")
    // 输出：当前类型：BaseAppModuleExtension
}
```

## 扩展阅读

- [Kotlin DSL 官方文档](https://kotlinlang.org/docs/type-safe-builders.html)
- [Gradle Kotlin DSL 指南](https://docs.gradle.org/current/userguide/kotlin_dsl.html)
