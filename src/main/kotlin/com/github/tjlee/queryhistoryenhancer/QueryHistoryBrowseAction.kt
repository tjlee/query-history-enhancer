package com.github.tjlee.queryhistoryenhancer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBList
import com.intellij.ui.speedSearch.ListWithFilter
import com.intellij.sql.psi.SqlLanguage
import com.intellij.ui.speedSearch.SpeedSearchSupply
import com.intellij.util.Function
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.JComponent
import javax.swing.JSplitPane
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.UIManager

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
    private val model = CollectionListModel(entries)
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
                // If speed search filter is active, let it handle the key (backspace clears chars)
                if (SpeedSearchSupply.getSupply(list)?.isPopupActive == true) return
                val selected = list.selectedValuesList.toList()
                if (selected.isEmpty()) return
                selected.forEach { entry ->
                    onRemove(entry.first)
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

        splitPane.preferredSize = Dimension(700, 500)
        return splitPane
    }

    override fun getDimensionServiceKey() = "QueryHistoryDialog"
}
