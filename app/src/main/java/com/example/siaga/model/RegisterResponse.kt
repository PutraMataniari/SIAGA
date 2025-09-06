package com.example.siaga.model

data class RegisterResponse(
    val status: Boolean,
    val message: String,
    val data: Pegawai?,
    val foto_url: String?
)