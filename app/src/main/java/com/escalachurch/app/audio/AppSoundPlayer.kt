package com.escalachurch.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.escalachurch.app.R

/**
 * App-wide singleton for short UI sound effects (the nav bar swipe pop), volume-controlled from
 * Configurações -> Áudio. A plain object (not part of AppContainer's DI) so composables that
 * don't otherwise touch the container - like the bottom nav bar - can still trigger a sound
 * effect without threading it through every screen.
 *
 * There used to also be a looped background-music track here (MediaPlayer, R.raw.background_music).
 * It was removed entirely: it was never tied to any lifecycle callback (no onPause/onStop, no
 * Service), so once started it kept looping - and draining battery - even after the app was
 * backgrounded or closed, until the OS killed the process. Rather than wire up proper lifecycle
 * handling for a decorative background loop, the feature was dropped.
 */
object AppSoundPlayer {

    private var soundPool: SoundPool? = null
    private var swipeSoundId: Int = 0
    private var swipeSoundLoaded = false

    /** Loads the swipe sound ahead of time so the very first swipe isn't silent while it loads. */
    fun preloadSwipeEffect(context: Context) {
        ensureSoundPool(context)
    }

    private fun ensureSoundPool(context: Context): SoundPool? {
        if (soundPool == null) {
            soundPool = SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .build()
            swipeSoundId = soundPool!!.load(context, R.raw.swipe_pop, 1)
            soundPool!!.setOnLoadCompleteListener { _, sampleId, status ->
                if (sampleId == swipeSoundId && status == 0) swipeSoundLoaded = true
            }
        }
        return soundPool
    }

    /** Soft pop for the nav bar swipe gesture; a no-op at [volume] 0 so the slider acts as a mute. */
    fun playSwipeEffect(context: Context, volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        if (clamped <= 0f) return
        val pool = ensureSoundPool(context) ?: return
        if (!swipeSoundLoaded) return
        runCatching { pool.play(swipeSoundId, clamped, clamped, 1, 0, 1f) }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        swipeSoundLoaded = false
    }
}
