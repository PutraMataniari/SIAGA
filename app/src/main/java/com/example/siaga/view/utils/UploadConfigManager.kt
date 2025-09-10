package com.example.siaga.view.utils

object UploadConfigManager {
    var maxUploadSize: Long = 5 * 1024 * 1024 // Default 5MB
    var allowedImageTypes = listOf("jpg", "jpeg", "png")
    var allowedDocTypes = listOf("jpg", "jpeg", "png", "pdf", "doc", "docx")
}
