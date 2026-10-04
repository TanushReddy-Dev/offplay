package com.offlineplayer.core.playback.di

import com.offlineplayer.core.playback.PlayerController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for the playback layer.
 *
 * [PlayerController] is constructor-injected with `@Singleton`,
 * so it doesn't need an explicit `@Provides`. This module exists
 * for any future factory bindings (e.g. an `AnalyticsListener`
 * delegate, playback-cache configuration, etc.).
 */
@Module
@InstallIn(SingletonComponent::class)
object PlaybackHiltModule
