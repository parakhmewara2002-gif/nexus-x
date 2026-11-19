package com.lnxv.player

import android.app.Application
import coil.ImageLoader
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.lnxv.player.core.database.LNXVDatabase
import com.lnxv.player.core.datastore.UserPreferencesDataStore
import com.lnxv.player.data.repository.VaultRepository
import com.lnxv.player.data.repository.VideoRepository
import com.lnxv.player.playback.engine.Media3PlayerManager

class LNXVApplication : Application(), ImageLoaderFactory {

    val database by lazy { LNXVDatabase.getInstance(this) }
    val dataStore by lazy { UserPreferencesDataStore(this) }
    val videoRepository by lazy { VideoRepository(this, database.videoDao()) }
    val vaultRepository by lazy { VaultRepository(this, database.videoDao(), dataStore) }
    private val playerManagerDelegate = lazy { Media3PlayerManager(this) }
    val playerManager by playerManagerDelegate
    val isPlayerInitialized: Boolean get() = playerManagerDelegate.isInitialized()

    override fun onCreate() {
        super.onCreate()
        instance = this
        CoroutineScope(Dispatchers.IO).launch { dataStore.userPreferencesFlow.first() }
    }

    override fun newImageLoader(): ImageLoader {
        val lowRam = getSystemService(android.app.ActivityManager::class.java)?.isLowRamDevice == true
        return ImageLoader.Builder(this)
            .allowRgb565(lowRam)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(if (lowRam) 0.10 else 0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("lnxv_thumbnail_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Drop decoded thumbnails when the system is short on RAM; they re-decode from the disk cache.
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            coil.Coil.imageLoader(this).memoryCache?.trimMemory(level)
        }
    }

    companion object {
        lateinit var instance: LNXVApplication
            private set
    }
}
