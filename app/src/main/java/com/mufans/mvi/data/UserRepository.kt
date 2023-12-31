package com.mufans.mvi.data

import com.mufans.mvi.data.model.LoginResp
import kotlinx.coroutines.delay

/**
 * @author mufans
 * @created on 2023/10/13
 * @desc desc
 *
 */
interface UserRepository {
    suspend fun login(userName: String, pwd: String): LoginResp

    suspend fun logout()
}

object MockUserRepository : UserRepository {
    override suspend fun login(userName: String, pwd: String): LoginResp {
        delay(500)
        return if (userName == "admin" && pwd == "admin") {
            LoginResp(true, "success", "token")
        } else {
            LoginResp(false, "login failed")
        }
    }

    override suspend fun logout() {
        delay(10)
    }
}