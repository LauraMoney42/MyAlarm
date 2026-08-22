package com.kindcode.alarmhub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import java.time.LocalDateTime
import java.util.Locale

/**
 * Takes pre-derived strings rather than a LocalDateTime. The clock ticks every
 * second but only changes every minute, and passing `now` straight in made the
 * two large cards re-execute 60 times more often than they had any reason to.
 * With stable String parameters they skip instead.
 */
@Composable
fun ClockPane(
    hh: String,
    mm: String,
    meridiem: String?,
    dateLabel: String,
    reducedMotion: Boolean,
    accent: Color,
    nextAlarmLabel: String,
    soundStatus: String,
    soundPlaying: Boolean,
    onPreviewWake: () -> Unit,
    onOpenSettings: () -> Unit,
) {

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(du(20)),
            ) {
                FlipCard(hh, corner = meridiem, reducedMotion = reducedMotion)
                FlipCard(mm, corner = null, reducedMotion = reducedMotion)
            }

            Spacer(Modifier.height(du(20)))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(du(22)),
            ) {
                StatusText(dateLabel)
                Dot()
                StatusText(nextAlarmLabel)
                Dot()
                StatusText(soundStatus, if (soundPlaying) accent else DC.ink(0.34f))
                Dot()
                StatusText(
                    "PREVIEW WAKE",
                    DC.ink(0.5f),
                    Modifier
                        .clip(RoundedCornerShape(du(6)))
                        .clickable(onClick = onPreviewWake),
                )
            }
        }

        // Deliberately small and dim. It is the only thing on the clock face
        // that is not the time, and it should never compete with it.
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = du(20), bottom = du(12))
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onOpenSettings)
                .padding(du(12)),
            contentAlignment = Alignment.Center,
        ) {
            Text("\u2699", style = TextStyle(color = DC.ink(0.26f), fontSize = su(22)))
        }
    }
}

@Composable
private fun StatusText(
    text: String,
    color: Color = DC.ink(0.34f),
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = su(15),
            fontWeight = FontWeight.Normal,
            letterSpacing = su(15 * 0.22f),
        ),
    )
}

@Composable
private fun Dot() {
    Box(
        Modifier
            .size(du(4))
            .clip(RoundedCornerShape(50))
            .background(DC.ink(0.24f))
    )
}

private val DAYS = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
private val MONTHS = listOf(
    "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
    "JUL", "AUG", "SEP", "OCT", "NOV", "DEC",
)

fun dateLabel(now: LocalDateTime): String =
    "${DAYS[now.dayOfWeek.value - 1]} ${MONTHS[now.monthValue - 1]} ${now.dayOfMonth}"
