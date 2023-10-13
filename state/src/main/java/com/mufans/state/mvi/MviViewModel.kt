package com.mufans.state.mvi

import androidx.lifecycle.ViewModel
import com.mufans.state.Action
import com.mufans.state.Event
import com.mufans.state.State
import com.mufans.state.Store
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * @author mufans
 * @created on 2023/10/3
 * @desc desc
 *
 */
abstract class MviViewModel<S : MviState, I : MviIntent, E : MviEvent> : ViewModel() {

    // 初始状态
    abstract val initState: S

    // store
    val store by lazy { buildStore(initState) }

    // 状态流
    val stateFlow: StateFlow<S> = store.stateFlow

    // 单次事件
    @Suppress("UNCHECKED_CAST")
    val singleEvent: Flow<E> = store.singleEvent as Flow<E>

    abstract fun buildStore(initState: S): Store<S>

    /**
     * 发送意图
     */
    fun dispatch(intent: I) {
        store.dispatch(intent)
    }

    override fun onCleared() {
        store.destroy()
        super.onCleared()
    }


}


// 意图
interface MviIntent : Action

// 状态
interface MviState : State

// 事件
interface MviEvent : Event