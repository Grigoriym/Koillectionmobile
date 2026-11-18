package com.grappim.koillectionmobile.feature.templates.data

import com.grappim.koillectionmobile.feature.templates.domain.SaveTemplateUseCase
import com.grappim.koillectionmobile.feature.templates.domain.SaveTemplateUseCaseImpl
import com.grappim.koillectionmobile.feature.templates.domain.TemplateChanges
import com.grappim.koillectionmobile.feature.templates.domain.TemplatesRepository
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/** The domain classes have no Koin annotations (`feature:templates:domain` has no DI plugin), so they are bound here. */
@Module
@Configuration
@ComponentScan("com.grappim.koillectionmobile.feature.templates.data")
class TemplatesDataModule {

    @Single
    fun provideTemplateChanges(): TemplateChanges = TemplateChanges()

    @Single
    fun provideSaveTemplateUseCase(templatesRepository: TemplatesRepository): SaveTemplateUseCase =
        SaveTemplateUseCaseImpl(templatesRepository)
}
