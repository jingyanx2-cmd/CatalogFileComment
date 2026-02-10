package com.annotation.catalogfilecomment.action

import com.annotation.catalogfilecomment.config.FileCommentConfigService
import com.intellij.ide.projectView.ProjectView
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages

class RemoveCommentAction : AnAction() {
    init {
        templatePresentation.text = "Remove File Comment"
        templatePresentation.description = "Remove comment for this file"
        templatePresentation.icon = com.intellij.icons.AllIcons.Actions.Cancel
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val config = FileCommentConfigService.getInstance()
        if (!config.hasComment(file.path)) {
            Messages.showWarningDialog(
                project,
                "No comment found for ${file.name}",
                "Remove Comment"
            )
            return
        }
        val result = Messages.showYesNoDialog(
            project,
            "Are you sure you want to remove the comment for:\n${file.name}?",
            "Confirm Remove",
            Messages.getQuestionIcon()
        )
        if (result == Messages.YES) {
            config.removeComment(file.path)
            val notificationGroup = NotificationGroupManager.getInstance()
                .getNotificationGroup("Catalog File Comments")
            val notification = notificationGroup.createNotification(
                "Comment removed for ${file.name}",
                NotificationType.INFORMATION
            )
            Notifications.Bus.notify(notification, project)
            ProjectView.getInstance(project).refresh()
        }
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val hasComment = file != null &&
                !file.isDirectory &&
                FileCommentConfigService.getInstance().hasComment(file.path)

        e.presentation.isEnabledAndVisible = hasComment
    }
}