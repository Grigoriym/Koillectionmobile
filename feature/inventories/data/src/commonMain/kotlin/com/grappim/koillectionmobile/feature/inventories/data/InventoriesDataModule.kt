package com.grappim.koillectionmobile.feature.inventories.data

import com.grappim.koillectionmobile.feature.inventories.domain.InventoryChanges
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/** The domain classes have no Koin annotations (`feature:inventories:domain` has no DI plugin), so they are bound here. */
@Module
@Configuration
@ComponentScan("com.grappim.koillectionmobile.feature.inventories.data")
class InventoriesDataModule {

    @Single
    fun provideInventoryChanges(): InventoryChanges = InventoryChanges()
}
