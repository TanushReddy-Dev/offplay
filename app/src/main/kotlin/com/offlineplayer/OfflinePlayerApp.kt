package com.offlineplayer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Offline Player.
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation.
 */
@HiltAndroidApp
class OfflinePlayerApp : Application()
