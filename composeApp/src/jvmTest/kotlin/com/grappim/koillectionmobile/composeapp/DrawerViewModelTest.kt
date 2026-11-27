package com.grappim.koillectionmobile.composeapp

import coil3.Canvas
import coil3.Image
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.koillectionmobile.feature.auth.domain.AuthRepository
import com.grappim.koillectionmobile.feature.auth.domain.LoginResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okio.Path.Companion.toOkioPath
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Moved here from `CollectionListViewModelTest` with Log out (M11.11). */
class DrawerViewModelTest {

    private val authRepository = FakeAuthRepository()
    private val mainDispatcherRule = MainDispatcherRule()
    private val cacheDir = Files.createTempDirectory("drawer-image-cache").toFile()
    private val imageLoader = ImageLoader.Builder(PlatformContext.INSTANCE)
        .memoryCache { MemoryCache.Builder().maxSizeBytes(MAX_SIZE).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.toOkioPath()).maxSizeBytes(MAX_SIZE).build() }
        .build()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
        imageLoader.shutdown()
        cacheDir.deleteRecursively()
    }

    @Test
    fun `log out clears the session through the auth repository`() {
        val sut = DrawerViewModel(authRepository, imageLoader)

        sut.logOut()

        assertEquals(1, authRepository.logOutCalls)
    }

    @Test
    fun `log out empties the image memory and disk caches`() {
        val memoryCache = requireNotNull(imageLoader.memoryCache)
        val diskCache = requireNotNull(imageLoader.diskCache)
        memoryCache[MemoryCache.Key("image")] = MemoryCache.Value(TestImage)
        requireNotNull(diskCache.openEditor("image")).apply {
            diskCache.fileSystem.write(data) { writeUtf8("bytes") }
            commit()
        }
        assertEquals(1, memoryCache.keys.size)
        assertEquals(true, diskCache.size > 0)
        val sut = DrawerViewModel(authRepository, imageLoader)

        sut.logOut()

        // The disk cache is cleared on `Dispatchers.IO`, after the main-thread part returns.
        runBlocking { withTimeout(TIMEOUT_MS) { while (diskCache.size > 0) delay(POLL_MS) } }
        assertEquals(0, memoryCache.keys.size)
        assertEquals(null, diskCache.openSnapshot("image"))
    }

    private object TestImage : Image {
        override val size: Long = 1
        override val width: Int = 1
        override val height: Int = 1
        override val shareable: Boolean = true
        override fun draw(canvas: Canvas) = Unit
    }

    private class FakeAuthRepository : AuthRepository {
        var logOutCalls = 0

        override suspend fun logIn(serverUrl: String, username: String, password: String): LoginResult =
            error("not used")

        override suspend fun logOut() {
            logOutCalls++
        }

        override fun storedServerUrl(): String = error("not used")
    }

    private companion object {
        const val MAX_SIZE = 1_000_000L
        const val TIMEOUT_MS = 5_000L
        const val POLL_MS = 10L
    }
}
