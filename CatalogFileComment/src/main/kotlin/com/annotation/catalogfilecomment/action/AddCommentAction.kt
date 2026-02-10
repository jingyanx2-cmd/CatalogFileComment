package com.annotation.catalogfilecomment.action

import com.annotation.catalogfilecomment.config.FileCommentConfigService
import com.annotation.catalogfilecomment.ui.CommentInputDialog
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class AddCommentAction : AnAction() {
    init {
        templatePresentation.text = "Add File Comment"
        templatePresentation.description = "Add or edit comment for selected file"
        templatePresentation.icon = com.intellij.icons.AllIcons.Actions.IntentionBulb
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val file: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)
        if (file == null || file.isDirectory) return

        when (file.fileType.name) {
            "JAVA" -> handleJavaFile(project, file)
            "XML" -> handleXmlFile(project, file)
            "Kotlin" -> handleKotlinFile(project, file)
            else -> handleOtherFile(project, file)
        }
    }

    private fun handleJavaFile(project: Project, file: VirtualFile) {
        processFileWithDialog(project, file, "Java")
    }

    private fun handleXmlFile(project: Project, file: VirtualFile) {
        processFileWithDialog(project, file, "XML")
    }

    private fun handleKotlinFile(project: Project, file: VirtualFile) {
        processFileWithDialog(project, file, "Kotlin")
    }

    private fun handleOtherFile(project: Project, file: VirtualFile) {
        processFileWithDialog(project, file, "Other")
    }

    private fun processFileWithDialog(project: Project, file: VirtualFile, fileType: String) {
        val config = FileCommentConfigService.getInstance()
        val existingComment = config.getComment(file.path) ?: ""

        val dialog = CommentInputDialog(project, file, existingComment)

        if (dialog.showAndGet()) {
            val newComment = dialog.getInputText()

            if (newComment.isBlank()) {
                config.removeComment(file.path)
                showInfo(project, "[$fileType] Comment removed for ${file.name}")
            } else {
                config.setComment(file.path, newComment)
                showInfo(project, "[$fileType] Comment saved for ${file.name}")
            }

            refreshProjectView(project)
        }
    }

    override fun update(e: AnActionEvent) {
        val file: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val isValidFile = file != null && !file.isDirectory
        e.presentation.isEnabledAndVisible = isValidFile
    }

    private fun refreshProjectView(project: Project) {
        ProjectView.getInstance(project).refresh()
    }

    private fun showInfo(project: Project, message: String) {
        val notificationGroup = com.intellij.notification.NotificationGroupManager.getInstance()
            .getNotificationGroup("Catalog File Comments")

        val notification = notificationGroup.createNotification(
            message,
            com.intellij.notification.NotificationType.INFORMATION
        )
        com.intellij.notification.Notifications.Bus.notify(notification, project)
    }
}