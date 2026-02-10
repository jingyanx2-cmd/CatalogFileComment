package com.annotation.catalogfilecomment.projectView

import com.annotation.catalogfilecomment.config.FileCommentProjectConfig
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import java.awt.Color

/**
 * Project View decorator — appends "  // <comment>" after the filename.
 *
 * ⚠️  KEY IntelliJ rendering rule (this is why filename was disappearing)
 * ─────────────────────────────────────────────────────────────────────────
 * PresentationData has TWO rendering modes:
 *
 *   Mode A — presentableText (plain String):
 *     Used when the coloredText list is EMPTY.
 *     This is the default; the IDE sets it to the file name.
 *
 *   Mode B — coloredText list (populated via addText()):
 *     As soon as ANY addText() call is made, this mode activates and
 *     presentableText is COMPLETELY IGNORED.
 *
 * Previous bug: we called addText("// comment") without first adding the
 * filename → coloredText mode activated with only the comment → filename
 * was replaced/hidden.
 *
 * Fix: ALWAYS call addText(filename) FIRST, then addText(comment).
 *
 * Visual result (matches the user's mockup):
 *   CultivationBreakthrough   // 数据实体
 *   LoginLog                  // 数据实体
 *   PunishmentRecord          // PunishmentRecord_数据实体
 *   RegisterRecord            // RegisterRecord_数据实体
 *   User                      // 实体类
 *   UserProfile               // UserProfile_核心组件
 */
class FileCommentTreeDecorator : ProjectViewNodeDecorator {

    // ── Colours (auto-adapt to Light / Dark theme) ────────────────────────────

    /**
     * File name: identical to the IDE's default REGULAR style so it looks
     * exactly as if we hadn't touched it.
     */
    private val nameAttr = SimpleTextAttributes.REGULAR_ATTRIBUTES

    /** The "//" glyph — noticeably dimmer than the comment body. */
    private val slashColor = JBColor(
        Gray._180,   // Light theme — light silver
        Gray._80     // Dark  theme — dim charcoal
    )
    private val slashAttr = SimpleTextAttributes(
        SimpleTextAttributes.STYLE_PLAIN,
        slashColor
    )

    /**
     * Comment body — classic IDE-comment gray, italic.
     * Italic mimics the look of real code comments in the editor.
     */
    private val commentColor = JBColor(
        Gray._110,   // Light theme — medium gray
        Gray._125    // Dark  theme — balanced gray
    )
    private val commentAttr = SimpleTextAttributes(
        SimpleTextAttributes.STYLE_ITALIC,
        commentColor
    )

    // ── Decorator ─────────────────────────────────────────────────────────────

    override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
        val file = node.virtualFile ?: return
        if (file.isDirectory) return

        val project = node.project ?: return

        // Only proceed if a comment has been configured for this file
        val comment = FileCommentProjectConfig
            .getInstance(project)
            .getEffectiveComment(file.path)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return   // No comment → leave presentation completely untouched

        // ── Step 1: Re-add the filename ───────────────────────────────────────
        //
        // CRITICAL: calling addText() switches PresentationData into
        // "coloredText mode", after which presentableText is ignored.
        // We must explicitly re-add the filename as the first text segment.
        //
        val displayName = data.presentableText
            ?.takeIf { it.isNotBlank() }
            ?: file.name

        data.addText(displayName, nameAttr)

        // ── Step 2: gap + "//" + comment body ────────────────────────────────
        data.addText("   ", SimpleTextAttributes.REGULAR_ATTRIBUTES) // visual gap
        data.addText("//", slashAttr)
        data.addText(" $comment", commentAttr)
    }
}