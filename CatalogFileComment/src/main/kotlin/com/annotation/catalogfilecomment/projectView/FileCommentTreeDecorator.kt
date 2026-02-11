package com.annotation.catalogfilecomment.projectView

import com.annotation.catalogfilecomment.config.FileCommentProjectConfig
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import java.awt.Color

class FileCommentTreeDecorator : ProjectViewNodeDecorator {
    private val nameAttr = SimpleTextAttributes.REGULAR_ATTRIBUTES
    private val slashColor = JBColor(
        Gray._180,
        Gray._80
    )
    private val slashAttr = SimpleTextAttributes(
        SimpleTextAttributes.STYLE_PLAIN,
        slashColor
    )
    private val commentColor = JBColor(
        Gray._110,
        Gray._125
    )
    private val commentAttr = SimpleTextAttributes(
        SimpleTextAttributes.STYLE_ITALIC,
        commentColor
    )

    override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
        val file = node.virtualFile ?: return
        if (file.isDirectory) return
        val project = node.project ?: return
        val comment = FileCommentProjectConfig
            .getInstance(project)
            .getEffectiveComment(file.path)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return
        val displayName = data.presentableText
            ?.takeIf { it.isNotBlank() }
            ?: file.name
        data.addText(displayName, nameAttr)
        data.addText("   ", SimpleTextAttributes.REGULAR_ATTRIBUTES) // visual gap
        data.addText("//", slashAttr)
        data.addText(" $comment", commentAttr)
    }
}