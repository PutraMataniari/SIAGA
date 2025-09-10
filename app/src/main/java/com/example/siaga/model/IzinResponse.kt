package com.example.siaga.model

import com.example.siaga.view.model.HistoryResponse

data class IzinResponse(
    val status: Boolean,
    val message: String,
    val data: HistoryResponse?
)


