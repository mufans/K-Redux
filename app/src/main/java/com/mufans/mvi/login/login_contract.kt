package com.mufans.mvi.login

import com.mufans.state.Reducer
import com.mufans.state.mvi.MviEvent
import com.mufans.state.mvi.MviIntent
import com.mufans.state.mvi.MviState

/**
 * @author liuj
 * @created on 2023/10/4
 * @desc desc
 *
 */

data class LoginState(val name: String? = null, val token: String? = null) : MviState

sealed interface LoginIntent : MviIntent {
    class RequestLogin(val name: String, val pass: String) : LoginIntent

    object Logout : LoginIntent
}


sealed interface LoginEvent : MviEvent {
    object Success : LoginEvent

    class Failure(val message: String) : LoginEvent
}

sealed interface LoginReducer : Reducer<LoginState> {
    class Success(val name: String, val token: String) : LoginReducer {
        override fun reduce(data: LoginState): LoginState {
            return data.copy(name = name, token = token)
        }
    }

    class Failure(val message: String) : LoginReducer {
        override fun reduce(data: LoginState): LoginState {
            return data.copy(name = null, token = null)
        }
    }

    object Logout : LoginReducer {
        override fun reduce(data: LoginState): LoginState {
            return data.copy(name = null, token = null)
        }
    }
}
