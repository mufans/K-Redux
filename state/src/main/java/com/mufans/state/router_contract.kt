package com.mufans.state

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.lang.ref.WeakReference

/**
 * @author mufans
 * @created on 2023/8/23
 * @desc 路由协议
 *
 */

sealed interface Router : Action {
    class RouterAction(ctx: Context, val intent: Intent) : Router {
        val contextRef = WeakReference(ctx)
    }
}

sealed interface RouterReducer<T> : Reducer<T> {
    override fun reduce(data: T): T {
        return data
    }

    class ReducerSelector<T>(val contextRef: WeakReference<Context>, val intent: Intent) :
        RouterReducer<T>
}

sealed interface RouterEvent : Event {
    class Result(
        val contextRef: WeakReference<Context>,
        val intent: Intent,
        val success: Boolean = true,
    ) : Event
}

internal fun <T> Flow<Router.RouterAction>.toRouterReducer(): Flow<RouterReducer.ReducerSelector<T>> =
    map { RouterReducer.ReducerSelector(it.contextRef, it.intent) }


class RouterMiddleware<T : State> : Middleware<T> {
    override suspend fun handle(store: Store<T>, action: Action): Action {
        return action
    }
}

class RouterContract<T : State> : IRouterContract<T> {
    override val actionToReducer: Flow<Action>.() -> Flow<Reducer<T>> =
        toReducer(Flow<Router.RouterAction>::toRouterReducer)

    override val middleware: Middleware<T> = RouterMiddleware()

    override val singleEventReducer: SingleEventReducer<T> = {
        if (it is RouterReducer.ReducerSelector) {
            it.contextRef.get()?.startActivity(it.intent)
            RouterEvent.Result(it.contextRef, it.intent)
        } else null
    }
}

interface IRouterContract<T : State> {

    val actionToReducer: Flow<Action>.() -> Flow<Reducer<T>>

    val middleware: Middleware<T>?

    val singleEventReducer: SingleEventReducer<T>?
}