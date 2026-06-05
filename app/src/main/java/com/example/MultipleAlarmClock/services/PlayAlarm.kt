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
import kotlinx.coroutines.launch


class PlayAlarm (private val context: Context, val analytics: Analytics){

    var mediaPlayer: MediaPlayer? =  null
    var audioFocusRequest: AudioFocusRequest? = null
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        logD("Audio Focus changed in the listener and it is $focusChange and Loss:${AudioManager.AUDIOFOCUS_LOSS} loss transient ${AudioManager.AUDIOFOCUS_LOSS_TRANSIENT} gain:${AudioManager.AUDIOFOCUS_GAIN } Gain transient:${AudioManager.AUDIOFOCUS_GAIN_TRANSIENT } gain transient exclusive:${AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE }AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:${AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK }  ")
        coroutineScope.launch {
            analytics.captureEvent("audio focus changed", mapOf(
                "focus_change" to focusChange,
                "additional_info" to "Audio Focus changed in the listener and it is $focusChange and Loss:${AudioManager.AUDIOFOCUS_LOSS} loss transient ${AudioManager.AUDIOFOCUS_LOSS_TRANSIENT} gain:${AudioManager.AUDIOFOCUS_GAIN} Gain transient:${AudioManager.AUDIOFOCUS_GAIN_TRANSIENT} gain transient exclusive:${AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE}AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:${AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK}  "
                )
            )
        }
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> mediaPlayer?.stop()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
            AudioManager.AUDIOFOCUS_GAIN,  AudioManager.AUDIOFOCUS_GAIN_TRANSIENT, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> {
                mediaPlayer?.start()
            }
        }
    }

	fun stop(){
		runCatching {
			mediaPlayer?.stop()
		}
	}


    /** play a new random alarm
	 * [soundUri] this will give me which alarmSound to play if this is a random alarm then give that in the uri*/
    fun play( soundUri: Uri ){
        runCatching {
            if (mediaPlayer?.isPlaying == true) {
                logD(" the mediaPlayer playing is ${mediaPlayer?.isPlaying} so we are returning ")
				mediaPlayer?.stop()
            }
            val audioFocusReqTemp = audioFocusRequest ?: buildAudioFocusRequest()
            audioFocusRequest = audioFocusReqTemp
            if (mediaPlayer == null ) mediaPlayer = buildMediaPLayer(soundUri)
            val result = audioManager.requestAudioFocus(audioFocusReqTemp)
            logD("Requested for audio focus and got it to be $result granted:${AudioManager.AUDIOFOCUS_REQUEST_GRANTED} , delayed:${AudioManager.AUDIOFOCUS_REQUEST_DELAYED} and failed:${AudioManager.AUDIOFOCUS_REQUEST_FAILED}")
            when(result){
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED , AudioManager.AUDIOFOCUS_REQUEST_DELAYED ->{
                    mediaPlayer?.start()
                    coroutineScope.launch {
                        analytics.captureEvent("AudioFocusRequest result arrived", mapOf(
                            "className" to this@PlayAlarm.toString(),
                            "result" to if (result ==1) "AUDIOFOCUS_REQUEST_GRANTED" else "AUDIOFOCUS_REQUEST_DELAYED",
                        ))
                    }

                }
                AudioManager.AUDIOFOCUS_REQUEST_FAILED ->{
					mediaPlayer?.start()
                    coroutineScope.launch {
                        analytics.captureEvent("AudioFocusRequest result arrived", mapOf(
                            "className" to this@PlayAlarm.toString(),
                            "result" to "AUDIOFOCUS_REQUEST_FAILED"
                        ))
                    }
                }
                else -> {}
            }
        }
    }

    fun pause(){
        this.mediaPlayer?.pause()
    }

    /**destroys itself*/
    fun destroy(){
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer = null
            audioFocusRequest?.let { req ->
                val result = audioManager.abandonAudioFocusRequest(req)
                logD("Abandoned audio focus. Result: $result")
            }
            audioFocusRequest = null
        }
    }

    private fun buildAudioFocusRequest(): AudioFocusRequest {
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE).run {
            setAudioAttributes(AudioAttributes.Builder().run {
                setUsage(AudioAttributes.USAGE_ALARM)
                setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                build()
            })
            setAcceptsDelayedFocusGain(false)
            setOnAudioFocusChangeListener(audioFocusChangeListener)
            build()
        }
        this.audioFocusRequest = focusRequest
        return focusRequest
    }

    private fun buildMediaPLayer(uri: Uri): MediaPlayer{
        val mediaPLayerNew = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
            )
            setDataSource(context, uri)
            isLooping = true
            prepare()
        }
        mediaPlayer = mediaPLayerNew
        return mediaPLayerNew
    }

    private fun logD(msg:String){
        Log.d("AAAAA", "[AlarmService] $msg")
    }
}