package com.annotation.catalogfilecomment.config

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "CatalogFileCommentsProject",
    storages = [Storage(".idea/catalog-file-comments.xml")]
)
class FileCommentProjectConfig(private val project: Project) {

    var useProjectSpecificConfig: Boolean = false

    val projectComments: MutableMap<String, String> = mutableMapOf()

    init {
        val autoSwitch = System.getProperty("catalogfilecomment.project.mode")
        if (autoSwitch != null) {
            switchMode(autoSwitch.toBoolean())
        }
        if (System.getProperty("catalogfilecomment.debug") == "true") {
            val stats = getProjectStats()
            println("FileCommentProjectConfig initialized: $stats")
        }
    }

    fun getEffectiveComment(filePath: String): String? {
        println("[${project.name}] Getting effective comment for: $filePath")

        return if (useProjectSpecificConfig) {
            projectComments[filePath]
        } else {
            FileCommentConfigService.getInstance().getComment(filePath)
        }
    }

    fun setEffectiveComment(filePath: String, comment: String?) {
        println("[${project.name}] Setting comment for: $filePath")

        if (useProjectSpecificConfig) {
            if (comment.isNullOrBlank()) {
                projectComments.remove(filePath)
            } else {
                projectComments[filePath] = comment.trim()
            }
        } else {
            if (comment.isNullOrBlank()) {
                FileCommentConfigService.getInstance().removeComment(filePath)
            } else {
                FileCommentConfigService.getInstance().setComment(filePath, comment.trim())
            }
        }
        if (comment.isNullOrBlank()) {
            cleanupAfterRemove(filePath)
        }
    }

    fun removeEffectiveComment(filePath: String) {
        println("[${project.name}] Removing comment for: $filePath")

        if (useProjectSpecificConfig) {
            projectComments.remove(filePath)
        } else {
            FileCommentConfigService.getInstance().removeComment(filePath)
        }
        if (hasEffectiveComment(filePath)) {
            println("Warning: Comment still exists after removal: $filePath")
        }
    }

    fun hasEffectiveComment(filePath: String): Boolean {
        if (System.getProperty("catalogfilecomment.debug") == "true") {
            println("[${project.name}] Checking comment existence for: $filePath")
        }
        return getEffectiveComment(filePath) != null
    }

    private fun cleanupAfterRemove(filePath: String) {
        // 双重检查确保彻底删除
        if (hasEffectiveComment(filePath)) {
            removeEffectiveComment(filePath)
        }
    }

    fun migrateAllComments(toProjectSpecific: Boolean) {
        if (this.useProjectSpecificConfig == toProjectSpecific) return

        val allComments = if (toProjectSpecific) {
            FileCommentConfigService.getInstance().getAllComments()
        } else {
            projectComments.toMap()
        }
        val originalMode = this.useProjectSpecificConfig
        allComments.forEach { (path, comment) ->
            // 使用 hasEffectiveComment 检查是否存在
            if (hasEffectiveComment(path)) {
                this.useProjectSpecificConfig = toProjectSpecific
                setEffectiveComment(path, comment)
                this.useProjectSpecificConfig = originalMode
                removeEffectiveComment(path)
            }
        }
        this.useProjectSpecificConfig = toProjectSpecific
        println("Migration completed. ${getProjectStats()}")
    }

    fun switchMode(useProjectSpecific: Boolean) {
        if (this.useProjectSpecificConfig != useProjectSpecific) {
            // 调用 migrateAllComments 进行数据迁移
            migrateAllComments(useProjectSpecific)
        }
    }

    fun getProjectStats(): Map<String, Any> {
        val totalInProject = projectComments.size
        val totalInGlobal = FileCommentConfigService.getInstance().getCommentCount()
        val allPaths = (projectComments.keys +
                FileCommentConfigService.getInstance().getAllComments().keys).toSet()
        val effectiveCount = allPaths.count { hasEffectiveComment(it) }
        return mapOf(
            "projectName" to project.name,
            "projectSpecificEnabled" to useProjectSpecificConfig,
            "totalInProjectStorage" to totalInProject,
            "totalInGlobalStorage" to totalInGlobal,
            "effectiveCommentCount" to effectiveCount
        )
    }

    companion object {
        fun getInstance(project: Project): FileCommentProjectConfig {
            return project.getService(FileCommentProjectConfig::class.java)
        }
    }
}