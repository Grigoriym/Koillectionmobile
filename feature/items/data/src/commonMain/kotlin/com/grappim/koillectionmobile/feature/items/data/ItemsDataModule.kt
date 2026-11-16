package com.grappim.koillectionmobile.feature.items.data

import com.grappim.koillectionmobile.feature.items.domain.AddItemUseCase
import com.grappim.koillectionmobile.feature.items.domain.AddItemUseCaseImpl
import com.grappim.koillectionmobile.feature.items.domain.EditItemUseCase
import com.grappim.koillectionmobile.feature.items.domain.EditItemUseCaseImpl
import com.grappim.koillectionmobile.feature.items.domain.ItemChanges
import com.grappim.koillectionmobile.feature.items.domain.ItemListChanges
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
import com.grappim.koillectionmobile.feature.tags.domain.TagsRepository
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/** The domain classes have no Koin annotations (`feature:items:domain` has no DI plugin), so they are bound here. */
@Module
@Configuration
@ComponentScan("com.grappim.koillectionmobile.feature.items.data")
class ItemsDataModule {

    @Single
    fun provideAddItemUseCase(itemsRepository: ItemsRepository, tagsRepository: TagsRepository): AddItemUseCase =
        AddItemUseCaseImpl(itemsRepository, tagsRepository)

    @Single
    fun provideEditItemUseCase(itemsRepository: ItemsRepository, tagsRepository: TagsRepository): EditItemUseCase =
        EditItemUseCaseImpl(itemsRepository, tagsRepository)

    @Single
    fun provideItemListChanges(): ItemListChanges = ItemListChanges()

    @Single
    fun provideItemChanges(): ItemChanges = ItemChanges()
}
