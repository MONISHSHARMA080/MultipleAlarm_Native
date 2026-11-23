package com.coolApps.MultipleAlarmClock.presentation.home

import android.app.AlarmManager
import android.content.Context
import android.os.SystemClock
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolApps.MultipleAlarmClock.ErrorHandling.ErrorHandler
import com.coolApps.MultipleAlarmClock.data.ads.AdManager
import com.coolApps.MultipleAlarmClock.data.local.AlarmData
import com.coolApps.MultipleAlarmClock.data.preferences.Settings
import com.coolApps.MultipleAlarmClock.data.preferences.copy
import com.coolApps.MultipleAlarmClock.domain.repository.AlarmRepository
import com.coolApps.MultipleAlarmClock.domain.usecase.AlarmsController
import com.coolApps.MultipleAlarmClock.domain.usecase.FeedbackPromptEligibilityChecker
import com.coolApps.MultipleAlarmClock.domain.usecase.InAppReviewEligibilityChecker
import com.coolApps.MultipleAlarmClock.presentation.logD
import com.coolApps.MultipleAlarmClock.util.Analytics
import com.coolApps.MultipleAlarmClock.util.FeatureFlagsData
import com.coolApps.MultipleAlarmClock.util.Result
import com.coolApps.MultipleAlarmClock.util.getFirstInstallTimeMs
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.model.ReviewErrorCode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


