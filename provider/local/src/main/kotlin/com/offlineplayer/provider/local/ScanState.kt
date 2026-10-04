package com.offlineplayer.provider.local

sealed interface ScanState {
    data object Idle : ScanState
    data class Scanning(val message: String) : ScanState
    data class Error(val message: String) : ScanState
}
