package com.offlineplayer.core.playback

import com.offlineplayer.core.model.AudioDiagnostics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioDiagnosticsReporter @Inject constructor() {
    private val _diagnostics = MutableStateFlow<AudioDiagnostics?>(null)
    val diagnostics: StateFlow<AudioDiagnostics?> = _diagnostics.asStateFlow()

    fun report(diagnostics: AudioDiagnostics?) {
        _diagnostics.value = diagnostics
    }
}
