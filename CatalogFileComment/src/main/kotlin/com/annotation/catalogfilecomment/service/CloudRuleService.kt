package com.annotation.catalogfilecomment.service

import com.google.gson.Gson
import com.intellij.openapi.application.PathManager
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * 云端规则服务 - 企业级高可用版本
 * 策略：优先公网 → 本地缓存 → 断网兜底
 * 缓存机制：内存缓存(运行时) + 本地文件(持久化) + 24小时过期检查
 */
class CloudRuleService {

    // ✅ 你的 GitHub Raw 地址
    private val dataUrl =
        "https://gist.githubusercontent.com/jingyanx2-cmd/63ab74e15135fd3fd0aec8c6012bd360/raw/2e6bd7384c012bf462dd746042d7b1116ee59ce8/CatalogFileComments.json"

    private val cacheFileName = "catalog_rules_cache_v2.json"
    private val cacheDurationHours = 24

    // 内存缓存
    private var cachedRules: Map<String, String> = emptyMap()
    private var lastFetchTime: Long = 0

    init {
        // 支持强制刷新模式：-Dcatalogfilecomment.forcerefresh=true
        if (System.getProperty("catalogfilecomment.forcerefresh") == "true") {
            println("🔄 强制刷新模式已启用")
            forceRefresh()
        }
    }

    companion object {
        private var instance: CloudRuleService? = null
        fun getInstance(): CloudRuleService {
            if (instance == null) instance = CloudRuleService()
            return instance!!
        }
    }

    /**
     * 获取规则映射表
     * 返回格式：Key="Controller", Value="表现层_控制器"
     */
    fun getRules(): Map<String, String> {
        // 1. 检查内存缓存（未过期）
        if (cachedRules.isNotEmpty() && !isCacheExpired()) {
            println("✅ 使用内存缓存")
            return cachedRules
        }

        // 2. 尝试加载本地缓存
        val localRules = loadFromLocalDisk()
        if (localRules.isNotEmpty()) {
            cachedRules = localRules
            lastFetchTime = File(PathManager.getPluginTempPath(), cacheFileName).lastModified()

            if (!isCacheExpired()) {
                println("✅ 使用本地缓存")
                return localRules
            }
        }

        // 3. 尝试从网络获取最新规则
        println("🌐 尝试从云端加载...")
        val remoteRules = fetchFromRemote()

        return if (remoteRules.isNotEmpty()) {
            // 网络成功：更新内存 + 本地缓存
            cachedRules = remoteRules
            saveToLocalDisk(remoteRules)
            lastFetchTime = System.currentTimeMillis()
            println("✅ 云端加载成功，已更新缓存")
            remoteRules
        } else {
            // 网络失败：使用过期本地缓存兜底
            println("⚠️ 网络失败，使用过期缓存")
            localRules
        }
    }

    /**
     * 强制刷新缓存（忽略现有缓存）
     */
    fun forceRefresh(): Map<String, String> {
        println("🔄 强制刷新...")
        lastFetchTime = 0
        cachedRules = emptyMap()
        return getRules()
    }

    /**
     * 从远程获取规则
     */
    private fun fetchFromRemote(): Map<String, String> {
        return try {
            val uri = URI(dataUrl)
            val connection = uri.toURL().openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 5000      // 5秒连接超时
                readTimeout = 5000         // 5秒读取超时
                setRequestProperty("User-Agent", "CatalogFileComment-Plugin/1.0")
                setRequestProperty("Cache-Control", "max-age=3600")
            }

            val responseCode = connection.responseCode
            if (responseCode == 429) {
                println("❌ GitHub限流(429)，请稍后再试")
                return emptyMap()
            }

            if (responseCode == 200) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                parseJsonToFlatMap(json)
            } else {
                println("❌ HTTP错误: $responseCode")
                emptyMap()
            }
        } catch (e: Exception) {
            println("❌ 网络请求失败: ${e.message}")
            emptyMap()
        }
    }

    /**
     * 从本地磁盘加载缓存
     */
    private fun loadFromLocalDisk(): Map<String, String> {
        return try {
            val file = File(PathManager.getPluginTempPath(), cacheFileName)
            if (file.exists()) {
                val json = file.readText()
                val type = object : com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type
                Gson().fromJson(json, type)
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            println("⚠️ 读取本地缓存失败: ${e.message}")
            emptyMap()
        }
    }

    /**
     * 保存到本地磁盘
     */
    private fun saveToLocalDisk(data: Map<String, String>) {
        try {
            val dir = File(PathManager.getPluginTempPath())
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, cacheFileName)
            file.writeText(Gson().toJson(data))
            println("💾 已保存到: ${file.absolutePath}")
        } catch (e: Exception) {
            println("⚠️ 保存本地缓存失败: ${e.message}")
        }
    }

    /**
     * 检查缓存是否过期
     */
    private fun isCacheExpired(): Boolean {
        val elapsed = System.currentTimeMillis() - lastFetchTime
        return elapsed > TimeUnit.HOURS.toMillis(cacheDurationHours.toLong())
    }

    /**
     * 解析JSON为扁平Map
     * 输入：{"layers": [{"name": "表现层", "components": [{"type": "Controller", "description": "控制器"}]}]}
     * 输出：{"Controller": "表现层_控制器"}
     */
    private fun parseJsonToFlatMap(json: String): Map<String, String> {
        val resultMap = mutableMapOf<String, String>()
        try {
            val data = Gson().fromJson(json, ArchitectureData::class.java)
            data.layers?.forEach { layer ->
                val layerName = layer.name ?: "未知层"
                layer.components?.forEach { component ->
                    val suffix = component.type
                    val desc = component.description
                    if (!suffix.isNullOrBlank()) {
                        resultMap[suffix] = "${layerName}_${desc}"
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ JSON解析失败: ${e.message}")
        }
        return resultMap
    }

    // JSON数据结构
    private data class ArchitectureData(val layers: List<Layer>? = null)
    private data class Layer(val name: String? = null, val components: List<Component>? = null)
    private data class Component(val type: String? = null, val description: String? = null)
}