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
	private var shouldResumeAfterTransientLoss = false

	private val audioManager =
		context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	private val audioFocusChangeListener =
		AudioManager.OnAudioFocusChangeListener { focusChange ->

			scope.launch {
				analytics.captureEvent(
					"audio_focus_changed",
					mapOf("focus_change" to focusChange)
				)
			}

			when (focusChange) {

				AudioManager.AUDIOFOCUS_LOSS -> {
					shouldResumeAfterTransientLoss = false
					stop(abandonFocus = false)
				}

				AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
					shouldResumeAfterTransientLoss =
						mediaPlayer?.isPlaying == true

					mediaPlayer?.pause()
				}

				AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
					mediaPlayer?.setVolume(0.2f, 0.2f)
				}

				AudioManager.AUDIOFOCUS_GAIN -> {
					mediaPlayer?.setVolume(1f, 1f)

					if (
						shouldResumeAfterTransientLoss &&
						mediaPlayer?.isPlaying == false
					) {
						runCatching {
							mediaPlayer?.start()
						}
					}

					shouldResumeAfterTransientLoss = false
				}
			}
		}

	private val audioFocusRequest: AudioFocusRequest by lazy {
		AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
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

	fun play(soundUri: Uri) {
		stop(true)

		val focusResult =
			audioManager.requestAudioFocus(audioFocusRequest)

		hasAudioFocus =
			focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

		if (!hasAudioFocus) {
			scope.launch {
				analytics.captureEvent(
					"audio_focus_denied",
					emptyMap()
				)
			}
			return
		}

		try {
			val player = MediaPlayer()

			player.apply {
				setAudioAttributes(
					AudioAttributes.Builder()
						.setUsage(AudioAttributes.USAGE_ALARM)
						.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
						.build()
				)

				isLooping = true

				setDataSource(context, soundUri)

				setOnPreparedListener {
					runCatching {
						it.start()
					}
				}

				setOnCompletionListener {
					// Probably never reached because looping=true,
					// but harmless.
				}

				setOnErrorListener { mp, what, extra ->

					scope.launch {
						analytics.captureEvent(
							"play_alarm_error",
							mapOf(
								"what" to what,
								"extra" to extra
							)
						)
					}

					runCatching { mp.release() }

					if (mediaPlayer === mp) {
						mediaPlayer = null
					}

					if (hasAudioFocus) {
						runCatching {
							audioManager.abandonAudioFocusRequest(
								audioFocusRequest
							)
						}
						hasAudioFocus = false
					}

					true
				}

				prepareAsync()
			}

			mediaPlayer = player

		} catch (t: Throwable) {

			scope.launch {
				analytics.captureEvent(
					"play_alarm_create_failed",
					mapOf(
						"message" to (t.message ?: "unknown")
					)
				)
			}

			if (hasAudioFocus) {
				runCatching {
					audioManager.abandonAudioFocusRequest(
						audioFocusRequest
					)
				}
				hasAudioFocus = false
			}
		}
	}

	fun stop(abandonFocus: Boolean = true) {

		mediaPlayer?.let { player ->

			runCatching {
				if (player.isPlaying) {
					player.stop()
				}
			}

			runCatching {
				player.release()
			}
		}

		mediaPlayer = null
		shouldResumeAfterTransientLoss = false

		if (abandonFocus && hasAudioFocus) {
			runCatching {
				audioManager.abandonAudioFocusRequest(
					audioFocusRequest
				)
			}

			hasAudioFocus = false
		}
	}

	fun destroy() {
		stop(abandonFocus = true)
		scope.cancel()
	}
}
