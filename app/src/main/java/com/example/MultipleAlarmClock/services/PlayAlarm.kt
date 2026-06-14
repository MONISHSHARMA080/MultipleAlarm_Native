package com.coolApps.MultipleAlarmClock.services

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.coolApps.MultipleAlarmClock.analytics.Analytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

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
	private val mainScope = CoroutineScope(Dispatchers.Main)

	private val audioFocusChangeListener =
		AudioManager.OnAudioFocusChangeListener { focusChange ->
			scope.launch {
				analytics.captureEvent(
					"audio focus changed",
					mapOf("focus_change" to focusChange),
				)
			}

		logD("audio focus changed: $focusChange ,AUDIOFOCUS_GAIN:${AudioManager.AUDIOFOCUS_GAIN}   ")

			when (focusChange) {
				AudioManager.AUDIOFOCUS_GAIN -> {
					// This triggers either immediately, after a delay, or after a transient loss.
					hasAudioFocus = true
					mediaPlayer?.let { player ->
						if (runCatching { !player.isPlaying }.getOrDefault(false)) {
							runCatching { player.start() }.onFailure { t ->
								scope.launch {
									analytics.captureEvent(
										"play alarm delayed start failed",
										mapOf("message" to (t.message ?: "unknown"))
									)
								}
							}
						}
					}
				}

				AudioManager.AUDIOFOCUS_LOSS,
				AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
				AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
					// Alarm policy: keep ringing if possible, or pause if strict policy requires.
					// For now, we only log the event and let the system enforce its rules.
					hasAudioFocus = false
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
			val focusResult = audioManager.requestAudioFocus(audioFocusRequest)
			logD("audio focus res:$focusResult, AudioManager.AUDIOFOCUS_REQUEST_GRANTED:${AudioManager.AUDIOFOCUS_REQUEST_GRANTED},AudioManager.AUDIOFOCUS_REQUEST_DELAYED:${AudioManager.AUDIOFOCUS_REQUEST_DELAYED}, AudioManager.AUDIOFOCUS_REQUEST_FAILED:${AudioManager.AUDIOFOCUS_REQUEST_FAILED}      ")

			when (focusResult) {
				AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
					hasAudioFocus = true
					// Focus granted immediately. Start playing!
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

				AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
					hasAudioFocus = false
					// DO NOT START PLAYING YET.
					// The system acknowledged the request but needs a moment (FGS propagation).
					// The `audioFocusChangeListener` will receive `AUDIOFOCUS_GAIN` shortly.
					scope.launch {
						analytics.captureEvent(
							"audio focus delayed",
							mapOf("uri" to soundUri.toString())
						)
					}
				}

				AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
					hasAudioFocus = false
					// The system outright denied focus.
					scope.launch {
						analytics.captureEvent(
							"audio focus request failed",
							mapOf("uri" to soundUri.toString())
						)
					}

					// Fallback strategy for alarms: Try to force play anyway.
					// If the system absolutely prohibits it, it will stay silent,
					// but we ensure our state machine doesn't break.
					runCatching { player.start() }.onFailure { t ->
						scope.launch {
							analytics.captureEvent(
								"play alarm force start failed",
								mapOf("message" to (t.message ?: "unknown"))
							)
						}
						stop(abandonFocus = true)
					}
				}
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

	private fun buildAudioFocusRequest(): AudioFocusRequest {
		// Change 1: TRANSIENT focus ensures background apps auto-resume when you hit stop.
		return AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
			.setAudioAttributes(
				AudioAttributes.Builder()
					.setUsage(AudioAttributes.USAGE_ALARM)
					.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
					.build()
			)
			// Change 2: Accept delayed focus to handle the Foreground Service race condition.
			.setAcceptsDelayedFocusGain(true)
			.setOnAudioFocusChangeListener(audioFocusChangeListener)
			.build()
	}
}

private fun logD(msg: String){
	Log.d("AAAA", "[PlayAlarm] $msg")
}