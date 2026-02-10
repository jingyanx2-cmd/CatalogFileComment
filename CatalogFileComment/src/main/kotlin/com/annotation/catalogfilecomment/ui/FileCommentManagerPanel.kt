// src/main/kotlin/com/annotation/catalogfilecomment/ui/FileCommentManagerPanel.kt
package com.annotation.catalogfilecomment.ui

import com.annotation.catalogfilecomment.config.FileCommentConfigService
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import javax.swing.table.DefaultTableModel

/**
 * File comment management panel for settings page
 */
class FileCommentManagerPanel : JPanel(BorderLayout()) {

    private val tableModel: DefaultTableModel
    private val table: JBTable
    // Search field setup
    private val searchField: JBTextField = JBTextField().apply {
        emptyText.text = "Search comments..."
        preferredSize = Dimension(300, 30)
    }

    private var originalData: Map<String, String> = emptyMap()

    init {

        val topPanel = JPanel(BorderLayout())
        topPanel.add(JLabel("Search: "), BorderLayout.WEST)
        topPanel.add(searchField, BorderLayout.CENTER)

        // Table setup
        val columnNames = arrayOf("File Path", "Comment")
        tableModel = object : DefaultTableModel(columnNames, 0) {
            override fun isCellEditable(row: Int, column: Int): Boolean = true
        }

        table = JBTable(tableModel)
        table.preferredScrollableViewportSize = Dimension(600, 400)
        table.autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS

        // Buttons
        val buttonPanel = JPanel()
        val addButton = JButton("Add New").apply {
            addActionListener { addNewRow() }
        }
        val removeButton = JButton("Remove Selected").apply {
            addActionListener { removeSelectedRows() }
        }
        val importButton = JButton("Import").apply {
            addActionListener { importFromClipboard() }
        }
        val exportButton = JButton("Export").apply {
            addActionListener { exportToClipboard() }
        }
        // Statistics button
        val statsButton = JButton("Statistics").apply {
            addActionListener { showCommentCount() }
        }

        buttonPanel.add(addButton)
        buttonPanel.add(removeButton)
        buttonPanel.add(Box.createHorizontalStrut(20))
        buttonPanel.add(importButton)
        buttonPanel.add(exportButton)
        buttonPanel.add(Box.createHorizontalStrut(20))
        buttonPanel.add(statsButton)

        // Assemble layout
        add(topPanel, BorderLayout.NORTH)
        add(JBScrollPane(table), BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.SOUTH)

        // Load data
        loadData()

        // Search listener using filterData method
        searchField.addActionListener {
            val keyword = searchField.text
            // 【修复】使用 filterData 方法进行本地过滤
            filterData(keyword)
        }
    }

    /**
     * Show comment statistics using FileCommentConfigService.getCommentCount()
     */
    private fun showCommentCount() {
        val count = FileCommentConfigService.getInstance().getCommentCount()
        JOptionPane.showMessageDialog(
            this,
            "Total comments: $count",
            "Statistics",
            JOptionPane.INFORMATION_MESSAGE
        )
    }

    private fun loadData() {
        originalData = FileCommentConfigService.getInstance().getAllComments()
        refreshTable(originalData)
    }

    private fun refreshTable(data: Map<String, String>) {
        tableModel.rowCount = 0
        data.forEach { (path, comment) ->
            tableModel.addRow(arrayOf(path, comment))
        }
    }

    /**
     * Filter data by keyword - used for local filtering in search
     */
    private fun filterData(keyword: String) {
        if (keyword.isBlank()) {
            refreshTable(originalData)
            return
        }

        val filtered = originalData.filter { (path, comment) ->
            path.contains(keyword, ignoreCase = true) ||
                    comment.contains(keyword, ignoreCase = true)
        }
        refreshTable(filtered)
    }

    private fun addNewRow() {
        tableModel.addRow(arrayOf("", ""))
    }

    private fun removeSelectedRows() {
        val selectedRows = table.selectedRows.sortedDescending()
        selectedRows.forEach { row ->
            tableModel.removeRow(row)
        }
    }

    /**
     * Import data from clipboard in JSON format
     */
    private fun importFromClipboard() {
        val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
        val data = clipboard.getData(java.awt.datatransfer.DataFlavor.stringFlavor) as? String ?: return

        try {
            val json = com.google.gson.JsonParser.parseString(data).asJsonObject
            json.entrySet().forEach { (key, value) ->
                tableModel.addRow(arrayOf(key, value.asString))
            }
        } catch (e: Exception) {
            // Log error and show message
            println("Import failed: ${e.message}")
            JOptionPane.showMessageDialog(
                this,
                "Invalid JSON format: ${e.message}",
                "Import Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    /**
     * Export data to clipboard as JSON
     */
    private fun exportToClipboard() {
        val data = mutableMapOf<String, String>()
        for (i in 0 until tableModel.rowCount) {
            val path = tableModel.getValueAt(i, 0) as? String ?: continue
            val comment = tableModel.getValueAt(i, 1) as? String ?: continue
            if (path.isNotBlank()) {
                data[path] = comment
            }
        }

        val json = com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(data)
        val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(java.awt.datatransfer.StringSelection(json), null)

        JOptionPane.showMessageDialog(this, "Exported to clipboard!", "Success", JOptionPane.INFORMATION_MESSAGE)
    }

    /**
     * Check if data has been modified
     */
    fun isModified(): Boolean {
        // Simple comparison, should do detailed comparison in production
        return true
    }

    /**
     * Apply changes to configuration
     */
    fun apply() {
        val newData = mutableMapOf<String, String>()
        for (i in 0 until tableModel.rowCount) {
            val path = tableModel.getValueAt(i, 0) as? String ?: continue
            val comment = tableModel.getValueAt(i, 1) as? String ?: continue
            if (path.isNotBlank() && comment.isNotBlank()) {
                newData[path] = comment
            }
        }

        FileCommentConfigService.getInstance().setAllComments(newData)
        originalData = newData.toMap()
    }

    /**
     * Reset to original data
     */
    fun reset() {
        loadData()
    }
}