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

/**
 * The design's weather layout, standing by for a data source.
 *
 * MVP1 ships with no INTERNET permission at all, so there is deliberately
 * nothing to display yet. Showing the design's sample temperatures would put
 * invented numbers on a bedside clock, which is worse than an empty panel.
 */
@Composable
fun WeatherPanel(accent: Color, onClose: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF101318),
                    0.46f to Color(0xFF14181F),
                    1f to Color(0xFF0E1116),
                )
            )
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = du(64), vertical = du(52)),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(du(14))) {
                    Text(
                        "Weather",
                        style = TextStyle(
                            color = DC.ink,
                            fontSize = su(62),
                            fontWeight = FontWeight.Light,
                            letterSpacing = su(-62 * 0.02f),
                        ),
                    )
                    Text(
                        "Not connected yet",
                        style = TextStyle(color = DC.ink(0.5f), fontSize = su(24), fontWeight = FontWeight.Light),
                    )
                }
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
                SectionLabel("WHAT ARRIVES IN MVP2", 13, 0.45f)
                Text(
                    "Current conditions, an hourly strip and a three day outlook, " +
                        "pulled from Open-Meteo.",
                    style = TextStyle(color = DC.ink(0.62f), fontSize = su(19), fontWeight = FontWeight.Light),
                )
                Text(
                    "No API key, no account, and coordinates stored as a fixed setting, " +
                        "so the app never asks for a location permission.",
                    style = TextStyle(color = DC.ink(0.42f), fontSize = su(17), fontWeight = FontWeight.Light),
                )
            }

            Spacer(Modifier.weight(1f))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "OFFLINE BY DESIGN · NO INTERNET PERMISSION IN THIS BUILD",
                    style = TextStyle(
                        color = accent.copy(alpha = 0.7f),
                        fontSize = su(13),
                        letterSpacing = su(13 * 0.2f),
                    ),
                )
            }
        }
        CloseGrip(vertical = true, modifier = Modifier.align(Alignment.CenterEnd)) { onClose() }

    }
}