@HiltViewModel
class AlarmContainerViewModel @Inject constructor(
	val analytics: Analytics,
	adManager: AdManager,
	private val alarmRepository: AlarmRepository,
	private val alarmManager: AlarmManager,
	private val dataStore: DataStore<Settings>,
	private val alarmsController: AlarmsController,
	private val errorHandler: ErrorHandler,
	@ApplicationContext  val context: Context
) : ViewModel(){

	private val eligibilityChecker: InAppReviewEligibilityChecker = InAppReviewEligibilityChecker()
	private val feedbackEligibilityChecker = FeedbackPromptEligibilityChecker()
	private val firstInstallTimeMs: Long by lazy { getFirstInstallTimeMs(context) }
	val reviewManager: ReviewManager by lazy { ReviewManagerFactory.create(context) }
	private var prefetchedReviewInfo: ReviewInfo? = null
	private var prefetchedReviewAtMs: Long = 0L
	private var promptCheckJob: Job? = null
	private val _showReviewUi = MutableStateFlow(false)
	private val _feedbackPrompt = MutableStateFlow(InAppFeedbackPromptUiState())

	val alarmControllerUi: StateFlow<AlarmContainerUiState> = combine(
		alarmRepository.getAlarmsStream(),
		_showReviewUi,
		_feedbackPrompt,
		adManager.shouldShowBannerAds,
	) { alarmList, showReviewUi, feedbackPrompt, shouldShowAds ->
		AlarmContainerUiState(
			items = buildAlarmItems(alarmList, shouldShowAds),
			showReviewUi = showReviewUi,
			feedbackPrompt = feedbackPrompt,
		)
	}
		.flowOn(Dispatchers.Default)
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5_000),
			initialValue = AlarmContainerUiState()
		)

	init {
		prefetchReviewInfoIfEligible()
	}

	private fun prefetchReviewInfoIfEligible() {
		viewModelScope.launch {
			val featureFlags = analytics.featureFlagsData.filterNotNull().first()
			val settingsData = dataStore.data.first()
			val alarmList = alarmRepository.getAlarmsStream().first()
			val nowMs = System.currentTimeMillis()
			if (isReviewEligible(alarmList, settingsData, featureFlags, nowMs)) {
				try {
					prefetchedReviewInfo = reviewManager.requestReview()
					prefetchedReviewAtMs = SystemClock.elapsedRealtime()
				} catch (e: CancellationException) {
					throw e
				} catch (e: Exception) {
					logD("review prefetch failed: ${e.message}")
				}
			}
		}
	}

	fun consumePrefetchedReviewInfo(): ReviewInfo? {
		val cached = prefetchedReviewInfo.takeIf {
			(SystemClock.elapsedRealtime() - prefetchedReviewAtMs) < REVIEW_INFO_TTL_MS
		}
		prefetchedReviewInfo = null
		prefetchedReviewAtMs = 0L
		return cached
	}

	private fun buildAlarmItems(
		alarms: List<AlarmData>,
		shouldShowAds: Boolean,
	): List<AlarmListItem> {
		if (!shouldShowAds || alarms.isEmpty()) return alarms.map(AlarmListItem::Alarm)
		if (alarms.size == 1) return listOf(AlarmListItem.Alarm(alarms[0]), AlarmListItem.Ad(0))

		val firstAdAfter = if (alarms.size <= 3) 0 else 1
		return buildList(alarms.size + 2) {
			alarms.forEachIndexed { index, alarm ->
				add(AlarmListItem.Alarm(alarm))
				val isAdSlot = index == firstAdAfter || (index > firstAdAfter && (index - firstAdAfter) % 4 == 0)
				if (isAdSlot && index < alarms.lastIndex) {
					add(AlarmListItem.Ad(index))
				}
			}
		}
	}


	private fun resolveInstallTimeMs(settingsData: Settings): Long =
		settingsData.installEpochTimeMs.takeIf { it > 0L } ?: firstInstallTimeMs

	private fun isCrossPromptGapPassed(nowMs: Long, otherAttemptMs: Long, ownCooldownDays: Int): Boolean {
		if (otherAttemptMs <= 0L) return true
		val gapDays = minOf(CROSS_PROMPT_GAP_DAYS, ownCooldownDays)
		return (nowMs - otherAttemptMs).coerceAtLeast(0L) >= gapDays * MILLIS_PER_DAY
	}

	private fun isReviewEligible(
		alarmList: List<AlarmData>,
		settingsData: Settings,
		featureFlags: FeatureFlagsData,
		nowMs: Long,
	): Boolean {
		val highestIdAlarm = alarmList.maxByOrNull { it.id } ?: return false
		if (!isCrossPromptGapPassed(nowMs, settingsData.lastFeedbackPromptAttemptedAt, featureFlags.cooldownDays)) {
			return false
		}
		val res = eligibilityChecker.evaluate(
			nowEpochMs = nowMs,
			installEpochTimeMs = resolveInstallTimeMs(settingsData),
			alarmCount = highestIdAlarm.id,
			lastReviewAttemptEpochTimeMs = settingsData.lastReviewAttemptedAt,
			config = featureFlags,
		)
		logD("checked review prompt eligibility: $res")
		return res is InAppReviewEligibilityChecker.Result.Eligible
	}

	fun onPositiveUserAction() {
		if (promptCheckJob?.isActive == true || _showReviewUi.value || _feedbackPrompt.value.isVisible) return

		promptCheckJob = viewModelScope.launch {
			val settingsData = dataStore.data.first()
			val featureFlags = analytics.featureFlagsData.value
			val alarmList = alarmRepository.getAlarmsStream().first()

			if (_showReviewUi.value || _feedbackPrompt.value.isVisible) return@launch

			if (featureFlags != null && alarmList.isNotEmpty()) {
				val nowMs = System.currentTimeMillis()
				if (isReviewEligible(alarmList, settingsData, featureFlags, nowMs)) {
					_showReviewUi.value = true
					return@launch
				}

				logD("checking if feature flag is eligible")
				val feedbackConfig = featureFlags.feedbackPromptConfig
				if (isCrossPromptGapPassed(nowMs, settingsData.lastReviewAttemptedAt, feedbackConfig.cooldownDays) &&
					feedbackEligibilityChecker.isEligible(
						installEpochTimeMs = resolveInstallTimeMs(settingsData),
						lastPromptAttemptEpochTimeMs = settingsData.lastFeedbackPromptAttemptedAt,
						config = feedbackConfig,
						nowEpochMs = nowMs,
					)
				) {
					logD("feedback is eligible")
					_feedbackPrompt.value = InAppFeedbackPromptUiState(isVisible = true)
					analytics.captureEvent("in_app_feedback_prompt_shown", mapOf())
				}
			}
		}
	}

	fun dismissReviewUi() {
		_showReviewUi.value = false
	}

	fun setInAppReviewConsumed(isSuccessful: Boolean, exception: Exception?) {
		viewModelScope.launch {
			_showReviewUi.value = false
			val reviewException = exception as? ReviewException
			val message = exception?.message ?: ""
			val statusCode = reviewException?.statusCode?.toString() ?: ""
			val stackTrace = exception?.stackTraceToString()?.take(2000) ?: ""
			val cause = exception?.cause?.toString() ?: ""
			val status = reviewException?.status?.toString() ?: ""
			val errorCode = reviewException?.errorCode
			val isTransientFailure = !isSuccessful && errorCode == ReviewErrorCode.INTERNAL_ERROR

			logD("review flow completed, success=$isSuccessful, error_code=$errorCode, left_state_unchanged/isTransientFailure =$isTransientFailure")

			analytics.captureEvent(
				"in_app_review_shown",
				mapOf(
					"success" to isSuccessful,
					"error_code" to (errorCode?.toString() ?: ""),
					"error_message" to message,
					"left_state_unchanged" to isTransientFailure,
					"status_code" to statusCode,
					"stack_trace" to stackTrace,
					"cause" to cause,
					"status" to status,
				)
			)
			if (!isTransientFailure) {
				dataStore.updateData {
					it.copy { lastReviewAttemptedAt = System.currentTimeMillis() }
				}
			}
		}
	}

	fun selectFeedbackRating(rating: Int): Boolean {
		val prompt = _feedbackPrompt.value
		if (!prompt.isVisible || prompt.step != InAppFeedbackPromptStep.Rating) return false
		if (rating >= 4) {
			analytics.captureEvent(
				"feedback_submitted",
				mapOf("rating" to rating, "routed_to_play_store" to true),
			)
			analytics.captureEvent(
				"Rate App Clicked",
				mapOf("screen" to "InAppFeedbackPrompt", "rating" to rating),
			)
			consumeFeedbackPrompt(markReviewAttempted = true)
			return true
		}
		_feedbackPrompt.value = prompt.copy(
			rating = rating,
			step = InAppFeedbackPromptStep.Review,
		)
		return false
	}

	fun submitFeedback(feedback: String) {
		val prompt = _feedbackPrompt.value
		if (!prompt.isVisible || prompt.step != InAppFeedbackPromptStep.Review) return
		analytics.captureEvent(
			"feedback_submitted",
			mapOf("rating" to (prompt.rating ?: 0), "feedback" to feedback),
		)
		consumeFeedbackPrompt()
	}

	fun dismissFeedbackPrompt() {
		val prompt = _feedbackPrompt.value
		if (!prompt.isVisible) return
		analytics.captureEvent(
			"feedback_prompt_dismissed",
			mapOf(
				"step" to prompt.step.name.lowercase(),
				"rating" to (prompt.rating ?: 0),
			),
		)
		consumeFeedbackPrompt()
	}

	private fun consumeFeedbackPrompt(markReviewAttempted: Boolean = false) {
		_feedbackPrompt.value = InAppFeedbackPromptUiState()
		val now = System.currentTimeMillis()
		viewModelScope.launch {
			dataStore.updateData {
				it.copy {
					lastFeedbackPromptAttemptedAt = now
					if (markReviewAttempted) {
						lastReviewAttemptedAt = now
					}
				}
			}
		}
	}


	fun stopAlarm(alarmData: AlarmData){
		viewModelScope.launch {
			launch {
				analytics.captureEvent("user stopped the alarm", mapOf("alarmData" to alarmData.toString()))
			}
			logD("user asked to stop the alarm $alarmData")
			alarmsController.cancelAlarmHandler(alarmData,  context, alarmManager).fold(onSuccess = {}, onError = { error ->
				errorHandler.handleError(Result.Failure(error))
				logD("there is a error in cancelling alarm-->${error.internalErrorMessage}")
			})
		}
	}

	fun captureEvent(event:String, properties:Map<String, Any>){
		analytics.captureEvent(event, properties)
	}

	fun resetAlarm(alarmData: AlarmData){
		viewModelScope.launch {
			logD("about to reset the alarm-+")
			val exception = alarmsController.resetAlarmsHandler(
				alarmData = alarmData,
				alarmManager = alarmManager,
				activityContext = context,
			)
			exception.fold(
				onSuccess = {
					viewModelScope.launch {
						analytics.captureEvent("alarm_reset", mapOf(
							"alarmData" to alarmData.toString()
						))
					}
				},
				onError = { error ->
					errorHandler.handleError(Result.Failure(error))
					logD("error in the reset alarm -->${error.internalErrorMessage}")
				}
			)
		}

	}

	fun deleteAlarm(alarmData: AlarmData){
		viewModelScope.launch {
			launch {
				analytics.captureEvent("user deleting the alarm", mapOf(
					"alarmData" to alarmData.toString()
				)
				)
			}
			logD("deleting the alarm $alarmData")
			alarmsController.deleteAlarmHandler(alarmData, context, alarmManager).fold(onSuccess = {}, onError = { error ->
				logD("there is a error in deleting the alarm that is $error")
				errorHandler.handleError(Result.Failure(error))
			})
		}

	}

	private companion object {
		const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
		const val CROSS_PROMPT_GAP_DAYS = 7
		const val REVIEW_INFO_TTL_MS = 5L * 60L * 1000L
	}

}
