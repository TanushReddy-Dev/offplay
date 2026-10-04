package com.offlineplayer.provider.local

import com.offlineplayer.core.provider.api.CatalogProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocalProviderModule {

    @Binds
    @Singleton
    abstract fun bindCatalogProvider(
        localProvider: LocalProvider
    ): CatalogProvider
}
