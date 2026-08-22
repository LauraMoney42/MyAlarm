package com.kindcode.alarmhub.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Canvas units from the design.
private const val CARD_W = 580f
private const val CARD_H = 684f
private const val CARD_R = 38f
private const val FACE_SIZE = 452f

// The design runs two 260ms halves back to back rather than one long flip.
private const val HALF_MS = 260
private val FALL_EASE = CubicBezierEasing(0.5f, 0f, 0.8f, 0.4f)
private val LAND_EASE = CubicBezierEasing(0.2f, 0.7f, 0.5f, 1f)

/**
 * One face of the split-flap, cropped to its top or bottom half.
 *
 * requiredSize is load bearing: without it the parent's half-height constraint
 * would squash the face instead of cropping it.
 */
@Composable
private fun Half(
    value: String,
    top: Boolean,
    corner: String?,
    digitColor: Color,
    cardColor: Color,
    modifier: Modifier = Modifier,
) {
    val w = du(CARD_W)
    val h = du(CARD_H)
    Box(modifier.size(w, h / 2).clipToBounds()) {
        Box(
            Modifier
                .requiredSize(w, h / 2)
                // The top leaf is lifted a touch so the hinge reads as a fold
                // rather than a drawn line, whatever colour the card is.
                .background(
                    if (top) lerp(cardColor, Color.White, 0.035f) else cardColor
                )
        )
        Box(Modifier.offset(y = if (top) 0.dp else -(h / 2))) {
            Box(Modifier.requiredSize(w, h), contentAlignment = Alignment.Center) {
                Text(
                    text = value,
                    style = TextStyle(
                        color = digitColor,
                        fontSize = su(FACE_SIZE),
                        lineHeight = su(FACE_SIZE),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = su(-FACE_SIZE * 0.02f),
                        textAlign = TextAlign.Center,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both,
                        ),
                    ),
                    // Compose centres the text line box, which leaves digit ink
                    // sitting high. Nudging down puts the glyph across the hinge,
                    // which is the entire point of a split-flap.
                    modifier = Modifier.offset(y = h * 0.263f),
                )
            }
        }
        if (top && corner != null) {
            Text(
                text = corner,
                style = TextStyle(
                    color = digitColor.copy(alpha = 0.85f),
                    fontSize = su(43),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = su(0.8f),
                ),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = du(37), y = du(32)),
            )
        }
    }
}

/**
 * A split-flap card carrying a whole two-character group ("09", "45"), matching
 * the design. The leaf that falls is the OLD top half; the leaf that lands is
 * the NEW bottom half. Everything else is static backing.
 *
 * At one flip per minute this costs almost nothing, which is what makes the
 * whole design viable on an Android Go GPU.
 */
@Composable
fun FlipCard(
    value: String,
    corner: String? = null,
    reducedMotion: Boolean = false,
    digitColor: Color = DC.digit,
    cardColor: Color = DC.cardBody,
) {
    val density = LocalDensity.current
    val w = du(CARD_W)
    val h = du(CARD_H)

    var current by remember { mutableStateOf(value) }
    var previous by remember { mutableStateOf(value) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(value) {
        if (value != current) {
            previous = current
            current = value
            if (reducedMotion) {
                // Crossfade costs no perspective transforms. Default on low-RAM
                // devices, where a rotating layer can drop frames.
                progress.snapTo(0f)
                progress.animateTo(1f, tween(220, easing = LinearEasing))
            } else {
                progress.snapTo(0f)
                progress.animateTo(0.5f, tween(HALF_MS, easing = FALL_EASE))
                progress.animateTo(1f, tween(HALF_MS, easing = LAND_EASE))
            }
        }
    }

    val p = progress.value
    val camera = with(density) { du(1500).toPx() }

    Box(
        Modifier
            .size(w, h)
            .clip(RoundedCornerShape(du(CARD_R)))
            .background(cardColor)
    ) {
        // Backing layers. The bottom one MUST settle on `current`: the crossfade
        // overlay stops drawing at p == 1, and if the backing still said
        // `previous` the old digit was left showing under nothing. That is what
        // made the minutes appear stuck with a stale bottom flap.
        val bottomBacking = when {
            p >= 1f -> current
            reducedMotion -> previous
            p < 0.5f -> previous
            else -> current
        }
        Half(current, top = true, corner = corner, digitColor = digitColor, cardColor = cardColor,
            modifier = Modifier.align(Alignment.TopStart))
        Half(
            bottomBacking,
            top = false, corner = null, digitColor = digitColor, cardColor = cardColor,
            modifier = Modifier.align(Alignment.BottomStart),
        )

        if (p < 1f && reducedMotion) {
            Box(Modifier.align(Alignment.TopStart).graphicsLayer { alpha = 1f - p }) {
                Half(previous, top = true, corner = corner, digitColor = digitColor, cardColor = cardColor)
            }
            Box(Modifier.align(Alignment.BottomStart).graphicsLayer { alpha = p }) {
                Half(current, top = false, corner = null, digitColor = digitColor, cardColor = cardColor)
            }
        } else if (p < 1f) {
            if (p < 0.5f) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .graphicsLayer {
                            rotationX = -180f * p
                            transformOrigin = TransformOrigin(0.5f, 1f)
                            cameraDistance = camera
                        }
                ) {
                    Half(previous, top = true, corner = corner, digitColor = digitColor, cardColor = cardColor)
                    Box(
                        Modifier
                            .size(w, h / 2)
                            .background(Color.Black.copy(alpha = 0.5f * (p * 2f)))
                    )
                }
            } else {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .graphicsLayer {
                            rotationX = 180f * (1f - p)
                            transformOrigin = TransformOrigin(0.5f, 0f)
                            cameraDistance = camera
                        }
                ) {
                    Half(current, top = false, corner = null, digitColor = digitColor, cardColor = cardColor)
                    Box(
                        Modifier
                            .size(w, h / 2)
                            .background(Color.Black.copy(alpha = 0.5f * ((1f - p) * 2f)))
                    )
                }
            }
        }

        // Hinge, drawn last so it sits above every leaf.
        Box(
            Modifier
                .align(Alignment.Center)
                .size(w, du(2).coerceAtLeast(1.dp))
                .background(DC.hinge)
        )
    }
}
