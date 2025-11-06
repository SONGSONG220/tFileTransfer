package com.tans.tfiletransporter.ui


import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update


interface CoroutineState<State : Any> {

    val stateFlow: MutableStateFlow<State>

    fun state(): State = stateFlow.value

    suspend fun updateState(update: suspend (oldState: State) -> State): State {
        stateFlow.update { update(it) }
        return stateFlow.value
    }

    fun stateFlow(): Flow<State> = stateFlow

}

fun <State : Any> CoroutineState(defaultState: State): CoroutineState<State> {
    return object : CoroutineState<State> {

        override val stateFlow: MutableStateFlow<State> = MutableStateFlow(defaultState)
    }
}