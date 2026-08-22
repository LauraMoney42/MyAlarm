package com.kindcode.alarmhub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import java.time.LocalDateTime

/**
 * Standing by for a data source, mirroring the weather panel on the other side.
 *
 * The plan is CalDAV through DAVx5, read out of Android's own CalendarContract,
 * which keeps a Google account out of the picture entirely. Until that exists
 * this shows the date and says so, rather than inventing sample appointments on
 * a bedside clock.
 */
@Composable
fun CalendarPanel(now: LocalDateTime, accent: Color, onClose: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF13111A),
                    0.46f to Color(0xFF17151F),
                    1f to Color(0xFF100E16),
                )
            )
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = du(64), vertical = du(52))) {

            Column(verticalArrangement = Arrangement.spacedBy(du(14))) {
                Text(
                    now.dayOfWeek.getDisplayName(
                        java.time.format.TextStyle.FULL,
                        java.util.Locale.getDefault(),
                    ),
                    style = TextStyle(
                        color = DC.ink,
                        fontSize = su(62),
                        fontWeight = FontWeight.Light,
                        letterSpacing = su(-62 * 0.02f),
                    ),
                )
                Text(
                    "${now.month.getDisplayName(
                        java.time.format.TextStyle.FULL,
                        java.util.Locale.getDefault(),
                    )} ${now.dayOfMonth}, ${now.year}",
                    style = TextStyle(
                        color = DC.ink(0.5f),
                        fontSize = su(24),
                        fontWeight = FontWeight.Light,
                    ),
                )
            }

            Spacer(Modifier.height(du(40)))

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(du(22)))
                    .background(DC.ink(0.045f))
                    .padding(horizontal = du(34), vertical = du(34)),
                verticalArrangement = Arrangement.spacedBy(du(18)),
            ) {
                SectionLabel("WHAT ARRIVES LATER", 13, 0.45f)
                Text(
                    "Today's agenda and the next few days, read from the tablet's " +
                        "own calendar provider.",
                    style = TextStyle(
                        color = DC.ink(0.62f),
                        fontSize = su(19),
                        fontWeight = FontWeight.Light,
                    ),
                )
                Text(
                    "Sync arrives through DAVx5 over CalDAV, so events land in " +
                        "CalendarContract and this app just reads them. No Google " +
                        "account anywhere in the chain.",
                    style = TextStyle(
                        color = DC.ink(0.42f),
                        fontSize = su(17),
                        fontWeight = FontWeight.Light,
                    ),
                )
            }

            Spacer(Modifier.weight(1f))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "NEEDS READ_CALENDAR · NOT REQUESTED IN THIS BUILD",
                    style = TextStyle(
                        color = accent.copy(alpha = 0.7f),
                        fontSize = su(13),
                        letterSpacing = su(13 * 0.2f),
                    ),
                )
            }
        }
        CloseGrip(vertical = true, modifier = Modifier.align(Alignment.CenterStart)) { onClose() }

    }
}
