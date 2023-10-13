package com.mufans.mvi.data.model

/**
 * @author mufans
 * @created on 2023/10/13
 * @desc desc
 *
 */
data class LoginResp(val success: Boolean, val message: String? = null, val token: String? = null)
