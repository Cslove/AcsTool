# Kotlin DSL 构建器

## 前端类比

Kotlin DSL 构建器类似于前端的 JSX 或 Vue 模板语法，用声明式的方式构建复杂结构。

## 核心概念

### 带接收者的 Lambda

```kotlin
// 前端 JSX
<div>
  <h1>Title</h1>
  <p>Content</p>
</div>

// Kotlin DSL
html {
    head {
        title = "My Page"
    }
    body {
        h1 { +"Title" }
        p { +"Content" }
    }
}
```

## 实战示例

### 1. HTML 构建 DSL

```kotlin
// 目标语法
html {
    head {
        title = "My Website"
    }
    body {
        h1 { +"Welcome" }
        ul {
            li { +"Item 1" }
            li { +"Item 2" }
        }
    }
}

// 实现
abstract class Tag(val name: String) {
    protected val children = mutableListOf<Tag>()
    
    fun render(): String {
        val childContent = children.joinToString("\n") { it.render() }
        return "<$name>\n$childContent\n</$name>"
    }
}

class HtmlTag : Tag("html") {
    fun head(configure: HeadTag.() -> Unit) {
        children.add(HeadTag().apply(configure))
    }
    
    fun body(configure: BodyTag.() -> Unit) {
        children.add(BodyTag().apply(configure))
    }
}

class HeadTag : Tag("head") {
    var title: String = ""
        set(value) {
            children.add(object : Tag("title") {
                override fun render() = "<title>$value</title>"
            })
        }
}

class BodyTag : Tag("body") {
    fun h1(configure: H1Tag.() -> Unit) {
        children.add(H1Tag().apply(configure))
    }
    
    fun ul(configure: UlTag.() -> Unit) {
        children.add(UlTag().apply(configure))
    }
}

class H1Tag : Tag("h1") {
    operator fun String.unaryPlus() {
        // 文本内容
    }
}

class UlTag : Tag("ul") {
    fun li(configure: LiTag.() -> Unit) {
        children.add(LiTag().apply(configure))
    }
}

class LiTag : Tag("li") {
    operator fun String.unaryPlus() {
        // 文本内容
    }
}

fun html(configure: HtmlTag.() -> Unit): String {
    return HtmlTag().apply(configure).render()
}
```

### 2. Jetpack Compose DSL

```kotlin
@Composable
fun LoginScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "用户登录",
            fontSize = 24.sp
        )
        
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("用户名") }
        )
        
        Button(onClick = { /* 登录 */ }) {
            Text("登录")
        }
    }
}
```

## DSL 设计要点

### 1. 类型安全

```kotlin
// ✅ 类型安全
android {
    namespace = "com.example"  // 编译期检查
}

// ❌ 类型不安全（Groovy）
android {
    namespace 'com.example'  // 运行期检查
}
```

### 2. IDE 支持

```kotlin
dependencies {
    implementation(libs.  // 自动补全
}
```

### 3. 声明式语法

```kotlin
// ✅ 声明式
server {
    host = "192.168.1.100"
    port = 8080
}

// ❌ 命令式
val server = Server()
server.setHost("192.168.1.100")
server.setPort(8080)
```

## 实现步骤

### 1. 定义配置类

```kotlin
data class ServerConfig(
    var host: String = "localhost",
    var port: Int = 80
)
```

### 2. 添加配置方法

```kotlin
data class ServerConfig(
    var host: String = "localhost",
    var port: Int = 80,
    var security: SecurityConfig? = null
) {
    fun security(configure: SecurityConfig.() -> Unit) {
        security = SecurityConfig().apply(configure)
    }
}

data class SecurityConfig(
    var ssl: Boolean = false
)
```

### 3. 定义入口函数

```kotlin
fun server(configure: ServerConfig.() -> Unit): ServerConfig {
    return ServerConfig().apply(configure)
}
```

### 4. 使用 DSL

```kotlin
val config = server {
    host = "192.168.1.100"
    port = 8080
    
    security {
        ssl = true
    }
}
```

## 常见 DSL 模式

### 1. 嵌套配置

```kotlin
server {
    database {
        pool {
            maxSize = 20
        }
    }
}
```

### 2. 列表配置

```kotlin
dependencies {
    implementation("lib1")
    implementation("lib2")
    implementation("lib3")
}
```

### 3. 条件配置

```kotlin
android {
    buildTypes {
        if (isDebug) {
            debug { /* ... */ }
        }
    }
}
```

## 最佳实践

### 1. 提供默认值

```kotlin
data class ServerConfig(
    var host: String = "localhost",
    var port: Int = 80,
    var timeout: Long = 30000
)
```

### 2. 限制层级深度

```kotlin
// ✅ 合理（2-3 层）
server {
    database { /* ... */ }
}

// ❌ 过深（难以维护）
config {
    app {
        android {
            default { /* ... */ }
        }
    }
}
```

### 3. 清晰命名

```kotlin
// ✅ 清晰
dependencies {
    implementation("...")
    testImplementation("...")
}

// ❌ 模糊
deps {
    add1("...")
    add2("...")
}
```

## 扩展阅读

- [Kotlin DSL 指南](https://kotlinlang.org/docs/type-safe-builders.html)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
