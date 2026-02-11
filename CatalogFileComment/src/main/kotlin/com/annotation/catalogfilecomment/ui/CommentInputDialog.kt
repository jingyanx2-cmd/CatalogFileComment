package com.annotation.catalogfilecomment.ui

import com.annotation.catalogfilecomment.service.CloudRuleService
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiModifier
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Comment input dialog with cloud-rule auto-analysis.
 *
 * Auto-analysis pipeline
 * ──────────────────────
 * 1. Read the class name from the Java file via PSI.
 * 2. Match the class name suffix against rules fetched from the cloud JSON
 *    (longest suffix key wins → "ServiceImpl" beats "Impl").
 * 3. If the cloud JSON returns no match, fall back to lightweight
 *    structural heuristics (interface / enum / entity fields).
 *
 * ❌  PSI doc-comments and inline comments are NEVER read.
 *     All analysis is based solely on: class name, field names,
 *     method names, implemented interfaces, superclass name.
 */
class CommentInputDialog(
    private val project: Project,
    private val file: VirtualFile,
    private val existingComment: String
) : DialogWrapper(project, true) {

    private val textField = JBTextField().apply {
        text = existingComment
    }

    private val validationRegex = Regex("""^[\u4E00-\u9FA5a-zA-Z0-9\s\-_+.]{2,50}$""")

    init {
        title = "File Comment — ${file.name}"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(10, 10)).apply {
            preferredSize = Dimension(500, 60)
        }

        textField.emptyText.text = "e.g. 业务层_用户鉴权服务"
        panel.add(JLabel("Comment:"), BorderLayout.WEST)
        panel.add(textField, BorderLayout.CENTER)

        val autoButton = JButton("Auto Analyze").apply {
            toolTipText = "Analyze class name and match cloud rules from JSON"
            addActionListener {
                text = "Analyzing…"
                isEnabled = false
                Thread {
                    val result = safeAnalyze(file, project)
                    SwingUtilities.invokeLater {
                        textField.text = result
                        text = "Auto Analyze"
                        isEnabled = true
                    }
                }.start()
            }
        }
        panel.add(autoButton, BorderLayout.EAST)

        return panel
    }

    override fun doValidate(): ValidationInfo? {
        val text = textField.text.trim()
        if (text.isEmpty()) return null   // blank = delete comment → OK

        if (!validationRegex.matches(text)) {
            return ValidationInfo(
                "2–50 chars · Chinese / Latin / digits / - _ + .",
                textField
            )
        }
        return null
    }

    fun getInputText(): String = textField.text.trim()
    private fun safeAnalyze(file: VirtualFile, project: Project): String {
        return try {
            analyzeFile(file, project)
        } catch (e: Exception) {
            file.nameWithoutExtension
        }
    }

    private fun analyzeFile(file: VirtualFile, project: Project): String {
        return when (file.extension?.lowercase()) {
            "java" -> analyzeJava(file, project)
            "kt" -> analyzeByName(file.nameWithoutExtension)
            else -> file.nameWithoutExtension
        }
    }

    private fun analyzeJava(file: VirtualFile, project: Project): String {
        val info = ReadAction.compute<ClassInfo?, Throwable> {
            val psiFile = PsiManager.getInstance(project).findFile(file) as? PsiJavaFile
                ?: return@compute null
            val cls = psiFile.classes.firstOrNull() ?: return@compute null

            ClassInfo(
                name = cls.name ?: return@compute null,
                isInterface = cls.isInterface,
                isEnum = cls.isEnum,
                isAbstract = !cls.isInterface &&
                        cls.hasModifierProperty(PsiModifier.ABSTRACT),
                fieldNames = cls.fields.map { it.name }.toSet(),
                interfaceNames = cls.implementsListTypes
                    .mapNotNull { it.resolve()?.name }
            )
        } ?: return file.nameWithoutExtension

        val cloudMatch = matchCloud(info.name)
        if (cloudMatch != null) return cloudMatch
        return structuralFallback(info)
    }

    private fun matchCloud(className: String): String? {
        val rules = CloudRuleService.getInstance().getRules()
        if (rules.isEmpty()) return null

        return rules.entries
            .sortedByDescending { it.key.length }
            .firstOrNull { (suffix, _) -> className.endsWith(suffix) }
            ?.let { (_, desc) -> "${className}_${desc}" }
    }

    private fun analyzeByName(name: String): String {
        return matchCloud(name) ?: name
    }

    private fun structuralFallback(info: ClassInfo): String {
        val name = info.name

        if (info.isInterface) return "${name}_接口定义"
        if (info.isEnum) return "${name}_枚举常量"
        if (info.isAbstract) return "${name}_抽象基类"
        val hasId = info.fieldNames.any { it == "id" || it.endsWith("Id") }
        val hasTs = info.fieldNames.any {
            it in setOf(
                "createTime", "updateTime", "createdAt", "updatedAt",
                "gmtCreate", "gmtModified"
            )
        }
        if (hasId && hasTs) return "${name}_数据实体"
        if (hasId && info.fieldNames.size >= 3) return "${name}_数据实体"
        val ifaces = info.interfaceNames.joinToString("|")
        if (ifaces.contains("Service")) return "${name}_业务服务"
        if (ifaces.contains("Repository")) return "${name}_数据仓储"
        if (ifaces.contains("Dao")) return "${name}_数据访问"

        return "${name}_核心组件"
    }

    private data class ClassInfo(
        val name: String,
        val isInterface: Boolean,
        val isEnum: Boolean,
        val isAbstract: Boolean,
        val fieldNames: Set<String>,
        val interfaceNames: List<String>
    )
}