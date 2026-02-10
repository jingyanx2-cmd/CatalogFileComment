package com.annotation.catalogfilecomment.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent

class FileCommentManagerDialog(project: Project) : DialogWrapper(project) {

    private val panel = FileCommentManagerPanel()

    init {
        title = "File Comments Manager"
        init()
    }

    override fun createCenterPanel(): JComponent = panel

    override fun doOKAction() {
        panel.apply()
        super.doOKAction()
    }
}