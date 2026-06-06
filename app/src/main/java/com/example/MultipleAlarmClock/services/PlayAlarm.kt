package com.coolApps.MultipleAlarmClock.services

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PlayAlarm(
	private val context: Context,
	private val analytics: Analytics,
) {
	private var mediaPlayer: MediaPlayer? = null
	private var hasAudioFocus = false

	private val audioManager =
		context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

	// Reuse the same request instance for request + abandon, per docs.
	private val audioFocusRequest: AudioFocusRequest by lazy { buildAudioFocusRequest() }

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
		scope.launch {
			analytics.captureEvent(
				"audio focus changed",
				mapOf(
					"focus_change" to focusChange,
				)
			)
		}

		when (focusChange) {
			AudioManager.AUDIOFOCUS_LOSS -> {
				// Full loss: stop and release.
				stop(abandonFocus = false)
			}

			AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
				// Pause only if you want resume behavior.
				mediaPlayer?.pause()
			}

			AudioManager.AUDIOFOCUS_GAIN,
			AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
			AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE,
			AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> {
				mediaPlayer?.let { player ->
					if (!player.isPlaying) player.start()
				}
			}
		}
	}

	fun play(soundUri: Uri) {
		// Always dispose the previous instance first.
		stop(abandonFocus = true)

		val player = try {
			MediaPlayer().apply {
				setAudioAttributes(
					AudioAttributes.Builder()
						.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
						.setUsage(AudioAttributes.USAGE_ALARM)
						.build()
				)
				setDataSource(context, soundUri)
				isLooping = true

				setOnErrorListener { mp, what, extra ->
					scope.launch {
						analytics.captureEvent(
							"play alarm error",
							mapOf(
								"what" to what,
								"extra" to extra,
							)
						)
					}
					runCatching { mp.release() }
					if (mediaPlayer === mp) mediaPlayer = null
					true
				}

				prepare()
			}
		} catch (t: Throwable) {
			scope.launch {
				analytics.captureEvent(
					"play alarm create failed",
					mapOf("message" to (t.message ?: "unknown"))
				)
			}
			return
		}

		mediaPlayer = player

		val result = audioManager.requestAudioFocus(audioFocusRequest)
		hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

		if (hasAudioFocus) {
			player.start()
		} else {
			// No focus, do not keep a live player around.
			stop(abandonFocus = false)
		}
	}

	fun stop(abandonFocus: Boolean = true) {
		mediaPlayer?.let { player ->
			runCatching {
				if (player.isPlaying) player.stop()
			}
			runCatching { player.release() }
		}
		mediaPlayer = null

		if (abandonFocus && hasAudioFocus) {
			runCatching { audioManager.abandonAudioFocusRequest(audioFocusRequest) }
			hasAudioFocus = false
		}
	}

	fun destroy() {
		stop(abandonFocus = true)
		scope.cancel()
	}

	private fun buildAudioFocusRequest(): AudioFocusRequest {
		return AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
			.setAudioAttributes(
				AudioAttributes.Builder()
					.setUsage(AudioAttributes.USAGE_ALARM)
					.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
					.build()
			)
			.setAcceptsDelayedFocusGain(false)
			.setOnAudioFocusChangeListener(audioFocusChangeListener)
			.build()
	}
}