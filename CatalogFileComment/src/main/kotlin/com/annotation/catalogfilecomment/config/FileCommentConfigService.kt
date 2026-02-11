package com.annotation.catalogfilecomment.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "CatalogFileComments",
    storages = [Storage("catalog-file-comments.xml")]
)
class FileCommentConfigService : PersistentStateComponent<FileCommentState> {

    private var state = FileCommentState()

    companion object {
        fun getInstance(): FileCommentConfigService {
            return ApplicationManager.getApplication()
                .getService(FileCommentConfigService::class.java)
        }
    }

    override fun getState(): FileCommentState = state

    override fun loadState(newState: FileCommentState) {
        XmlSerializerUtil.copyBean(newState, state)
    }

    fun getComment(filePath: String): String? {
        return state.comments[filePath]
    }

    fun setComment(filePath: String, comment: String) {
        if (comment.isBlank()) {
            state.comments.remove(filePath)
        } else {
            state.comments[filePath] = comment.trim()
        }
    }

    fun removeComment(filePath: String) {
        state.comments.remove(filePath)
    }

    fun getAllComments(): Map<String, String> {
        return state.comments.toMap()
    }

    fun setAllComments(comments: Map<String, String>) {
        state.comments.clear()
        state.comments.putAll(comments)
    }

    fun hasComment(filePath: String): Boolean {
        return state.comments.containsKey(filePath)
    }

    fun searchComments(keyword: String): Map<String, String> {
        return state.comments.filter { (_, comment) ->
            comment.contains(keyword, ignoreCase = true)
        }
    }

    fun getCommentCount(): Int = state.comments.size
}