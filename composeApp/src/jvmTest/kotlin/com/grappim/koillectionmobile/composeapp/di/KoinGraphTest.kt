package com.grappim.koillectionmobile.composeapp.di

import androidx.lifecycle.SavedStateHandle
import com.grappim.kit.customheaders.CustomHeaderValidator
import com.grappim.koillectionmobile.core.photos.PhotoFiles
import com.grappim.koillectionmobile.feature.albums.domain.AlbumChanges
import com.grappim.koillectionmobile.feature.albums.domain.AlbumsRepository
import com.grappim.koillectionmobile.feature.albums.domain.PhotoChanges
import com.grappim.koillectionmobile.feature.auth.domain.AuthRepository
import com.grappim.koillectionmobile.feature.collections.domain.CollectionsRepository
import com.grappim.koillectionmobile.feature.inventories.domain.InventoriesRepository
import com.grappim.koillectionmobile.feature.items.domain.AddItemUseCase
import com.grappim.koillectionmobile.feature.items.domain.ItemListChanges
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
import com.grappim.koillectionmobile.feature.loans.domain.LoansRepository
import com.grappim.koillectionmobile.feature.logs.domain.LogsRepository
import com.grappim.koillectionmobile.feature.tags.domain.TagsRepository
import com.grappim.koillectionmobile.feature.templates.domain.ChoiceListChanges
import com.grappim.koillectionmobile.feature.templates.domain.SaveTemplateUseCase
import com.grappim.koillectionmobile.feature.templates.domain.TemplateChanges
import com.grappim.koillectionmobile.feature.templates.domain.TemplatesRepository
import com.grappim.koillectionmobile.feature.wishlists.domain.WishChanges
import com.grappim.koillectionmobile.feature.wishlists.domain.WishlistChanges
import com.grappim.koillectionmobile.feature.wishlists.domain.WishlistsRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import org.koin.core.context.stopKoin
import org.koin.plugin.module.dsl.startKoin
import org.koin.test.verify.verify
import kotlin.test.Test

/**
 * A missing Koin definition is otherwise a launch-time crash that no gate catches: a
 * `@ComponentScan` that misses a class, or an `AppModule` that forgets an `includes` line, both
 * compile.
 *
 * `verify()` checks every definition's constructor by reflection and fails on a parameter type
 * the module set can't supply, without instantiating anything. It is JVM-only, so this test
 * lives in `jvmTest`, not `commonTest`.
 *
 * Limit: it does not check a provider function's own parameters, only the bound type's
 * constructor (deskmate's M2.4 finding). A provider that returns an interface passes with any
 * parameter unbound. The second test resolves the real JVM graph to cover that.
 */
class KoinGraphTest {
    @Test
    fun `every definition in the app graph can be resolved`() {
        AppModule().module().verify(extraTypes = EXTERNALLY_SUPPLIED)
    }

    /**
     * Covers the limit above for the provider chain `verify()` cannot see: `HttpClient` needs
     * `BaseUrlProvider`, `SessionStorage` and `CustomHeadersStorage`, all from provider functions.
     * `AuthRepository`, `CollectionsRepository`, `ItemsRepository`, `TagsRepository`, `TemplatesRepository` (M8.1) and `WishlistsRepository` (M9.1) and `LoansRepository` (M10.1) and `AlbumsRepository` (M11.1) and `InventoriesRepository` (M12.2) and `LogsRepository` (M13.2) are resolved, with the M8.4
     * `SaveTemplateUseCase` and `TemplateChanges`, the M8.5 `ChoiceListChanges` and the M9.4 `WishlistChanges`, the M9.6 `WishChanges`, the M11.4 `AlbumChanges`, the M11.6 `PhotoChanges`,
     * too, and the M3.6 add-item bindings (`AddItemUseCase`, `ItemListChanges`, `PhotoFiles`), which
     * come from provider functions, and the M4.2 `CustomHeaderValidator`: `verify()` checks only what the graph defines, so a repository without its `@Single`
     * would not fail it (mutation-checked in M1.5). A missing `includes` line still passes here, as in M1.3.
     * Resolving them creates no file: DataStore writes only on the first save.
     */
    @Test
    fun `the HTTP client and the repositories resolve from the JVM app graph`() {
        val koin = startKoin<KoinApp> { }.koin
        try {
            koin.get<AuthRepository>()
            koin.get<CollectionsRepository>()
            koin.get<ItemsRepository>()
            koin.get<TagsRepository>()
            koin.get<TemplatesRepository>()
            koin.get<WishlistsRepository>()
            koin.get<LoansRepository>()
            koin.get<AlbumsRepository>()
            koin.get<InventoriesRepository>()
            koin.get<LogsRepository>()
            koin.get<WishlistChanges>()
            koin.get<WishChanges>()
            koin.get<AlbumChanges>()
            koin.get<PhotoChanges>()
            koin.get<SaveTemplateUseCase>()
            koin.get<TemplateChanges>()
            koin.get<ChoiceListChanges>()
            koin.get<AddItemUseCase>()
            koin.get<ItemListChanges>()
            koin.get<PhotoFiles>()
            koin.get<CustomHeaderValidator>()
            koin.get<HttpClient>().close()
        } finally {
            stopKoin()
        }
    }

    private companion object {
        /**
         * Types nothing in `AppModule` defines, on purpose:
         * - `HttpClientEngine` is a false positive. `verify()` reads `NetworkModule`'s
         *   `provideHttpClient` through the `HttpClient(engine)` constructor, not through the
         *   function's own parameters. The function builds the client on Ktor's default engine,
         *   so Koin is never asked for one (wallosmobile has the same entry).
         * - `SavedStateHandle` (M3.5, `AddItemViewModel`): Koin takes it from the ViewModel's
         *   creation extras when `koinViewModel()` creates it, not from a module.
         */
        val EXTERNALLY_SUPPLIED = listOf(HttpClientEngine::class, SavedStateHandle::class)
    }
}
