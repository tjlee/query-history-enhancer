package com.github.tjlee.queryhistoryenhancer

import com.intellij.database.datagrid.DataAuditor
import com.intellij.database.datagrid.DataRequest
import com.intellij.database.run.ConsoleDataRequest

class QueryTimestampAuditor : DataAuditor {

    override fun afterStatement(context: DataRequest.Context) {
        val request = context.request
        if (request !is ConsoleDataRequest) return
        val query = request.query?.trim()?.takeIf { it.isNotEmpty() } ?: return
        val project = request.console.project
        val source = runCatching { request.console.dataSource.name }.getOrDefault("")
        QueryTimestampService.getInstance(project).record(query, source)
    }
}
