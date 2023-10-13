package com.mufans.mvi.login

import com.mufans.mvi.data.MockUserRepository
import com.mufans.state.Event
import com.mufans.state.FlowAction
import com.mufans.state.Reducer
import com.mufans.state.Store
import com.mufans.state.mvi.MviViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

/**
 * @author liuj
 * @created on 2023/10/4
 * @desc desc
 *
 */
class LoginViewModel : MviViewModel<LoginState, LoginIntent, LoginEvent>() {
    override val initState: LoginState
        get() = LoginState()


    override fun buildStore(initState: LoginState): Store<LoginState> {
        return Store.StoreBuilder(initState).addActionToReducer(createActionToReducer())
            .addSingleEventReducer(::reducerToEvent).build()
    }


    private fun reducerToEvent(reducer: Reducer<LoginState>): Event? {
        return when (reducer) {
            is LoginReducer.Success -> LoginEvent.Success
            is LoginReducer.Failure -> LoginEvent.Failure(reducer.message)
            else -> null
        }
    }

    private fun createActionToReducer(): FlowAction.() -> Flow<Reducer<LoginState>> {
        return {
            merge(loginAction(), logoutAction())
        }
    }
}

private fun FlowAction.logoutAction() = filterIsInstance<LoginIntent.Logout>().map {
    LoginReducer.Logout
}

private fun FlowAction.loginAction() = filterIsInstance<LoginIntent.RequestLogin>().map {
    val resp = MockUserRepository.login(it.name, it.pass)
    if (resp.success) {
        LoginReducer.Success(it.name, resp.token ?: "")
    } else {
        LoginReducer.Failure(resp.message ?: "")
    }
}


