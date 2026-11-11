package xyz.draumaz.kestrel

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.gif.AnimatedImageDecoder
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import xyz.draumaz.kestrel.core.Analytics
import xyz.draumaz.kestrel.core.net.Http
import xyz.draumaz.kestrel.ui.ButtonHaptics

class KestrelApp : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        appInstance = AppContainer(this)
        app.settings.reload()
        app.start()
        app.session.reload()

        // Before the first Compose view exists, so every window gets hooked.
        ButtonHaptics.install()

        Analytics.init(this)
        // Background inbox notifications (opt-in).
        app.inbox.onAppStart()
    }

    /** One image pipeline: shared OkHttp pool, big memory + disk caches, GIF + video frames. */
    override fun newImageLoader(context: android.content.Context): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { Http.client }))
                add(AnimatedImageDecoder.Factory())
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.25).build() }
            .diskCache {
                // Settings → Image cache size (read once: the loader is built once).
                DiskCache.Builder().directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(app.settings.value.imageCacheMb.toLong() shl 20).build()
            }
            .crossfade(true)
            .build()
}
