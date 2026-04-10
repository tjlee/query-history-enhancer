package com.github.tjlee.queryhistoryenhancer

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(name = "QueryTimestampService", storages = [Storage("queryTimestamps.xml")])
class QueryTimestampService : PersistentStateComponent<QueryTimestampService.State> {

    companion object {
        private const val MAX_SIZE = 200

        fun getInstance(project: Project): QueryTimestampService = project.service()
    }

    class State {
        var entries: LinkedHashMap<String, Long> = LinkedHashMap()
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    fun record(query: String) {
        val alreadyPresent = myState.entries.remove(query) != null
        if (!alreadyPresent && myState.entries.size >= MAX_SIZE) {
            myState.entries.remove(myState.entries.keys.first())
        }
        myState.entries[query] = System.currentTimeMillis()
    }

    fun entries(): List<Pair<String, Long>> =
        myState.entries.entries.map { it.key to it.value }.reversed()

    fun remove(query: String) {
        myState.entries.remove(query)
    }
}
