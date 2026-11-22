package com.grappim.koillectionmobile.feature.loans.data

import com.grappim.koillectionmobile.feature.loans.domain.LoanChanges
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/** The domain classes have no Koin annotations (`feature:loans:domain` has no DI plugin), so they are bound here. */
@Module
@Configuration
@ComponentScan("com.grappim.koillectionmobile.feature.loans.data")
class LoansDataModule {

    @Single
    fun provideLoanChanges(): LoanChanges = LoanChanges()
}
