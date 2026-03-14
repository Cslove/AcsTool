package com.example.acstool // 替换为你的包名

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AcsAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AutoTool"
        // 提供一个静态变量，方便在Activity中检查服务状态
        var isServiceEnabled = false
    }

    // 当服务连接成功时调用，适合进行初始化配置
    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceEnabled = true
        Log.d(TAG, "✅ 无障碍服务已连接")

        // 动态配置服务：告诉系统我们想监听哪些事件
        val info = AccessibilityServiceInfo().apply {
            // 监听窗口状态变化和内容变化，这是自动化最基础的事件
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED

            // 设置反馈类型，对于自动化工具，通常使用反馈通用类型即可
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC

            // 允许服务检索窗口内容，这是读取屏幕文本的关键 [citation:4]
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        setServiceInfo(info)
    }

    // 当系统发送无障碍事件时调用，这是处理自动化逻辑的入口 [citation:7]
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        Log.d(TAG, "📢 收到事件: ${event.eventType}, 包名: ${event.packageName}")

        // 在这里，你可以解析 event 的源节点，获取屏幕上的文本或执行操作
        val sourceNode = event.source
        sourceNode?.let { node ->
            // 示例：打印出触发事件的节点文本
            val nodeText = node.text
            Log.d(TAG, "节点文本: $nodeText")
            // **重要：使用完后要回收节点，避免内存泄漏**
            node.recycle()
        }
    }

    // 当系统需要中断服务时调用（例如用户关闭了服务）[citation:7]
    override fun onInterrupt() {
        isServiceEnabled = false
        Log.d(TAG, "⛔ 无障碍服务被中断")
    }

    // 当服务销毁时调用
    override fun onDestroy() {
        super.onDestroy()
        isServiceEnabled = false
        Log.d(TAG, "👋 无障碍服务已销毁")
    }
}