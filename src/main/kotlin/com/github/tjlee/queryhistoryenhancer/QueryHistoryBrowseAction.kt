package com.github.tjlee.queryhistoryenhancer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionListModel
import com.intellij.ui.LanguageTextField
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.speedSearch.ListWithFilter
import com.intellij.ui.speedSearch.SpeedSearchSupply
import com.intellij.sql.psi.SqlLanguage
import com.intellij.util.Function
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.UIManager

private enum class DateRange(val label: String) {
    TODAY("Today"),
    WEEK("Last 7 days"),
    MONTH("Last 30 days"),
    ALL("All time");

    fun matches(ts: Long): Boolean {
        if (ts <= QueryTimestampService.IMPORTED_TS) return this == ALL
        val cutoff = when (this) {
            TODAY -> LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            WEEK  -> Instant.now().minus(7, ChronoUnit.DAYS).toEpochMilli()
            MONTH -> Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli()
            ALL   -> 0L
        }
        return ts >= cutoff
    }
}

class QueryHistoryBrowseAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val service = QueryTimestampService.getInstance(project)

        val entries = service.entries()
        if (entries.isEmpty()) return

        val dialog = QueryHistoryDialog(project, entries, service::remove)
        if (dialog.showAndGet()) {
            val rawQuery = dialog.selectedEntries.joinToString("\n") { it.first }
            WriteCommandAction.writeCommandAction(project).run<Throwable> {
                editor.document.setText(rawQuery)
                editor.caretModel.moveToOffset(editor.document.textLength)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null && e.getData(CommonDataKeys.EDITOR) != null
    }
}

private class QueryHistoryDialog(
    private val project: Project,
    entries: List<Pair<String, Long>>,
    private val onRemove: (String) -> Unit
) : DialogWrapper(project, true) {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val allEntries: MutableList<Pair<String, Long>> = entries.toMutableList()
    private val model = CollectionListModel(allEntries)
    private val list = JBList(model).apply {
        selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        setCellRenderer { _, value, _, isSelected, cellHasFocus ->
            SimpleColoredComponent().also { c ->
                val (query, ts) = value
                val timeLabel = if (ts <= QueryTimestampService.IMPORTED_TS) "imported"
                                else formatter.format(Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()))
                val firstLine = query.lines().first().trim()
                c.append("[$timeLabel] ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                c.append(firstLine)
                if (isSelected) {
                    c.background = UIManager.getColor(
                        if (cellHasFocus) "List.selectionBackground" else "List.selectionInactiveBackground"
                    )
                    c.isOpaque = true
                }
            }
        }
    }

    val selectedEntries: List<Pair<String, Long>>
        get() = list.selectedValuesList

    init {
        title = "Query History"
        init()
    }

    private fun applyDateFilter(range: DateRange) {
        model.replaceAll(allEntries.filter { (_, ts) -> range.matches(ts) })
    }

    override fun createCenterPanel(): JComponent {
        val preview = LanguageTextField(SqlLanguage.INSTANCE, project, "", false).apply {
            setViewer(true)
        }

        list.addListSelectionListener {
            preview.text = list.selectedValue?.first ?: ""
        }

        list.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode != KeyEvent.VK_DELETE) return
                if (SpeedSearchSupply.getSupply(list)?.isPopupActive == true) return
                val selected = list.selectedValuesList.toList()
                if (selected.isEmpty()) return
                selected.forEach { entry ->
                    onRemove(entry.first)
                    allEntries.remove(entry)
                    model.remove(entry)
                }
                e.consume()
            }
        })

        val listWithFilter = ListWithFilter.wrap(
            list,
            ScrollPaneFactory.createScrollPane(list),
            Function<Pair<String, Long>, String> { (query, ts) ->
                val timeLabel = if (ts <= QueryTimestampService.IMPORTED_TS) "imported"
                                else formatter.format(Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()))
                "[$timeLabel] ${query.lines().first().trim()}"
            }
        )

        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT).apply {
            topComponent = listWithFilter
            bottomComponent = preview
            resizeWeight = 0.6
        }
        SwingUtilities.invokeLater { splitPane.setDividerLocation(0.6) }

        val dateFilter = ComboBox(DateRange.entries.toTypedArray()).apply {
            renderer = SimpleListCellRenderer.create("") { it.label }
            selectedItem = DateRange.ALL
            addActionListener { applyDateFilter(selectedItem as DateRange) }
        }

        val topBar = JPanel(FlowLayout(FlowLayout.LEFT, 8, 4)).apply {
            add(JLabel("Date:"))
            add(dateFilter)
        }

        val content = JPanel(BorderLayout()).apply {
            add(topBar, BorderLayout.NORTH)
            add(splitPane, BorderLayout.CENTER)
        }
        content.preferredSize = Dimension(700, 540)
        return content
    }

    override fun getDimensionServiceKey() = "QueryHistoryDialog"
}
