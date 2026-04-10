package com.github.tjlee.queryhistoryenhancer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actions.ContentChooser
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class QueryHistoryBrowseAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val service = QueryTimestampService.getInstance(project)

        val entries = service.entries()
        if (entries.isEmpty()) return

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        val chooser = object : ContentChooser<Pair<String, Long>>(project, "Query History", true, true) {
            override fun getContents(): List<Pair<String, Long>> = service.entries()

            override fun getStringRepresentationFor(content: Pair<String, Long>): String {
                val formatted = formatter.format(Instant.ofEpochMilli(content.second).atZone(ZoneId.systemDefault()))
                return "[$formatted] ${content.first}"
            }

            override fun removeContentAt(content: Pair<String, Long>) {
                service.remove(content.first)
            }

            override fun dispose() {
                if (exitCode == OK_EXIT_CODE) {
                    val rawQuery = selectedContents.map { it.first }.joinToString("\n")
                    WriteCommandAction.writeCommandAction(project).run<Throwable> {
                        editor.document.setText(rawQuery)
                        editor.caretModel.moveToOffset(editor.document.textLength)
                    }
                }
                super.dispose()
            }
        }

        chooser.setContentIcon(null)
        chooser.setSplitterOrientation(false)
        chooser.setSelectedIndex(0)
        chooser.isModal = false
        chooser.show()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null && e.getData(CommonDataKeys.EDITOR) != null
    }
}
