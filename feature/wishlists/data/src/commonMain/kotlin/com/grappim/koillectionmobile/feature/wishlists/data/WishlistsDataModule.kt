package com.grappim.koillectionmobile.feature.wishlists.data

import com.grappim.koillectionmobile.feature.wishlists.domain.WishlistChanges
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/** The domain classes have no Koin annotations (`feature:wishlists:domain` has no DI plugin), so they are bound here. */
@Module
@Configuration
@ComponentScan("com.grappim.koillectionmobile.feature.wishlists.data")
class WishlistsDataModule {

    @Single
    fun provideWishlistChanges(): WishlistChanges = WishlistChanges()
}
