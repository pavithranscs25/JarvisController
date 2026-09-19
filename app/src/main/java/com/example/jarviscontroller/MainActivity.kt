package com.example.jarviscontroller

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

import com.example.jarviscontroller.ui.theme.JarvisScreen

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

import org.json.JSONObject

import java.io.IOException
import java.net.URLEncoder
import java.util.Locale


enum class AssistantMode {
    WAKE_WORD,
    ACTIVE_COMMAND
}

enum class JarvisVisualState{
    STANDBY,
    LISTENING,
    PROCESSING,
    SPEAKING
}

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    // ==================================================
    // BACKEND
    // ==================================================

    private val backendUrl =
        "http://10.38.101.184:8080/api/jarvis/ask"

    private val httpClient = OkHttpClient()

    private val mainHandler =
        Handler(Looper.getMainLooper())


    // ==================================================
    // SPEECH AND TTS
    // ==================================================

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null

    private var isTtsSpeaking = false
    private var shouldKeepListening = true


    // ==================================================
    // ASSISTANT STATE
    // ==================================================

    private var assistantMode by mutableStateOf(
        AssistantMode.WAKE_WORD
    )

    private var visualState by mutableStateOf(
        JarvisVisualState.STANDBY
    )

    private var listeningStatus by mutableStateOf(
        "Standby mode. Say Hey Jarvis"
    )

    private var recognizedText by mutableStateOf("")
    private var backendResponse by mutableStateOf("")
    private var isListening by mutableStateOf(false)


    // ==================================================
    // CALL STATE
    // ==================================================

    private var pendingPhoneNumber: String? = null
    private var pendingContactName: String? = null


    // ==================================================
    // MICROPHONE PERMISSION
    // ==================================================

    private val microphonePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->

            if (isGranted) {

                initializeSpeechRecognizer()
                startListeningAccordingToMode()

            } else {

                listeningStatus =
                    "Microphone permission is required"
            }
        }


    // ==================================================
    // CAMERA PERMISSION
    // ==================================================

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->

            if (isGranted) {

                openCamera()

            } else {

                Toast.makeText(
                    this,
                    "Camera permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


    // ==================================================
    // PHONE CALL PERMISSION
    // ==================================================

    private val callPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                pendingPhoneNumber?.let { number ->

                    makeDirectCallWithPermission(number)
                }

                pendingPhoneNumber = null

            } else {

                speakResponse(
                    "I need permission to make phone calls."
                )
            }
        }


    // ==================================================
    // CONTACTS PERMISSION
    // ==================================================

    private val contactsPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                pendingContactName?.let { name ->

                    callContactByName(name)
                }

                pendingContactName = null

            } else {

                speakResponse(
                    "I need permission to access your contacts."
                )
            }
        }


    // ==================================================
    // ACTIVITY CREATED
    // ==================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textToSpeech = TextToSpeech(
            this,
            this
        )

        setContent {
            JarvisScreen(
                listeningStatus = listeningStatus,
                recognizedText = recognizedText,
                backendResponse = backendResponse,
                assistantMode = assistantMode,
                isListening = isListening,
                visualState = visualState,

                onStartListening = {
                    shouldKeepListening = true
                    checkMicrophonePermission()
                },

                onStopListening = {
                    stopAssistant()
                },

                onCameraClick = {
                    checkCameraPermission()
                },

                onSendMessage = { message ->
                    sendTypedMessage(message)
                }
            )
        }

        checkMicrophonePermission()
    }


    // ==================================================
    // TYPED MESSAGE
    // ==================================================

    private fun sendTypedMessage(message: String) {

        if (message.isBlank()) return

        shouldKeepListening = false

        speechRecognizer?.cancel()

        recognizedText = message
        listeningStatus = "Processing your message..."

        visualState = JarvisVisualState.PROCESSING

        askBackend(message)
    }


    // ==================================================
    // PERMISSIONS
    // ==================================================

    private fun checkMicrophonePermission() {

        val permissionGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {

            if (speechRecognizer == null) {
                initializeSpeechRecognizer()
            }

            startListeningAccordingToMode()

        } else {

            microphonePermissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }


    private fun checkCameraPermission() {

        val permissionGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {

            openCamera()

        } else {

            cameraPermissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }


    // ==================================================
    // TEXT TO SPEECH
    // ==================================================

    override fun onInit(status: Int) {

        if (status == TextToSpeech.SUCCESS) {

            textToSpeech?.setLanguage(
                Locale.forLanguageTag("en-IN")
            )

            textToSpeech?.setSpeechRate(0.95f)
        }
    }


    private fun speakResponse(message: String) {

        if (message.isBlank()) return

        isTtsSpeaking = true
        visualState = JarvisVisualState.SPEAKING

        textToSpeech?.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {

                override fun onStart(utteranceId: String?) {
                    isTtsSpeaking = true
                }

                override fun onDone(utteranceId: String?) {

                    runOnUiThread {
                        isTtsSpeaking = false

                        if(shouldKeepListening){
                            visualState = JarvisVisualState.STANDBY
                        }
                    }
                }

                @Suppress("DEPRECATION")
                override fun onError(utteranceId: String?) {

                    runOnUiThread {
                        isTtsSpeaking = false
                        if(shouldKeepListening){
                            visualState = JarvisVisualState.STANDBY
                        }
                    }
                }
            }
        )

        textToSpeech?.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "JARVIS_RESPONSE"
        )
    }


    // ==================================================
    // SPEECH RECOGNIZER INITIALIZATION
    // ==================================================

    private fun initializeSpeechRecognizer() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {

            listeningStatus =
                "Speech recognition is not available"

            return
        }

        speechRecognizer?.destroy()

        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    visualState = JarvisVisualState.LISTENING
                }

                override fun onBeginningOfSpeech() {

                    isListening = true
                    visualState = JarvisVisualState.LISTENING

                    listeningStatus =
                        if (
                            assistantMode == AssistantMode.WAKE_WORD
                        ) {
                            "Listening for Hey Jarvis..."
                        } else {
                            "Listening for your command..."
                        }
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Not required
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    // Not required
                }

                override fun onEndOfSpeech() {
                    isListening = false

                    if(shouldKeepListening){
                        visualState = JarvisVisualState.PROCESSING
                    }
                }

                override fun onError(error: Int) {

                    isListening = false

                    if (!shouldKeepListening) return

                    restartListeningWithDelay(800L)
                }

                override fun onResults(results: Bundle?) {

                    isListening = false

                    val matches =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    val spokenText =
                        matches?.firstOrNull()

                    if (!spokenText.isNullOrBlank()) {

                        processRecognizedSpeech(spokenText)

                    } else {

                        restartListeningWithDelay(500L)
                    }
                }

                override fun onPartialResults(
                    partialResults: Bundle?
                ) {
                    // Not required
                }

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {
                    // Not required
                }
            }
        )
    }


    private fun createSpeechIntent(): Intent {

        return Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "en-IN"
            )

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "en-IN"
            )

            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                false
            )

            putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                1
            )
        }
    }


    // ==================================================
    // LISTENING CONTROL
    // ==================================================

    private fun startListeningAccordingToMode() {

        if (assistantMode == AssistantMode.WAKE_WORD) {

            startWakeWordListening()

        } else {

            startCommandListening()
        }
    }


    private fun startWakeWordListening() {

        if (!shouldKeepListening || isTtsSpeaking) return

        assistantMode = AssistantMode.WAKE_WORD

        listeningStatus =
            "Standby mode. Say Hey Jarvis"

        startSpeechRecognition()
    }


    private fun startCommandListening() {

        if (!shouldKeepListening || isTtsSpeaking) return

        assistantMode = AssistantMode.ACTIVE_COMMAND

        listeningStatus =
            "Active mode. Ask your command"

        startSpeechRecognition()
    }


    private fun startSpeechRecognition() {

        if (!shouldKeepListening || isTtsSpeaking) return

        try {

            speechRecognizer?.cancel()

            speechRecognizer?.startListening(
                createSpeechIntent()
            )

        } catch (e: Exception) {

            listeningStatus =
                "Restarting microphone..."

            restartListeningWithDelay(1000L)
        }
    }


    private fun restartListeningWithDelay(
        delayMillis: Long
    ) {

        if (!shouldKeepListening) return

        mainHandler.removeCallbacksAndMessages(null)

        mainHandler.postDelayed(
            {

                if (!shouldKeepListening) {
                    return@postDelayed
                }

                if (isTtsSpeaking) {
                    restartListeningWithDelay(700L)
                    return@postDelayed
                }

                startListeningAccordingToMode()
            },
            delayMillis
        )
    }


    // ==================================================
    // MAIN SPEECH PROCESSING
    // ==================================================

    private fun processRecognizedSpeech(text: String) {

        recognizedText = text

        val normalizedText =
            text.lowercase(Locale.getDefault()).trim()


        // --------------------------------------------------
        // WAKE WORD MODE
        // --------------------------------------------------

        if (assistantMode == AssistantMode.WAKE_WORD) {

            val wakeWordDetected =
                normalizedText.contains("hey jarvis") ||
                        normalizedText.contains("hey, jarvis") ||
                        normalizedText == "jarvis" ||
                        normalizedText.startsWith("jarvis ")

            if (wakeWordDetected) {

                assistantMode =
                    AssistantMode.ACTIVE_COMMAND

                listeningStatus =
                    "Active mode. Ask your command"

                speakResponse(
                    "Yes, how can I help?"
                )

                restartListeningWithDelay(2200L)

            } else {

                restartListeningWithDelay(500L)
            }

            return
        }


        // --------------------------------------------------
        // ACTIVE MODE: STANDBY COMMAND
        // --------------------------------------------------

        val standbyRequested =
            normalizedText.contains("standby") ||
                    normalizedText.contains("go to sleep") ||
                    normalizedText.contains("sleep mode") ||
                    normalizedText.contains("stop listening") ||
                    normalizedText.contains("stop jarvis")

        if (standbyRequested) {

            assistantMode =
                AssistantMode.WAKE_WORD

            listeningStatus =
                "Standby mode. Say Hey Jarvis"

            speakResponse(
                "Going to standby mode."
            )

            restartListeningWithDelay(2200L)

            return
        }


        // --------------------------------------------------
        // ACTIVE MODE: DIRECT CALL COMMAND
        // --------------------------------------------------

        val callCommand =
            extractCallTarget(normalizedText)

        if (callCommand != null) {

            shouldKeepListening = false

            speechRecognizer?.cancel()

            listeningStatus =
                "Calling $callCommand..."

            recognizedText = text

            makePhoneCall(callCommand)

            return
        }


        // --------------------------------------------------
        // ACTIVE MODE: SEND TO BACKEND
        // --------------------------------------------------

        listeningStatus =
            "Processing your command..."

        visualState = JarvisVisualState.PROCESSING
        shouldKeepListening = false

        askBackend(text)
    }

    // ==================================================
    // CALL COMMAND EXTRACTION
    // ==================================================

    private fun extractCallTarget(text: String): String? {

        val normalized = text.trim()

        val patterns = listOf(
            Regex("^call\\s+(.+)$", RegexOption.IGNORE_CASE),
            Regex("^phone\\s+(.+)$", RegexOption.IGNORE_CASE),
            Regex("^dial\\s+(.+)$", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {

            val match = pattern.find(normalized)

            if (match != null) {

                val target = match.groupValues[1].trim()

                if (target.isNotBlank()) {
                    return target
                }
            }
        }

        return null
    }

    // ==================================================
    // BACKEND API CALL
    // ==================================================

    private fun askBackend(question: String) {

        val json = JSONObject().apply {
            put("question", question)
        }

        val requestBody =
            json.toString().toRequestBody(
                "application/json; charset=utf-8".toMediaType()
            )

        val request =
            Request.Builder()
                .url(backendUrl)
                .post(requestBody)
                .build()

        httpClient.newCall(request).enqueue(
            object : Callback {

                override fun onFailure(
                    call: Call,
                    e: IOException
                ) {

                    runOnUiThread {

                        backendResponse =
                            "Backend connection failed: ${e.message}"

                        listeningStatus =
                            "Backend connection failed"

                        speakResponse(
                            "Sorry, I could not connect to the server."
                        )

                        shouldKeepListening = true
                        assistantMode = AssistantMode.ACTIVE_COMMAND

                        restartListeningWithDelay(2500L)
                    }
                }

                override fun onResponse(
                    call: Call,
                    response: okhttp3.Response
                ) {

                    response.use {

                        val isSuccessful =
                            it.isSuccessful

                        val responseCode =
                            it.code

                        val responseBody =
                            it.body?.string()

                        Log.d(
                            "JARVIS_API",
                            "Backend response: $responseBody"
                        )

                        runOnUiThread {

                            if (
                                isSuccessful &&
                                !responseBody.isNullOrBlank()
                            ) {

                                try {

                                    val jsonResponse =
                                        JSONObject(responseBody)

                                    val answer =
                                        jsonResponse.optString(
                                            "message",
                                            "No response received from Jarvis."
                                        )

                                    val action =
                                        jsonResponse.optString(
                                            "action",
                                            "NONE"
                                        )

                                    val query =
                                        jsonResponse.optString(
                                            "query",
                                            ""
                                        )

                                    val hour =
                                        jsonResponse.optInt(
                                            "hour",
                                            -1
                                        )

                                    val minute =
                                        jsonResponse.optInt(
                                            "minute",
                                            -1
                                        )

                                    backendResponse =
                                        answer

                                    Log.d(
                                        "JARVIS_ACTION",
                                        "Action: $action"
                                    )

                                    Log.d(
                                        "JARVIS_ACTION",
                                        "Query: $query"
                                    )

                                    Log.d(
                                        "JARVIS_ACTION",
                                        "Hour: $hour"
                                    )

                                    Log.d(
                                        "JARVIS_ACTION",
                                        "Minute: $minute"
                                    )

                                    handleAction(
                                        action = action,
                                        query = query,
                                        hour = hour,
                                        minute = minute
                                    )

                                    speakResponse(answer)

                                } catch (e: Exception) {

                                    Log.e(
                                        "JARVIS_API",
                                        "JSON parsing error",
                                        e
                                    )

                                    backendResponse =
                                        "Invalid JSON response from server."

                                    speakResponse(
                                        "I received an invalid response from the server."
                                    )
                                }

                            } else {

                                backendResponse =
                                    "Server error: $responseCode"

                                speakResponse(
                                    "Sorry, there was a server error."
                                )
                            }

                            shouldKeepListening = true

                            assistantMode =
                                AssistantMode.ACTIVE_COMMAND

                            listeningStatus =
                                "Active mode. Ask your next command"

                            restartListeningWithDelay(2500L)
                        }
                    }
                }
            }
        )
    }


    // ==================================================
    // ANDROID ACTION HANDLER
    // ==================================================

    private fun handleAction(
        action: String,
        query: String,
        hour: Int,
        minute: Int
    ) {

        when (action.uppercase(Locale.getDefault())) {

            "FLASHLIGHT_ON" -> {
                turnFlashlightOn()
            }

            "FLASHLIGHT_OFF" -> {
                turnFlashlightOff()
            }

            "OPEN_YOUTUBE" -> {
                openYouTubeApp()
            }

            "SEARCH_YOUTUBE",
            "PLAY_YOUTUBE" -> {

                val searchQuery = query.trim()

                if (searchQuery.isNotEmpty()) {

                    searchYouTubeApp(searchQuery)

                } else {

                    Toast.makeText(
                        this,
                        "What should I search on YouTube?",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            "OPEN_SPOTIFY" -> {
                openSpotifyApp()
            }

            "SEARCH_SPOTIFY",
            "PLAY_SPOTIFY" -> {

                val searchQuery = query.trim()

                if (searchQuery.isNotEmpty()) {

                    searchSpotifyApp(searchQuery)

                } else {

                    Toast.makeText(
                        this,
                        "What should I search on Spotify?",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            "OPEN_INSTAGRAM" -> {
                openInstagramApp()
            }

            "OPEN_CHROME" -> {
                openChrome()
            }

            "OPEN_WHATSAPP" -> {
                openWhatsApp()
            }

            "OPEN_GALLERY" -> {
                openGallery()
            }

            "OPEN_CALCULATOR" -> {
                openCalculator()
            }

            "OPEN_SETTINGS" -> {
                openSettings()
            }

            "OPEN_MAPS" -> {
                openMaps()
            }

            "OPEN_PHONE" -> {
                openPhone()
            }

            "CALL_CONTACT" -> {
                makePhoneCall(query)
            }

            "OPEN_MESSAGES" -> {
                openMessages()
            }

            "SEARCH_WEB" -> {

                val searchQuery = query.trim()

                if (searchQuery.isNotEmpty()) {

                    openWebSearch(searchQuery)

                } else {

                    Toast.makeText(
                        this,
                        "What should I search on the web?",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            "OPEN_CAMERA" -> {
                checkCameraPermission()
            }

            "SET_ALARM" -> {

                if (hour in 0..23 && minute in 0..59) {

                    setAlarm(
                        hour = hour,
                        minute = minute
                    )

                } else {

                    Toast.makeText(
                        this,
                        "Invalid alarm time",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            "NONE" -> {
                // Normal question; no Android action
            }

            else -> {

                Log.d(
                    "JARVIS_ACTION",
                    "Unknown action: $action"
                )
            }
        }
    }


    // ==================================================
    // YOUTUBE APP ACTIONS
    // ==================================================

    private fun openYouTubeApp() {

        try {

            val launchIntent =
                packageManager.getLaunchIntentForPackage(
                    "com.google.android.youtube"
                )

            if (launchIntent != null) {

                startActivity(launchIntent)

            } else {

                Toast.makeText(
                    this,
                    "YouTube app is not installed",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to open YouTube app",
                Toast.LENGTH_SHORT
            ).show()

            Log.e(
                "YOUTUBE",
                "YouTube opening failed",
                e
            )
        }
    }


    private fun searchYouTubeApp(query: String) {

        try {

            val encodedQuery =
                URLEncoder.encode(query, "UTF-8")

            val youtubeUrl =
                "https://www.youtube.com/results?search_query=$encodedQuery"

            val youtubeIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(youtubeUrl)
            ).apply {
                setPackage("com.google.android.youtube")
            }

            if (
                youtubeIntent.resolveActivity(packageManager) != null
            ) {

                startActivity(youtubeIntent)

            } else {

                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(youtubeUrl)
                )

                startActivity(browserIntent)
            }

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to search YouTube",
                Toast.LENGTH_SHORT
            ).show()

            Log.e(
                "YOUTUBE",
                "YouTube search failed",
                e
            )
        }
    }


    // ==================================================
    // SPOTIFY APP ACTIONS
    // ==================================================

    private fun openSpotifyApp() {

        try {

            val launchIntent =
                packageManager.getLaunchIntentForPackage(
                    "com.spotify.music"
                )

            if (launchIntent != null) {

                startActivity(launchIntent)

            } else {

                Toast.makeText(
                    this,
                    "Spotify app is not installed",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to open Spotify app",
                Toast.LENGTH_SHORT
            ).show()

            Log.e(
                "SPOTIFY",
                "Spotify opening failed",
                e
            )
        }
    }


    private fun searchSpotifyApp(query: String) {

        try {

            val encodedQuery =
                URLEncoder.encode(query, "UTF-8")

            val spotifyIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("spotify:search:$encodedQuery")
            ).apply {
                setPackage("com.spotify.music")
            }

            if (
                spotifyIntent.resolveActivity(packageManager) != null
            ) {

                startActivity(spotifyIntent)

            } else {

                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://open.spotify.com/search/$encodedQuery"
                    )
                )

                startActivity(browserIntent)
            }

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to search Spotify",
                Toast.LENGTH_SHORT
            ).show()

            Log.e(
                "SPOTIFY",
                "Spotify search failed",
                e
            )
        }
    }


    // ==================================================
    // INSTAGRAM APP ACTIONS
    // ==================================================

    private fun openInstagramApp() {

        try {

            val launchIntent =
                packageManager.getLaunchIntentForPackage(
                    "com.instagram.android"
                )

            if (launchIntent != null) {

                startActivity(launchIntent)

            } else {

                Toast.makeText(
                    this,
                    "Instagram app is not installed",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to open Instagram app",
                Toast.LENGTH_SHORT
            ).show()

            Log.e(
                "INSTAGRAM",
                "Instagram opening failed",
                e
            )
        }
    }


    // ==================================================
    // WHATSAPP APP ACTIONS
    // ==================================================

    private fun openWhatsApp() {

        try {

            val normalWhatsApp =
                packageManager.getLaunchIntentForPackage(
                    "com.whatsapp"
                )

            val businessWhatsApp =
                packageManager.getLaunchIntentForPackage(
                    "com.whatsapp.w4b"
                )

            val whatsappIntent =
                normalWhatsApp ?: businessWhatsApp

            if (whatsappIntent != null) {

                startActivity(whatsappIntent)

            } else {

                Toast.makeText(
                    this,
                    "WhatsApp is not installed",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to open WhatsApp",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // ==================================================
    // GALLERY
    // ==================================================

    private fun openGallery() {

        try {

            val galleryIntent = Intent(
                Intent.ACTION_VIEW
            ).apply {

                type = "image/*"
            }

            startActivity(galleryIntent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Gallery app not found",
                Toast.LENGTH_SHORT
            ).show()

            Log.e(
                "GALLERY",
                "Gallery opening failed",
                e
            )
        }
    }


    // ==================================================
    // CALCULATOR
    // ==================================================

    private fun openCalculator() {

        val calculatorPackages = listOf(
            "com.google.android.calculator",
            "com.android.calculator2",
            "com.sec.android.app.popupcalculator"
        )

        var calculatorOpened = false

        for (packageName in calculatorPackages) {

            val launchIntent =
                packageManager.getLaunchIntentForPackage(
                    packageName
                )

            if (launchIntent != null) {

                startActivity(launchIntent)

                calculatorOpened = true

                break
            }
        }

        if (!calculatorOpened) {

            Toast.makeText(
                this,
                "Calculator app not found",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // ==================================================
    // SETTINGS
    // ==================================================

    private fun openSettings() {

        try {

            val settingsIntent = Intent(
                Settings.ACTION_SETTINGS
            )

            startActivity(settingsIntent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to open settings",
                Toast.LENGTH_SHORT
            ).show()

            Log.e(
                "SETTINGS",
                "Settings opening failed",
                e
            )
        }
    }


    // ==================================================
    // GOOGLE MAPS
    // ==================================================

    private fun openMaps() {

        try {

            val mapsIntent =
                packageManager.getLaunchIntentForPackage(
                    "com.google.android.apps.maps"
                )

            if (mapsIntent != null) {

                startActivity(mapsIntent)

            } else {

                val browserMapsIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://maps.google.com")
                )

                startActivity(browserMapsIntent)
            }

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to open Google Maps",
                Toast.LENGTH_SHORT
            ).show()

            Log.e(
                "MAPS",
                "Maps opening failed",
                e
            )
        }
    }


    // ==================================================
    // PHONE / DIALER
    // ==================================================

    private fun openPhone() {

        try {

            val phoneIntent = Intent(
                Intent.ACTION_DIAL
            )

            startActivity(phoneIntent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to open phone dialer",
                Toast.LENGTH_SHORT
            ).show()

            Log.e(
                "PHONE",
                "Phone opening failed",
                e
            )
        }
    }


    // ==================================================
    // CALLING
    // ==================================================

    private fun makePhoneCall(query: String) {

        val contactOrNumber =
            query.trim()

        if (contactOrNumber.isBlank()) {

            Toast.makeText(
                this,
                "Whom should I call?",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        /*
         * If query contains a phone number,
         * directly call that number.
         *
         * Example:
         * "9876543210"
         * "+919876543210"
         */

        val digitCount =
            contactOrNumber.count { it.isDigit() }

        if (digitCount >= 7) {

            makeDirectCallWithPermission(
                contactOrNumber
            )

            return
        }

        /*
         * Otherwise, treat query as contact name.
         *
         * Example:
         * "Call Amma"
         * "Call Arun"
         */

        val contactsPermissionGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED

        if (contactsPermissionGranted) {

            callContactByName(contactOrNumber)

        } else {

            pendingContactName =
                contactOrNumber

            contactsPermissionLauncher.launch(
                Manifest.permission.READ_CONTACTS
            )
        }
    }


    private fun makeDirectCallWithPermission(phoneNumber: String) {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            pendingPhoneNumber = phoneNumber

            callPermissionLauncher.launch(
                Manifest.permission.CALL_PHONE
            )

            return
        }

        try {

            val cleanNumber =
                phoneNumber.trim()

            val intent = Intent(
                Intent.ACTION_CALL,
                Uri.parse("tel:${Uri.encode(cleanNumber)}")
            )

            startActivity(intent)

        } catch (e: Exception) {

            Log.e(
                "JARVIS_CALL",
                "Could not place call",
                e
            )

            speakResponse(
                "I couldn't make the call."
            )
        }
    }


    private fun callContactByName(name: String) {

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )

        val selection =
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"

        val selectionArgs =
            arrayOf("%$name%")

        try {

            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->

                if (cursor.moveToFirst()) {

                    val numberIndex =
                        cursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                        )

                    val displayNameIndex =
                        cursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                        )

                    if (numberIndex >= 0) {

                        val phoneNumber =
                            cursor.getString(numberIndex)

                        val displayName =
                            if (displayNameIndex >= 0) {
                                cursor.getString(displayNameIndex)
                            } else {
                                name
                            }

                        Log.d(
                            "JARVIS_CALL",
                            "Found contact: $displayName -> $phoneNumber"
                        )

                        speakResponse(
                            "Calling $displayName"
                        )

                        makeDirectCallWithPermission(
                            phoneNumber
                        )

                    } else {

                        speakResponse(
                            "I couldn't find a phone number for $name."
                        )
                    }

                } else {

                    Log.d(
                        "JARVIS_CALL",
                        "Contact not found: $name"
                    )

                    speakResponse(
                        "I couldn't find $name in your contacts."
                    )
                }
            }

        } catch (e: Exception) {

            Log.e(
                "JARVIS_CALL",
                "Contact lookup failed",
                e
            )

            speakResponse(
                "I couldn't access that contact."
            )
        }
    }


    // ==================================================
    // MESSAGES / SMS
    // ==================================================

    private fun openMessages() {

        try {

            val messagesIntent = Intent(
                Intent.ACTION_MAIN
            ).apply {

                addCategory(
                    Intent.CATEGORY_APP_MESSAGING
                )
            }

            startActivity(messagesIntent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Messages app not found",
                Toast.LENGTH_SHORT
            ).show()

            Log.e(
                "MESSAGES",
                "Messages opening failed",
                e
            )
        }
    }


    // ==================================================
    // FLASHLIGHT ACTIONS
    // ==================================================

    private fun getFlashCameraId(): String? {

        return try {

            val cameraManager =
                getSystemService(
                    CAMERA_SERVICE
                ) as android.hardware.camera2.CameraManager

            cameraManager.cameraIdList.firstOrNull { cameraId ->

                val characteristics =
                    cameraManager.getCameraCharacteristics(
                        cameraId
                    )

                characteristics.get(
                    android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE
                ) == true
            }

        } catch (e: Exception) {

            null
        }
    }


    private fun turnFlashlightOn() {

        try {

            val cameraManager =
                getSystemService(
                    CAMERA_SERVICE
                ) as android.hardware.camera2.CameraManager

            val cameraId =
                getFlashCameraId()

            if (cameraId != null) {

                cameraManager.setTorchMode(
                    cameraId,
                    true
                )

                Toast.makeText(
                    this,
                    "Flashlight ON",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                Toast.makeText(
                    this,
                    "Flashlight is not available",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to turn on flashlight",
                Toast.LENGTH_SHORT
            ).show()

            Log.e(
                "FLASHLIGHT",
                "Flashlight ON failed",
                e
            )
        }
    }


    private fun turnFlashlightOff() {

        try {

            val cameraManager =
                getSystemService(
                    CAMERA_SERVICE
                ) as android.hardware.camera2.CameraManager

            val cameraId =
                getFlashCameraId()

            if (cameraId != null) {

                cameraManager.setTorchMode(
                    cameraId,
                    false
                )

                Toast.makeText(
                    this,
                    "Flashlight OFF",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                Toast.makeText(
                    this,
                    "Flashlight is not available",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to turn off flashlight",
                Toast.LENGTH_SHORT
            ).show()

            Log.e(
                "FLASHLIGHT",
                "Flashlight OFF failed",
                e
            )
        }
    }


    // ==================================================
    // CHROME
    // ==================================================

    private fun openChrome() {

        try {

            val chromeIntent =
                packageManager.getLaunchIntentForPackage(
                    "com.android.chrome"
                )

            if (chromeIntent != null) {

                startActivity(chromeIntent)

            } else {

                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com")
                )

                startActivity(browserIntent)
            }

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to open Chrome",
                Toast.LENGTH_SHORT
            ).show()

            Log.e(
                "CHROME",
                "Chrome opening failed",
                e
            )
        }
    }


    // ==================================================
    // WEB SEARCH
    // ==================================================

    private fun openWebSearch(query: String) {

        if (query.isBlank()) return

        val searchUrl =
            "https://www.google.com/search?q=" +
                    Uri.encode(query)

        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(searchUrl)
        )

        try {

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to open web search",
                Toast.LENGTH_SHORT
            ).show()

            Log.e(
                "WEB_SEARCH",
                "Web search failed",
                e
            )
        }
    }


    // ==================================================
    // CAMERA
    // ==================================================

    private fun openCamera() {

        val intent = Intent(
            MediaStore.ACTION_IMAGE_CAPTURE
        )

        try {

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Camera app not found",
                Toast.LENGTH_SHORT
            ).show()

            Log.e(
                "CAMERA",
                "Camera opening failed",
                e
            )
        }
    }


    // ==================================================
    // ALARM
    // ==================================================

    private fun setAlarm(
        hour: Int,
        minute: Int
    ) {

        try {

            val alarmIntent = Intent(
                AlarmClock.ACTION_SET_ALARM
            ).apply {

                putExtra(
                    AlarmClock.EXTRA_HOUR,
                    hour
                )

                putExtra(
                    AlarmClock.EXTRA_MINUTES,
                    minute
                )

                putExtra(
                    AlarmClock.EXTRA_MESSAGE,
                    "Jarvis Alarm"
                )

                putExtra(
                    AlarmClock.EXTRA_SKIP_UI,
                    false
                )
            }

            startActivity(alarmIntent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to set alarm",
                Toast.LENGTH_SHORT
            ).show()

            Log.e(
                "ALARM",
                "Alarm setting failed",
                e
            )
        }
    }


    // ==================================================
    // STOP ASSISTANT
    // ==================================================

    private fun stopAssistant() {

        shouldKeepListening = false
        isListening = false
        isTtsSpeaking = false

        visualState = JarvisVisualState.STANDBY

        mainHandler.removeCallbacksAndMessages(null)

        speechRecognizer?.cancel()
        textToSpeech?.stop()

        listeningStatus =
            "Assistant stopped"
    }


    // ==================================================
    // ACTIVITY DESTROYED
    // ==================================================

    override fun onDestroy() {

        shouldKeepListening = false

        mainHandler.removeCallbacksAndMessages(null)

        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null

        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null

        httpClient.dispatcher.cancelAll()

        super.onDestroy()
    }
}