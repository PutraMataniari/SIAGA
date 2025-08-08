package com.example.siaga.view.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfilViewModel : ViewModel() {
    private val _profileImageUri = MutableStateFlow<Uri?>(null)
    val profileImageUri: StateFlow<Uri?> get() = _profileImageUri

    fun updateProfilImage(uri: Uri?) {
        viewModelScope.launch {
            _profileImageUri.emit(uri)
        }
    }
}

