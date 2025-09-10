package com.example.siaga.model

data class LoginResponse(
    val status: Boolean,
    val message: String,
    val token: String,
    val data: User
)

data class User(
    val id: Int,
    val name: String,
    val nama: String,
    val email: String
)
