package com.github.tjlee.queryhistoryenhancer

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.io.File

private const val HISTORY_DELIMITER = "-- -. . -..- - / . -. - .-. -.--"

class QueryHistoryImportActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val service = QueryTimestampService.getInstance(project)
        if (service.state.hasImportedPlatformHistory) return

        val historyDir = File(PathManager.getConfigPath(), "consoles/.history/db")
        if (historyDir.isDirectory) {
            historyDir.listFiles { f -> f.extension == "sql" }
                ?.sortedBy { it.lastModified() }  // oldest files first so newest queries survive cap eviction
                ?.forEach { file ->
                    runCatching { file.readText() }.getOrNull()
                        ?.split(HISTORY_DELIMITER)
                        ?.map { it.trim() }
                        ?.filter { it.isNotBlank() }
                        ?.forEach { query -> service.recordImported(query) }
                }
        }

        service.state.hasImportedPlatformHistory = true
    }
}
