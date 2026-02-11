package com.annotation.catalogfilecomment.action

import com.annotation.catalogfilecomment.ui.FileCommentManagerDialog
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class OpenManagerAction : AnAction() {
    init {
        templatePresentation.text = "File Comments Manager"
        templatePresentation.description = "Open batch file comments manager"
        templatePresentation.icon = com.intellij.icons.AllIcons.Toolwindows.ToolWindowProject
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        FileCommentManagerDialog(project).show()
    }
}