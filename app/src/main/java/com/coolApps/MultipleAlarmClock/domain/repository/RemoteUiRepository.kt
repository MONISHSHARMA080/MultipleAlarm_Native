package com.coolApps.MultipleAlarmClock.domain.repository

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.player.core.RemoteDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.URL
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class RemoteUiRepository @Inject constructor() {
    fun fetchRemoteDocument(url: String): Flow<Result<CoreDocument>> = flow {
        try {
            val bytes = URL(url).readBytes()
            val document = RemoteDocument(bytes).document
            emit(Result.success(document))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
}
