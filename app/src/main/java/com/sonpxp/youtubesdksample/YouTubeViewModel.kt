package com.sonpxp.youtubesdksample

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class YouTubeViewModel @Inject constructor() : ViewModel() {

    private val _videoId = MutableStateFlow<String?>(null)
    val videoId: StateFlow<String?> = _videoId.asStateFlow()

    private val _currentSecond = MutableStateFlow(0f)
    val currentSecond: StateFlow<Float> = _currentSecond.asStateFlow()

    fun loadVideo(videoId: String) {
        _videoId.value = videoId
    }

    fun updateCurrentSecond(second: Float) {
        _currentSecond.value = second
    }
}