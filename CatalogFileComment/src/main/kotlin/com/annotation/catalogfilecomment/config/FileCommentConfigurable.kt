package com.annotation.catalogfilecomment.config

import com.annotation.catalogfilecomment.ui.FileCommentManagerPanel
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import javax.swing.JComponent

class FileCommentConfigurable : Configurable {

    private var panel: FileCommentManagerPanel? = null

    override fun getDisplayName(): String = "File Comments"

    override fun createComponent(): JComponent {
        panel = FileCommentManagerPanel()
        return panel!!
    }

    override fun isModified(): Boolean {
        return panel?.isModified() ?: false
    }

    override fun apply() {
        panel?.apply()
        // 刷新所有项目的 Project View
        ProjectManager.getInstance().openProjects.forEach { project ->
            com.intellij.ide.projectView.ProjectView.getInstance(project).refresh()
        }
    }

    override fun reset() {
        panel?.reset()
    }

    override fun disposeUIResources() {
        panel = null
    }
}