package com.grappim.koillectionmobile.feature.albums.data

import com.grappim.koillectionmobile.feature.albums.domain.AlbumChanges
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/** The domain classes have no Koin annotations (`feature:albums:domain` has no DI plugin), so they are bound here. */
@Module
@Configuration
@ComponentScan("com.grappim.koillectionmobile.feature.albums.data")
class AlbumsDataModule {

    @Single
    fun provideAlbumChanges(): AlbumChanges = AlbumChanges()
}
