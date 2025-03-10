package com.ppojin.hello

class AuthToken (
    val id_token: String,
    val access_token: String,
    val refresh_token: String,
    val expires_in: Int,
    val token_type: String,
)