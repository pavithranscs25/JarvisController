package com.example.jarviscontroller.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarviscontroller.AssistantMode
import com.example.jarviscontroller.JarvisVisualState
import kotlin.math.cos
import kotlin.math.sin

// ======================================================
// COLORS
// ======================================================

private val BackgroundColor = Color(0xFF030611)
private val TopBarColor = Color(0xFF070B17)

private val CardColor = Color(0xFF0B1020)
private val CardColorLight = Color(0xFF10182A)

private val CyanColor = Color(0xFF28D9F5)
private val CyanBright = Color(0xFF7DEFFF)

private val PurpleColor = Color(0xFFB477FF)
private val PurpleBright = Color(0xFFD0A8FF)

private val GreenColor = Color(0xFF00D084)

private val MutedText = Color(0xFF7E8BA8)
private val SecondaryText = Color(0xFFAAB5CC)


// ======================================================
// MAIN SCREEN
// ======================================================

@Composable
fun JarvisScreen(
    listeningStatus: String,
    recognizedText: String,
    backendResponse: String,
    assistantMode: AssistantMode,
    isListening: Boolean,
    visualState: JarvisVisualState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onCameraClick: () -> Unit,
    onSendMessage: (String) -> Unit
) {

    var inputText by remember {
        mutableStateOf("")
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundColor
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            // ==================================================
            // TOP BAR
            // ==================================================

            JarvisTopBar(
                isOnline = true
            )

            // ==================================================
            // MAIN CONTENT
            // ==================================================

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                item {

                    Spacer(
                        modifier = Modifier.height(28.dp)
                    )

                    // ==================================================
                    // ORB
                    // ==================================================

                    JarvisOrb(
                        modifier = Modifier.size(270.dp),
                        visualState = visualState
                    )

                    Spacer(
                        modifier = Modifier.height(18.dp)
                    )

                    // ==================================================
                    // MAIN TITLE
                    // ==================================================

                    Text(
                        text = when (visualState) {
                            JarvisVisualState.STANDBY ->
                                "How can I assist you?"

                            JarvisVisualState.LISTENING ->
                                "Listening..."

                            JarvisVisualState.PROCESSING ->
                                "Thinking..."

                            JarvisVisualState.SPEAKING ->
                                "Speaking..."
                        },
                        color = Color.White,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(
                        modifier = Modifier.height(7.dp)
                    )

                    Text(
                        text = listeningStatus,
                        color = when (visualState) {
                            JarvisVisualState.LISTENING ->
                                CyanColor

                            JarvisVisualState.PROCESSING ->
                                PurpleColor

                            JarvisVisualState.SPEAKING ->
                                GreenColor

                            JarvisVisualState.STANDBY ->
                                MutedText
                        },
                        fontSize = 13.sp,
                        fontWeight = if (
                            visualState != JarvisVisualState.STANDBY
                        ) {
                            FontWeight.Medium
                        } else {
                            FontWeight.Normal
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(25.dp)
                    )
                }

                // ==================================================
                // USER MESSAGE
                // ==================================================

                if (recognizedText.isNotBlank()) {

                    item {

                        ConversationCard(
                            title = "YOU",
                            content = recognizedText,
                            titleColor = CyanColor,
                            cardBackground = Color(0xFF071722),
                            borderColor = Color(0xFF12445A)
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )
                    }
                }

                // ==================================================
                // JARVIS RESPONSE
                // ==================================================

                if (backendResponse.isNotBlank()) {

                    item {

                        ConversationCard(
                            title = "JARVIS",
                            content = backendResponse,
                            titleColor = PurpleColor,
                            cardBackground = CardColor,
                            borderColor = Color(0xFF28253D)
                        )

                        Spacer(
                            modifier = Modifier.height(20.dp)
                        )
                    }
                }

                // ==================================================
                // QUICK ACTIONS
                // ==================================================

                item {

                    QuickActionButtons(
                        isListening = isListening,
                        onStartListening = onStartListening,
                        onStopListening = onStopListening,
                        onCameraClick = onCameraClick
                    )

                    Spacer(
                        modifier = Modifier.height(25.dp)
                    )
                }

                // ==================================================
                // INPUT
                // ==================================================

                item {

                    AskJarvisInput(
                        inputText = inputText,

                        onInputChange = {
                            inputText = it
                        },

                        onVoiceClick = onStartListening,

                        onSendClick = {

                            if (inputText.trim().isNotEmpty()) {

                                onSendMessage(
                                    inputText.trim()
                                )

                                inputText = ""
                            }
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(35.dp)
                    )
                }
            }
        }
    }
}


// ======================================================
// TOP BAR
// ======================================================

@Composable
private fun JarvisTopBar(
    isOnline: Boolean
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                TopBarColor
            )
            .padding(
                horizontal = 20.dp,
                vertical = 15.dp
            ),

        verticalAlignment = Alignment.CenterVertically
    ) {

        // ==================================================
        // JARVIS ICON
        // ==================================================

        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(
                    RoundedCornerShape(14.dp)
                )
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0A3344),
                            Color(0xFF15112E)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF164A61),
                    shape = RoundedCornerShape(14.dp)
                ),

            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "✦",
                color = CyanBright,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.width(13.dp)
        )

        // ==================================================
        // TITLE
        // ==================================================

        Column {

            Text(
                text = "JARVIS",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Text(
                text = "Personal AI Assistant",
                color = MutedText,
                fontSize = 11.sp
            )
        }

        Spacer(
            modifier = Modifier.weight(1f)
        )

        // ==================================================
        // ONLINE STATUS
        // ==================================================

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (isOnline) {
                        Color(0xFF06251C)
                    } else {
                        Color(0xFF2A0B10)
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (isOnline) {
                        Color(0xFF0D5C43)
                    } else {
                        Color(0xFF6A1720)
                    },
                    shape = CircleShape
                )
                .padding(
                    horizontal = 10.dp,
                    vertical = 6.dp
                ),

            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        if (isOnline) {
                            GreenColor
                        } else {
                            Color.Red
                        }
                    )
            )

            Spacer(
                modifier = Modifier.width(6.dp)
            )

            Text(
                text = if (isOnline) {
                    "Online"
                } else {
                    "Offline"
                },

                color = if (isOnline) {
                    GreenColor
                } else {
                    Color.Red
                },

                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


// ======================================================
// ANIMATED JARVIS ORB
// ======================================================

@Composable
private fun JarvisOrb(
    modifier: Modifier = Modifier,
    visualState: JarvisVisualState
) {
    val infiniteTransition =
        rememberInfiniteTransition(label = "jarvis_orb")

    val isListening =
        visualState == JarvisVisualState.LISTENING

    val isProcessing =
        visualState == JarvisVisualState.PROCESSING

    val isSpeaking =
        visualState == JarvisVisualState.SPEAKING

    val isStandby =
        visualState == JarvisVisualState.STANDBY

    // ---------------------------------------------
    // ROTATION SPEED
    // ---------------------------------------------

    val rotationDuration =
        when {
            isListening -> 3200
            isProcessing -> 1800
            isSpeaking -> 4500
            else -> 7500
        }

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = rotationDuration,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_rotation"
    )

    // ---------------------------------------------
    // MAIN ORB PULSE
    // ---------------------------------------------

    val pulseMin =
        when {
            isListening -> 0.96f
            isProcessing -> 0.92f
            isSpeaking -> 0.97f
            else -> 0.98f
        }

    val pulseMax =
        when {
            isListening -> 1.12f
            isProcessing -> 1.16f
            isSpeaking -> 1.08f
            else -> 1.04f
        }

    val pulseDuration =
        when {
            isListening -> 700
            isProcessing -> 450
            isSpeaking -> 1000
            else -> 1800
        }

    val pulse by infiniteTransition.animateFloat(
        initialValue = pulseMin,
        targetValue = pulseMax,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = pulseDuration,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    // ---------------------------------------------
    // GLOW PULSE
    // ---------------------------------------------

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = when {
            isListening -> 0.90f
            isProcessing -> 1.0f
            isSpeaking -> 0.85f
            else -> 0.50f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    isProcessing -> 500
                    isListening -> 750
                    isSpeaking -> 1100
                    else -> 2000
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_glow"
    )

    Canvas(modifier = modifier) {

        val center = Offset(
            x = size.width / 2f,
            y = size.height / 2f
        )

        val minSize = size.minDimension

        val outerRadius =
            minSize * 0.455f

        val innerRingRadius =
            minSize * 0.335f

        val orbRadius =
            minSize * 0.245f * pulse

        // ---------------------------------------------
        // STATE COLORS
        // ---------------------------------------------

        val primaryColor =
            when {
                isListening ->
                    Color(0xFF24E7FF)

                isProcessing ->
                    Color(0xFFB36CFF)

                isSpeaking ->
                    Color(0xFF00E6A8)

                else ->
                    Color(0xFF39BFFF)
            }

        val secondaryColor =
            when {
                isListening ->
                    Color(0xFF2488FF)

                isProcessing ->
                    Color(0xFF7047FF)

                isSpeaking ->
                    Color(0xFF00A8FF)

                else ->
                    Color(0xFF8C5CFF)
            }

        val brightColor =
            when {
                isListening ->
                    Color(0xFF9CF7FF)

                isProcessing ->
                    Color(0xFFE0B5FF)

                isSpeaking ->
                    Color(0xFF9CFFE1)

                else ->
                    Color(0xFF9CEBFF)
            }

        // ---------------------------------------------
        // OUTER GLOW
        // ---------------------------------------------

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(
                        alpha = glowAlpha
                    ),
                    secondaryColor.copy(
                        alpha = 0.22f
                    ),
                    Color.Transparent
                ),
                center = center,
                radius = minSize * 0.54f
            ),
            radius = minSize * 0.54f,
            center = center
        )

        // ---------------------------------------------
        // SECONDARY GLOW
        // ---------------------------------------------

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondaryColor.copy(alpha = 0.28f),
                    Color.Transparent
                ),
                center = Offset(
                    center.x + minSize * 0.15f,
                    center.y + minSize * 0.15f
                ),
                radius = minSize * 0.48f
            ),
            radius = minSize * 0.48f,
            center = Offset(
                center.x + minSize * 0.15f,
                center.y + minSize * 0.15f
            )
        )

        // ---------------------------------------------
        // OUTER CIRCLE
        // ---------------------------------------------

        drawCircle(
            color = primaryColor.copy(
                alpha = if (isStandby) 0.45f else 0.85f
            ),
            radius = outerRadius,
            center = center,
            style = Stroke(
                width = 2.dp.toPx()
            )
        )

        // ---------------------------------------------
        // MAIN ROTATING ARC
        // ---------------------------------------------

        drawArc(
            color = brightColor,
            startAngle = rotation,
            sweepAngle = 245f,
            useCenter = false,
            topLeft = Offset(
                center.x - outerRadius,
                center.y - outerRadius
            ),
            size = Size(
                outerRadius * 2,
                outerRadius * 2
            ),
            style = Stroke(
                width = if (isProcessing) {
                    7.dp.toPx()
                } else {
                    5.dp.toPx()
                },
                cap = StrokeCap.Round
            )
        )

        // ---------------------------------------------
        // SECOND ROTATING ARC
        // ---------------------------------------------

        drawArc(
            color = secondaryColor.copy(alpha = 0.85f),
            startAngle = rotation + 180f,
            sweepAngle = if (isProcessing) 125f else 90f,
            useCenter = false,
            topLeft = Offset(
                center.x - outerRadius,
                center.y - outerRadius
            ),
            size = Size(
                outerRadius * 2,
                outerRadius * 2
            ),
            style = Stroke(
                width = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        )

        // ---------------------------------------------
        // ROTATING ENERGY MARKERS
        // ---------------------------------------------

        for (index in 0 until 36) {

            val angle =
                Math.toRadians(
                    (index * 10f + rotation).toDouble()
                )

            val startRadius =
                minSize * 0.365f

            val endRadius =
                minSize * 0.405f

            val start = Offset(
                x = center.x +
                        cos(angle).toFloat() *
                        startRadius,
                y = center.y +
                        sin(angle).toFloat() *
                        startRadius
            )

            val end = Offset(
                x = center.x +
                        cos(angle).toFloat() *
                        endRadius,
                y = center.y +
                        sin(angle).toFloat() *
                        endRadius
            )

            drawLine(
                color =
                    if (index % 4 == 0) {
                        brightColor
                    } else {
                        primaryColor.copy(alpha = 0.70f)
                    },
                start = start,
                end = end,
                strokeWidth =
                    if (index % 4 == 0) {
                        6.dp.toPx()
                    } else {
                        3.dp.toPx()
                    },
                cap = StrokeCap.Round
            )
        }

        // ---------------------------------------------
        // INNER RING
        // ---------------------------------------------

        drawCircle(
            color = primaryColor.copy(
                alpha = if (isStandby) 0.55f else 0.85f
            ),
            radius = innerRingRadius,
            center = center,
            style = Stroke(
                width = 2.dp.toPx()
            )
        )

        drawCircle(
            color = secondaryColor.copy(alpha = 0.45f),
            radius = minSize * 0.305f,
            center = center,
            style = Stroke(
                width = 2.dp.toPx()
            )
        )

        // ---------------------------------------------
        // CENTRAL ORB
        // ---------------------------------------------

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    brightColor,
                    primaryColor,
                    secondaryColor,
                    Color(0xFF11162F)
                ),
                center = Offset(
                    center.x - orbRadius * 0.25f,
                    center.y - orbRadius * 0.3f
                ),
                radius = orbRadius * 1.55f
            ),
            radius = orbRadius,
            center = center
        )

        // ---------------------------------------------
        // CENTRAL ORB BORDER
        // ---------------------------------------------

        drawCircle(
            color = brightColor.copy(alpha = 0.95f),
            radius = orbRadius,
            center = center,
            style = Stroke(
                width = 2.5.dp.toPx()
            )
        )

        // ---------------------------------------------
        // JARVIS SYMBOL
        // ---------------------------------------------

        val symbolWidth = 12.dp.toPx()
        val symbolHeight = 27.dp.toPx()

        drawLine(
            color = Color.White.copy(alpha = 0.95f),
            start = Offset(
                center.x - symbolWidth,
                center.y
            ),
            end = Offset(
                center.x + symbolWidth,
                center.y
            ),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawLine(
            color = Color.White.copy(alpha = 0.95f),
            start = Offset(
                center.x,
                center.y - symbolHeight
            ),
            end = Offset(
                center.x,
                center.y + symbolHeight
            ),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = center
        )

        // ---------------------------------------------
        // ORBITING DOTS
        // ---------------------------------------------

        drawCircle(
            color = brightColor,
            radius = 5.dp.toPx(),
            center = Offset(
                center.x - minSize * 0.40f,
                center.y - minSize * 0.27f
            )
        )

        drawCircle(
            color = secondaryColor,
            radius = 5.dp.toPx(),
            center = Offset(
                center.x + minSize * 0.40f,
                center.y + minSize * 0.27f
            )
        )

        drawCircle(
            color = brightColor,
            radius = 4.dp.toPx(),
            center = Offset(
                center.x + minSize * 0.46f,
                center.y - minSize * 0.39f
            )
        )

        drawCircle(
            color = secondaryColor,
            radius = 4.dp.toPx(),
            center = Offset(
                center.x - minSize * 0.44f,
                center.y + minSize * 0.39f
            )
        )
    }
}


