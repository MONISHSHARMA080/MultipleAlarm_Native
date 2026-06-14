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
			val focusResult = runCatching {
				audioManager.requestAudioFocus(audioFocusRequest)
			}.getOrElse {
				AudioManager.AUDIOFOCUS_REQUEST_FAILED
			}

			when (focusResult) {
				AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
					hasAudioFocus = true
					// We have focus, start ringing!
					startPlayer(player)
				}
				AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
					hasAudioFocus = false
					// Do NOT start playing yet.
					// The system will call your audioFocusChangeListener with AUDIOFOCUS_GAIN
					// the moment your foreground status registers or the blocking app releases focus.
					scope.launch {
						analytics.captureEvent("audio focus delayed", mapOf("uri" to soundUri.toString()))
					}
				}
				AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
					hasAudioFocus = false
					// Focus was outright denied (e.g., user is on an active phone call).
					// For an alarm app, you usually still force the playback anyway,
					// but at a lower volume or routing to the earpiece.
					startPlayer(player)
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

	private fun startPlayer(player: MediaPlayer) {
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

	private fun buildAudioFocusRequest(): AudioFocusRequest {
		return AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
			.setAudioAttributes(
				AudioAttributes.Builder()
					.setUsage(AudioAttributes.USAGE_ALARM)
					.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
					.build()
			)
			.setAcceptsDelayedFocusGain(true)
			.setOnAudioFocusChangeListener(audioFocusChangeListener)
			.build()
	}
}
