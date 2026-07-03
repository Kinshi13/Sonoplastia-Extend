package com.escalachurch.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.escalachurch.app.R

/**
 * App-wide singleton for the background music loop and short UI sound effects (e.g. the nav bar
 * swipe pop), volume-controlled from Configurações -> Áudio. A plain object (not part of
 * AppContainer's DI) so composables that don't otherwise touch the container - like the bottom
 * nav bar - can still trigger a sound effect without threading it through every screen.
 */
object AppSoundPlayer {

    private var musicPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null
    private var swipeSoundId: Int = 0
    private var swipeSoundLoaded = false

    /** Starts the looped background track if it isn't already running, and applies [volume] (0..1). */
    fun ensureMusicStarted(context: Context, volume: Float) {
        if (musicPlayer == null) {
            musicPlayer = runCatching {
                MediaPlayer.create(context, R.raw.background_music)?.apply { isLooping = true }
            }.getOrNull()
        }
        setMusicVolume(volume)
    }

    /** Applies [volume] (0..1) live; pauses playback entirely at 0 instead of just silencing it. */
    fun setMusicVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        val player = musicPlayer ?: return
        runCatching {
            player.setVolume(clamped, clamped)
            if (clamped <= 0f) {
                if (player.isPlaying) player.pause()
            } else if (!player.isPlaying) {
                player.start()
            }
        }
    }

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
        musicPlayer?.release()
        musicPlayer = null
        soundPool?.release()
        soundPool = null
        swipeSoundLoaded = false
    }
}