// ======================================================
// CONVERSATION CARD
// ======================================================

@Composable
private fun ConversationCard(
    title: String,
    content: String,
    titleColor: Color,
    cardBackground: Color,
    borderColor: Color
) {

    Column(

        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(18.dp)
            )
            .background(
                cardBackground
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(17.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(titleColor)
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text = title,
                color = titleColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.3.sp
            )
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Text(
            text = content,
            color = Color.White,
            fontSize = 15.sp,
            lineHeight = 23.sp
        )
    }
}


// ======================================================
// QUICK ACTION BUTTONS
// ======================================================

@Composable
private fun QuickActionButtons(
    isListening: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onCameraClick: () -> Unit
) {

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ==================================================
            // VOICE BUTTON
            // ==================================================

            Button(

                onClick = {

                    if (isListening) {
                        onStopListening()
                    } else {
                        onStartListening()
                    }
                },

                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),

                shape = RoundedCornerShape(15.dp),

                colors = ButtonDefaults.buttonColors(

                    containerColor = if (isListening) {
                        Color(0xFF35131C)
                    } else {
                        Color(0xFF0C2B3D)
                    },

                    contentColor = if (isListening) {
                        Color(0xFFFF7184)
                    } else {
                        CyanBright
                    }
                )
            ) {

                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text = if (isListening) {
                        "Stop Voice"
                    } else {
                        "Voice Mode"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // ==================================================
            // CAMERA BUTTON
            // ==================================================

            Button(

                onClick = onCameraClick,

                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),

                shape = RoundedCornerShape(15.dp),

                colors = ButtonDefaults.buttonColors(

                    containerColor = Color(0xFF12192B),

                    contentColor = PurpleBright
                )
            ) {

                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text = "Camera",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


// ======================================================
// ASK JARVIS INPUT
// ======================================================

@Composable
private fun AskJarvisInput(
    inputText: String,
    onInputChange: (String) -> Unit,
    onVoiceClick: () -> Unit,
    onSendClick: () -> Unit
) {

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        // ==================================================
        // TITLE
        // ==================================================

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Ask Jarvis",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(CyanColor)
            )
        }

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        Text(
            text = "Type a question or use voice mode.",
            color = MutedText,
            fontSize = 12.sp
        )

        Spacer(
            modifier = Modifier.height(11.dp)
        )

        // ==================================================
        // INPUT ROW
        // ==================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            OutlinedTextField(

                value = inputText,

                onValueChange = onInputChange,

                modifier = Modifier.weight(1f),

                placeholder = {

                    Text(
                        text = "Ask me anything...",
                        color = MutedText,
                        fontSize = 14.sp
                    )
                },

                singleLine = true,

                trailingIcon = {

                    IconButton(
                        onClick = onVoiceClick
                    ) {

                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice input",
                            tint = CyanColor
                        )
                    }
                },

                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),

                shape = RoundedCornerShape(17.dp),

                colors = androidx.compose.material3
                    .OutlinedTextFieldDefaults.colors(

                        focusedTextColor = Color.White,

                        unfocusedTextColor = Color.White,

                        focusedBorderColor = CyanColor,

                        unfocusedBorderColor = Color(0xFF222C42),

                        focusedContainerColor = Color(0xFF080D1A),

                        unfocusedContainerColor = Color(0xFF080D1A),

                        cursorColor = CyanBright
                    )
            )

            Spacer(
                modifier = Modifier.width(9.dp)
            )

            // ==================================================
            // SEND BUTTON
            // ==================================================

            IconButton(

                onClick = onSendClick,

                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                CyanColor,
                                Color(0xFF4B9DFF)
                            )
                        )
                    )
            ) {

                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send message",
                    tint = BackgroundColor,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}