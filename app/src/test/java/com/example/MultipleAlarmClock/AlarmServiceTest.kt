package com.coolApps.MultipleAlarmClock

//
//@OptIn(ExperimentalCoroutinesApi::class)
//@RunWith(RobolectricTestRunner::class)
//@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
//class AlarmServiceTest {
//
//    private lateinit var context: Context
//    private lateinit var service: AlarmService
//    private lateinit var notificationManager: NotificationManager
//    private lateinit var shadowNotificationManager: ShadowNotificationManager
//    private lateinit var audioManager: AudioManager
//    private lateinit var shadowAudioManager: ShadowAudioManager
//    private val testDispatcher = UnconfinedTestDispatcher()
//
//    @Before
//    fun setup() {
//        context = ApplicationProvider.getApplicationContext()
//
//        // Get real system services so Robolectric shadows work
//        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        shadowNotificationManager = shadowOf(notificationManager)
//
//        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//        shadowAudioManager = shadowOf(audioManager)
//
//        // Create service instance
//        service = Robolectric.buildService(AlarmService::class.java).create().get()
//    }
//
//    @Test
//    fun `service starts foreground when ACTION_START_ALARM received`() = runTest {
//        // Given
//        val intent = createAlarmIntent(alarmId = 1, message = "Wake up!")
//        intent.action = AlarmService.ACTION_START_ALARM
//
//        // When
//        val result = service.onStartCommand(intent, 0, 1)
//
//        // Then
//        assertEquals(android.app.Service.START_REDELIVER_INTENT, result)
//
//        // Verify notification was posted
//        val notifications = shadowNotificationManager.allNotifications
//        assertTrue(notifications.isNotEmpty(), "Expected notification to be posted")
//
//        val notification = notifications.first()
//        assertNotNull(notification.contentTitle)
//        assertEquals("Alarm", notification.contentTitle.toString())
//    }
//
//    @Test
//    fun `service requests audio focus with correct parameters when alarm starts`() = runTest {
//        // Given
//        val intent = createAlarmIntent(alarmId = 1, message = "Test alarm")
//        intent.action = AlarmService.ACTION_START_ALARM
//
//        // When
//        service.onStartCommand(intent, 0, 1)
//
//        // Allow coroutine to execute
//        testDispatcher.scheduler.advanceUntilIdle()
//
//        // Then
//        // Note: Robolectric's ShadowAudioManager doesn't fully track AudioFocusRequest details
//        // In a real test, you'd mock AudioManager or use a wrapper class
//        // For now, we verify the service attempted to request focus
//        assertTrue(service.audioFocusRequest != null, "Audio focus request should be created")
//    }
//
//    @Test
//    fun `service adds alarm to intentHashMap when started`() = runTest {
//        // Given
//        val intent = createAlarmIntent(alarmId = 123, message = "Morning alarm")
//        intent.action = AlarmService.ACTION_START_ALARM
//
//        // When
//        service.onStartCommand(intent, 0, 1)
//
//        // Then
//        assertTrue(service.intentHashMap.containsKey(123), "Alarm should be added to hashMap")
//        assertEquals(1, service.intentHashMap.size)
//    }
//
//    @Test
//    fun `service does not add duplicate alarm IDs to intentHashMap`() = runTest {
//        // Given
//        val intent1 = createAlarmIntent(alarmId = 1, message = "First")
//        intent1.action = AlarmService.ACTION_START_ALARM
//
//        val intent2 = createAlarmIntent(alarmId = 1, message = "Duplicate")
//        intent2.action = AlarmService.ACTION_START_ALARM
//
//        // When
//        service.onStartCommand(intent1, 0, 1)
//        service.onStartCommand(intent2, 0, 2)
//
//        // Then
//        assertEquals(1, service.intentHashMap.size, "Duplicate alarm ID should not be added")
//    }
//
//    @Test
//    fun `service tracks multiple different alarms in intentHashMap`() = runTest {
//        // Given
//        val intent1 = createAlarmIntent(alarmId = 1, message = "Alarm 1")
//        intent1.action = AlarmService.ACTION_START_ALARM
//
//        val intent2 = createAlarmIntent(alarmId = 2, message = "Alarm 2")
//        intent2.action = AlarmService.ACTION_START_ALARM
//
//        val intent3 = createAlarmIntent(alarmId = 3, message = "Alarm 3")
//        intent3.action = AlarmService.ACTION_START_ALARM
//
//        // When
//        service.onStartCommand(intent1, 0, 1)
//        service.onStartCommand(intent2, 0, 2)
//        service.onStartCommand(intent3, 0, 3)
//
//        // Then
//        assertEquals(3, service.intentHashMap.size)
//        assertTrue(service.intentHashMap.containsKey(1))
//        assertTrue(service.intentHashMap.containsKey(2))
//        assertTrue(service.intentHashMap.containsKey(3))
//    }
//
//    @Test
//    fun `dismissing only alarm stops service and removes audio focus`() = runTest {
//        // Given - start an alarm
//        val startIntent = createAlarmIntent(alarmId = 1, message = "Test")
//        startIntent.action = AlarmService.ACTION_START_ALARM
//        service.onStartCommand(startIntent, 0, 1)
//
//        testDispatcher.scheduler.advanceUntilIdle()
//
//        // When - dismiss it
//        val dismissIntent = createAlarmIntent(alarmId = 1, message = "Test")
//        dismissIntent.action = AlarmService.ACTION_DISMISS_ALARM
//        service.onStartCommand(dismissIntent, 0, 2)
//
//        // Then
//        assertTrue(service.intentHashMap.isEmpty(), "HashMap should be empty after dismissing only alarm")
//        // Service should be stopping (hard to verify directly in Robolectric)
//    }
//
//    @Test
//    fun `dismissing first alarm plays second alarm from queue`() = runTest {
//        // Given - start two alarms
//        val intent1 = createAlarmIntent(alarmId = 1, message = "First alarm")
//        intent1.action = AlarmService.ACTION_START_ALARM
//        service.onStartCommand(intent1, 0, 1)
//
//        val intent2 = createAlarmIntent(alarmId = 2, message = "Second alarm")
//        intent2.action = AlarmService.ACTION_START_ALARM
//        service.onStartCommand(intent2, 0, 2)
//
//        testDispatcher.scheduler.advanceUntilIdle()
//
//        assertEquals(2, service.intentHashMap.size)
//
//        // When - dismiss first alarm
//        val dismissIntent = createAlarmIntent(alarmId = 1, message = "First alarm")
//        dismissIntent.action = AlarmService.ACTION_DISMISS_ALARM
//        service.onStartCommand(dismissIntent, 0, 3)
//
//        testDispatcher.scheduler.advanceUntilIdle()
//
//        // Then
//        assertEquals(1, service.intentHashMap.size, "Only second alarm should remain")
//        assertTrue(service.intentHashMap.containsKey(2), "Second alarm should still be in queue")
//        assertFalse(service.intentHashMap.containsKey(1), "First alarm should be removed")
//
//        // Verify new notification was posted for second alarm
//        val notifications = shadowNotificationManager.allNotifications
//        assertTrue(notifications.isNotEmpty())
//    }
//
//    @Test
//    fun `service handles null intent gracefully`() = runTest {
//        // When
//        val result = service.onStartCommand(null, 0, 1)
//
//        // Then
//        assertEquals(android.app.Service.START_NOT_STICKY, result)
//    }
//
//    @Test
//    fun `service handles unknown action gracefully`() = runTest {
//        // Given
//        val intent = createAlarmIntent(alarmId = 1, message = "Test")
//        intent.action = "UNKNOWN_ACTION"
//
//        // When
//        val result = service.onStartCommand(intent, 0, 1)
//
//        // Then
//        assertEquals(android.app.Service.START_NOT_STICKY, result)
//    }
//
//    @Test
//    fun `service handles missing intentData gracefully on start`() = runTest {
//        // Given - intent without intentData
//        val intent = Intent(context, AlarmService::class.java)
//        intent.action = AlarmService.ACTION_START_ALARM
//
//        // Pre-populate hashMap so we hit the branch that checks for intentData
//        val validIntent = createAlarmIntent(alarmId = 99, message = "Existing")
//        validIntent.action = AlarmService.ACTION_START_ALARM
//        service.onStartCommand(validIntent, 0, 1)
//
//        // When - try to start with invalid intent
//        val result = service.onStartCommand(intent, 0, 2)
//
//        // Then
//        assertEquals(android.app.Service.START_NOT_STICKY, result)
//    }
//
//    @Test
//    fun `service handles missing intentData gracefully on dismiss`() = runTest {
//        // Given - start a valid alarm first
//        val startIntent = createAlarmIntent(alarmId = 1, message = "Test")
//        startIntent.action = AlarmService.ACTION_START_ALARM
//        service.onStartCommand(startIntent, 0, 1)
//
//        // When - try to dismiss with invalid intent (no intentData)
//        val dismissIntent = Intent(context, AlarmService::class.java)
//        dismissIntent.action = AlarmService.ACTION_DISMISS_ALARM
//        val result = service.onStartCommand(dismissIntent, 0, 2)
//
//        // Then
//        assertEquals(android.app.Service.START_NOT_STICKY, result)
//    }
//
//    @Test
//    fun `onDestroy cleans up audio focus and ringtone`() = runTest {
//        // Given - start an alarm
//        val intent = createAlarmIntent(alarmId = 1, message = "Test")
//        intent.action = AlarmService.ACTION_START_ALARM
//        service.onStartCommand(intent, 0, 1)
//
//        testDispatcher.scheduler.advanceUntilIdle()
//
//        // When
//        service.onDestroy()
//
//        // Then - no exceptions thrown, cleanup succeeded
//        // In a real test with mocked AudioManager, you'd verify abandonAudioFocusRequest was called
//    }
//
//    @Test
//    fun `notification channel is created with correct properties`() = runTest {
//        // Given
//        val intent = createAlarmIntent(alarmId = 1, message = "Test")
//        intent.action = AlarmService.ACTION_START_ALARM
//
//        // When
//        service.onStartCommand(intent, 0, 1)
//
//        // Then
//        val channel = notificationManager.getNotificationChannel("alarm_channel_id")
//        assertNotNull(channel, "Notification channel should be created")
//        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
//        assertTrue(channel.canBypassDnd(), "Channel should bypass DND")
//    }
//
//    @Test
//    fun `notification contains dismiss action`() = runTest {
//        // Given
//        val intent = createAlarmIntent(alarmId = 1, message = "Morning alarm")
//        intent.action = AlarmService.ACTION_START_ALARM
//
//        // When
//        service.onStartCommand(intent, 0, 1)
//
//        // Then
//        val notifications = shadowNotificationManager.allNotifications
//        assertTrue(notifications.isNotEmpty())
//
//        val notification = notifications.first()
//        assertTrue(notification.actions.isNotEmpty(), "Notification should have actions")
//
//        val dismissAction = notification.actions.firstOrNull { it.title == "Dismiss" }
//        assertNotNull(dismissAction, "Should have dismiss action")
//    }
//
//    @Test
//    fun `notification displays custom message when provided`() = runTest {
//        // Given
//        val customMessage = "Time to wake up and exercise!"
//        val intent = createAlarmIntent(alarmId = 1, message = customMessage)
//        intent.action = AlarmService.ACTION_START_ALARM
//
//        // When
//        service.onStartCommand(intent, 0, 1)
//
//        // Then
//        val notifications = shadowNotificationManager.allNotifications
//        val notification = notifications.first()
//        assertEquals(customMessage, notification.contentText.toString())
//    }
//
//    @Test
//    fun `notification displays default message when message is empty`() = runTest {
//        // Given
//        val intent = createAlarmIntent(alarmId = 1, message = "")
//        intent.action = AlarmService.ACTION_START_ALARM
//
//        // When
//        service.onStartCommand(intent, 0, 1)
//
//        // Then
//        val notifications = shadowNotificationManager.allNotifications
//        val notification = notifications.first()
//        assertEquals("Alarm is ringing", notification.contentText.toString())
//    }
//
//    // Helper function to create test intents
//    private fun createAlarmIntent(alarmId: Int, message: String): Intent {
//        val intent = Intent(context, AlarmService::class.java)
//        val intentData = AlarmActivityIntentData(
//            alarmIdInDb = alarmId,
//            message = message,
//            // Add other required fields based on your AlarmActivityIntentData class
//        )
//        intent.putExtra("intentData", intentData)
//        return intent
//    }
//}
//
///**
// * Additional test class for testing audio focus behavior with mocked AudioManager
// * This demonstrates how to test audio focus more thoroughly
// */
//@OptIn(ExperimentalCoroutinesApi::class)
//@RunWith(RobolectricTestRunner::class)
//@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
//class AlarmServiceAudioFocusTest {
//
//    private lateinit var context: Context
//    private lateinit var mockAudioManager: AudioManager
//    private val testDispatcher = UnconfinedTestDispatcher()
//
//    @Before
//    fun setup() {
//        context = ApplicationProvider.getApplicationContext()
//        mockAudioManager = mock()
//
//        // In production code, you'd inject AudioManager or use a wrapper class
//        // For this test, we'll verify behavior through integration
//    }
//
//    @Test
//    fun `audio focus request uses AUDIOFOCUS_GAIN_TRANSIENT`() {
//        // This test would require either:
//        // 1. Dependency injection of AudioManager
//        // 2. A wrapper class around audio focus logic
//        // 3. Reflection to inspect the AudioFocusRequest object
//
//        // Example with wrapper approach:
//        /*
//        val audioFocusWrapper = mock<AudioFocusWrapper>()
//        val service = AlarmService(audioFocusWrapper)
//
//        whenever(audioFocusWrapper.requestAudioFocus(any())).thenReturn(AUDIOFOCUS_REQUEST_GRANTED)
//
//        service.playAlarm()
//
//        argumentCaptor<AudioFocusRequest>().apply {
//            verify(audioFocusWrapper).requestAudioFocus(capture())
//            assertEquals(AUDIOFOCUS_GAIN_TRANSIENT, firstValue.focusGain)
//        }
//        */
//    }
//
//    @Test
//    fun `audio focus request uses USAGE_ALARM attribute`() {
//        // Similar to above - would need injectable dependencies to test thoroughly
//    }
//
//    @Test
//    fun `ringtone does not play when audio focus denied`() {
//        // Would need to mock RingtoneManager to verify play() is not called
//        // when requestAudioFocus returns AUDIOFOCUS_REQUEST_FAILED
//    }
//}