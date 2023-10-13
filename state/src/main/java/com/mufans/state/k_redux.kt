package com.mufans.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * @author liuj
 * @created on 2023/8/14
 * @desc 参考Redux实现的状态管理框架。作为Mvi架构原型基础，提供唯一可信数据源、单向数据流的状态更新以及订阅。
 * 其中Action对应Intent, Reducer响应Action更新状态，Middleware实现切面逻辑，
 * StateFlow作为状态的聚合用来进行订阅, singleEvent则是提供单次事件订阅。
 *
 */
class Store<T : State>(
    private val initState: T,
    private val actionToReducers: List<ActionToReducer<T>>,
    private val singleEventReducers: List<SingleEventReducer<T>>? = null,
    private val middlewares: List<Middleware<T>> = emptyList(),
    private val coroutineScope: CoroutineScope = appGlobalScope
) {

    constructor(builder: StoreBuilder<T>) : this(
        builder.initState,
        builder.actionToReducers,
        builder.singleEventReducers,
        builder.middlewares,
        builder.coroutineScope
    )

    /**
     *  Action热流
     */
    private val actionSharedFlow = MutableSharedFlow<Action>()


    /**
     * 当前状态
     */
    val stateFlow: StateFlow<T> =
        actionSharedFlow.middlewares().merge(actionToReducers).handleSingleEvent().catchError()
            .scan(initState) { state, change ->
                return@scan change.reduce(state)
            }.stateIn(coroutineScope, Eagerly, initState)

    /**
     * 事件管道
     */
    private val eventChannel = Channel<Event>()

    /**
     * 单次事件
     */
    val singleEvent: Flow<Event> = eventChannel.receiveAsFlow()

    /**
     * 发送Action
     */
    fun dispatch(action: Action) {
        coroutineScope.launch {
            try {
                actionSharedFlow.emit(action)
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
            }
        }
    }

    /**
     * 中间件处理
     */
    private fun Flow<Action>.middlewares(): Flow<Action> {
        return map { action ->
            var acc = action
            middlewares.forEach {
                acc = it.handle(this@Store, acc)
            }
            acc
        }
    }

    /**
     * 处理单次事件
     */
    private fun Flow<Reducer<T>>.handleSingleEvent(): Flow<Reducer<T>> = onEach { src ->
        singleEventReducers?.apply {
            map { eventReducer ->
                eventReducer(src)?.apply {
                    sendEvent(this)
                }
            }
        }
    }

    private fun sendEvent(event: Event) {
        coroutineScope.launch {
            eventChannel.send(event)
        }
    }

    /**
     * 销毁
     */
    fun destroy() {
        eventChannel.close()
    }

    class StoreBuilder<T : State>(val initState: T) {

        internal val actionToReducers: MutableList<ActionToReducer<T>> = mutableListOf()
        internal val singleEventReducers: MutableList<SingleEventReducer<T>> = mutableListOf()
        internal var middlewares: MutableList<Middleware<T>> = mutableListOf()
        var coroutineScope: CoroutineScope = appGlobalScope

        fun addActionToReducer(actionToReducer: ActionToReducer<T>): StoreBuilder<T> {
            actionToReducers.add(actionToReducer)
            return this
        }

        fun addActionToReducers(atrList: List<ActionToReducer<T>>): StoreBuilder<T> {
            actionToReducers.addAll(atrList)
            return this
        }

        fun addSingleEventReducer(singleEventReducer: SingleEventReducer<T>): StoreBuilder<T> {
            singleEventReducers.add(singleEventReducer)
            return this
        }

        fun addSingleEventReducers(eventReducerList: List<SingleEventReducer<T>>): StoreBuilder<T> {
            singleEventReducers.addAll(eventReducerList)
            return this
        }

        fun addMiddleware(middleware: Middleware<T>): StoreBuilder<T> {
            middlewares.add(middleware)
            return this
        }

        fun addMiddlewares(middlewareList: List<Middleware<T>>): StoreBuilder<T> {
            middlewares.addAll(middlewareList)
            return this
        }

        fun addRouterContract(contract: IRouterContract<T>): StoreBuilder<T> {
            contract.apply {
                addActionToReducer(actionToReducer)
                if (singleEventReducer != null)
                    addSingleEventReducer(singleEventReducer!!)
                if (middleware != null)
                    addMiddleware(middleware!!)
            }
            return this
        }

        fun build(): Store<T> {
            addRouterContract(RouterContract())
            return Store(this)
        }

    }
}


/**
 * State是当前的状态，通常是多个状态的聚合
 */
interface State

/**
 * 一次性事件
 */
interface Event


/**
 *  Action是单一数据流的起点，作为触发状态变化的行为载体
 */
interface Action

/**
 *  Reducer是描述如何更新状态，用于Action对应的逻辑执行完拿到结果后进行状态合并
 */
interface Reducer<T> {
    fun reduce(data: T): T

    class EmptyReducer<T> : Reducer<T> {
        override fun reduce(data: T): T {
            return data
        }
    }
}


/**
 * Middleware在Reducer执行之前进行拦截，通常作为切面或者处理一些异步的逻辑
 */
interface Middleware<T : State> {
    suspend fun handle(store: Store<T>, action: Action): Action
}

/**
 * Action转换Reducer
 */
typealias ActionToReducer<T> = Flow<Action>.() -> Flow<Reducer<T>>

typealias ActionToReducer2<A, T> = Flow<A>.() -> Flow<Reducer<T>>

inline fun <reified A : Action, T> toReducer(
    crossinline call: ActionToReducer2<A, T>
): ActionToReducer<T> {
    return {
        filterIsInstance<A>().call()
    }
}

typealias FlowAction = Flow<Action>

typealias FlowReducer<T> = Flow<Reducer<T>>

typealias SingleEventReducer<T> = (Reducer<T>) -> Event?


/**
 * 组合多个Reducer
 */
fun <T> Flow<Action>.merge(actionToReducers: List<ActionToReducer<T>>): Flow<Reducer<T>> {
    val totalReducers = actionToReducers.map { it() }.toList()
    return merge(*totalReducers.toTypedArray())
}

/**
 * 捕获异常
 */
fun <T> Flow<Reducer<T>>.catchError(): Flow<Reducer<T>> = retryWhen { throwable, _ ->
    emit(Reducer.EmptyReducer())
    true
}


inline fun <T, V> Flow<T>.distinctMap(crossinline transform: suspend (value: T) -> V): Flow<V> =
    map(transform).distinctUntilChanged()
