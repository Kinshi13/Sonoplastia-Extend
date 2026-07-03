package com.escalachurch.app.audio

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import com.escalachurch.app.R

/**
 * App-wide singleton for the background music loop and short UI sound effects (e.g. the nav bar
 * swipe click), volume-controlled from Configurações -> Áudio. A plain object (not part of
 * AppContainer's DI) so composables that don't otherwise touch the container - like the bottom
 * nav bar - can still trigger a sound effect without threading it through every screen.
 *
 * The swipe click currently uses [ToneGenerator] (a short synthesized tone) rather than a custom
 * sound file - there was no dedicated UI sound effect asset provided, only the background music
 * track. Swap in a real asset later the same way background_music.mp3 was added, if desired.
 */
object AppSoundPlayer {

    private var musicPlayer: MediaPlayer? = null
    private var toneGenerator: ToneGenerator? = null
    private var toneGeneratorVolumePercent: Int = -1

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

    /** Short click for the nav bar swipe gesture; a no-op at [volume] 0 so the slider acts as a mute. */
    fun playSwipeEffect(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        if (clamped <= 0f) return
        val volumePercent = (clamped * 100).toInt().coerceIn(1, 100)
        if (toneGenerator == null || toneGeneratorVolumePercent != volumePercent) {
            toneGenerator?.release()
            toneGenerator = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, volumePercent) }.getOrNull()
            toneGeneratorVolumePercent = volumePercent
        }
        runCatching { toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 40) }
    }

    fun release() {
        musicPlayer?.release()
        musicPlayer = null
        toneGenerator?.release()
        toneGenerator = null
    }
}
