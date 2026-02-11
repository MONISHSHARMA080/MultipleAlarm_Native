package com.example.trying_native.services

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import com.example.trying_native.logD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds


class PlayAlarm (private val context: Context){
    var mediaPlayer: MediaPlayer? =  null
    var audioFocusRequest: AudioFocusRequest? = null
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val coroutineScope = CoroutineScope(Dispatchers.IO)



    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        logD("Audio Focus changed in the listener and it is $focusChange and Loss:${AudioManager.AUDIOFOCUS_LOSS} loss transient ${AudioManager.AUDIOFOCUS_LOSS_TRANSIENT} gain:${AudioManager.AUDIOFOCUS_GAIN } Gain transient:${AudioManager.AUDIOFOCUS_GAIN_TRANSIENT } gain transient exclusive:${AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE }AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:${AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK }  ")
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> mediaPlayer?.stop()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
            AudioManager.AUDIOFOCUS_GAIN,  AudioManager.AUDIOFOCUS_GAIN_TRANSIENT, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> {
                mediaPlayer?.start()
            }
        }
    }


    /** play a new random alarm*/
    fun play( ){
        runCatching {
            val audioFocusReqTemp = audioFocusRequest ?: buildAudioFocusRequest()
            audioFocusRequest = audioFocusReqTemp
            if (mediaPlayer == null ) mediaPlayer = buildMediaPLayer()
            val result = audioManager.requestAudioFocus(audioFocusReqTemp)
            logD("Requested for audio focus and got it to be $result granted:${AudioManager.AUDIOFOCUS_REQUEST_GRANTED} , delayed:${AudioManager.AUDIOFOCUS_REQUEST_DELAYED} and failed:${AudioManager.AUDIOFOCUS_REQUEST_FAILED}")
            when(result){
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED , AudioManager.AUDIOFOCUS_REQUEST_DELAYED ->{
                    mediaPlayer?.start()
                }
                AudioManager.AUDIOFOCUS_REQUEST_FAILED ->{
                        mediaPlayer?.start()
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

    private fun buildMediaPLayer(): MediaPlayer{
        val randomAlarmToPlay = getRandomAlarm()
        val mediaPLayerNew = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
            )
            setDataSource(context, randomAlarmToPlay)
            isLooping = true
            prepare()
        }
        mediaPlayer = mediaPLayerNew
        return mediaPLayerNew
    }

    private fun getRandomAlarm(): Uri{
        val ringtoneManager = RingtoneManager(context)
        ringtoneManager.setType(RingtoneManager.TYPE_ALARM)
        val ringtoneCursor = ringtoneManager.cursor
        val len = ringtoneCursor.count
        val randomIndex = Random.nextInt(len)
        val ringtone = ringtoneManager.getRingtone(randomIndex)

        ringtone.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        ringtone.isLooping = true
        val uri = if (len > 0) {
            val randomIndex = Random.nextInt(len)
            ringtoneManager.getRingtoneUri(randomIndex)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }
        return uri
    }

    private fun logD(msg:String){
        Log.d("AAAAA", "[AlarmService] $msg")
    }
}