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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class PlayAlarm(
	context: Context,
	private val analytics: Analytics,
) {
	private val appContext = context.applicationContext
	private val audioManager =
		appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

	private var mediaPlayer: MediaPlayer? = null
	private var hasAudioFocus = false
	private var playJob: Job? = null

	// Reuse the same request instance for request + abandon, per docs.
	private val audioFocusRequest: AudioFocusRequest by lazy { buildAudioFocusRequest() }

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
	private val mainScope = CoroutineScope( Dispatchers.Main)


	private val audioFocusChangeListener =
		AudioManager.OnAudioFocusChangeListener { focusChange ->
			scope.launch {
				analytics.captureEvent(
					"audio focus changed",
					mapOf("focus_change" to focusChange),
				)
			}

			when (focusChange) {
				AudioManager.AUDIOFOCUS_GAIN -> {
					// If we were paused/stopped for some reason and still have a player,
					// resume. For an alarm, we prefer not to stop just because focus changed.
					mediaPlayer?.let { player ->
						if (runCatching { !player.isPlaying }.getOrDefault(false)) {
							runCatching { player.start() }
						}
					}
				}

				AudioManager.AUDIOFOCUS_LOSS,
				AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
				AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
					// Alarm policy: keep ringing. We only log the event.
					// If the system enforces a mute/fade for this device/state, we can't override it.
				}
			}
		}

	fun play(soundUri: Uri) {
		stop(abandonFocus = true)

		val player = try {
			MediaPlayer().apply {
				setAudioAttributes(
					AudioAttributes.Builder()
						.setUsage(AudioAttributes.USAGE_ALARM)
						.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
						.build()
				)

				setDataSource(appContext, soundUri)
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

					if (mediaPlayer === mp) {
						mediaPlayer = null
					}

					if (hasAudioFocus) {
						runCatching { audioManager.abandonAudioFocusRequest(audioFocusRequest) }
						hasAudioFocus = false
					}

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

		mainScope.launch {
			var focusResult = runCatching {
				audioManager.requestAudioFocus(audioFocusRequest)
			}.getOrElse {
				AudioManager.AUDIOFOCUS_REQUEST_FAILED
			}
			val tries = 4
			for (i in 1..tries ) {
				hasAudioFocus = focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
				if (hasAudioFocus) break
				delay(350.milliseconds) // 350 is an arbitrary no
				focusResult = runCatching { audioManager.requestAudioFocus(audioFocusRequest) }.getOrElse { AudioManager.AUDIOFOCUS_REQUEST_FAILED }
			}
			if (focusResult == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
				scope.launch {
					analytics.captureEvent(
						"audio focus request failed",
						mapOf("uri" to soundUri.toString(),
							"no of audioFocusRequest" to tries
						)
					)
				}
			}

			// Alarm-app behavior: proceed even if focus wasn't granted.
			runCatching { player.start() }.onFailure { t ->
				scope.launch {
					analytics.captureEvent(
						"play alarm start failed",
						mapOf("message" to (t.message ?: "unknown"))
					)
				}
				stop(abandonFocus = true)
			}
		}
	}

	fun stop(abandonFocus: Boolean = true) {
		playJob?.cancel()
		playJob = null

		mediaPlayer?.let { player ->
			runCatching {
				if (player.isPlaying) {
					player.stop()
				}
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

	private suspend fun requestAudioFocusWithForegroundServiceRetry(soundUri: Uri): Int {
		var focusResult = requestAudioFocus()
		var attemptsLeft = 3

		// On Android 15+, a just-started FGS may not be eligible for focus immediately.
		while (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED && attemptsLeft > 0) {
			delay(0.5.seconds)
			focusResult = requestAudioFocus()
			attemptsLeft--
		}

		if (focusResult == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
			analytics.captureEvent(
				"audio focus request failed",
				mapOf("uri" to soundUri.toString())
			)
		}

		return focusResult
	}

	private fun requestAudioFocus(): Int {
		return runCatching {
			audioManager.requestAudioFocus(audioFocusRequest)
		}.getOrElse {
			AudioManager.AUDIOFOCUS_REQUEST_FAILED
		}
	}

	private fun buildAudioFocusRequest(): AudioFocusRequest {
		return AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
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
