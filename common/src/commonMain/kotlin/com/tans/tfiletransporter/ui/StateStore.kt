package com.tans.tfiletransporter.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import com.tans.tfiletransporter.ioDispatcher
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch


@Suppress("UNCHECKED_CAST")
open class StateStore<S : Any>(state: S) : ViewModel(),
    CoroutineState<S> by CoroutineState(state), CoroutineScope by CoroutineScope(ioDispatcher()) {

    private val actionEvents: MutableSharedFlow<Action<S>> =
        MutableSharedFlow(extraBufferCapacity = Int.MAX_VALUE)

    private val executedActionsCount = atomic(0)

    private val simpleData: MutableMap<String, Any> = mutableMapOf()

    init {
        launch {
            actionEvents.collect { action ->
                action.preExecute()
                updateState { action.execute(it) }
                action.postExecute()
                executedActionsCount.addAndGet(1)
            }
        }
    }

    fun enqueueAction(action: Action<S>) {
        actionEvents.tryEmit(action)
    }

    fun enqueueAction(action: Action<S>, pre: suspend () -> Unit = {}, post: suspend () -> Unit) {
        actionEvents.tryEmit(object : Action<S>() {

            override suspend fun preExecute() {
                pre()
            }

            override suspend fun execute(oldState: S): S {
                return action.execute(oldState)
            }

            override suspend fun postExecute() {
                post()
            }

        })
    }

    fun executeActionsCount(): Int = executedActionsCount.value

    @Composable
    inline fun <T : Any> getOrSaveSimpleData(key: String, createNew: @Composable () -> T): T {

        val old = getSimpleData<T>(key)
        if (old != null) {
            return old
        }
        val new = createNew()
        saveSimpleData(key, new)
        return new
    }

    fun <T> getSimpleData(key: String): T? = simpleData[key] as? T

    fun saveSimpleData(key: String, value: Any) {
        simpleData[key] = value
    }

    override fun onCleared() {
        super.onCleared()
        cancel("ViewModel cleared")
        simpleData.clear()
    }

}

abstract class Action<State : Any> {

    open suspend fun preExecute() {

    }

    abstract suspend fun execute(oldState: State): State

    open suspend fun postExecute() {

    }
}