package com.corvix.player.widget

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Lightweight configuration activity opened when tapping the Winamp lightning bolt (⚡)
 * to grant Notification Access for live cross-app MediaSession sync.
 */
class WinampWidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val lifecycleOwner = LocalLifecycleOwner.current
            var granted by remember {
                mutableStateOf(WinampMediaListenerService.isNotificationAccessGranted(this))
            }

            DisposableEffect(lifecycleOwner) {
                val obs = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        granted = WinampMediaListenerService.isNotificationAccessGranted(this@WinampWidgetConfigActivity)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(obs)
                onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF12121A))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF282838), RoundedCornerShape(4.dp))
                        .border(1.5.dp, Color(0xFF4A4A60), RoundedCornerShape(4.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "⚡ WINAMP 2.x WIDGET",
                        color = Color(0xFFD4AF37),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (granted) {
                            "Notification Access is GRANTED. Live metadata, elapsed timer, and controls work with Corvix, Spotify, YouTube Music, and all system players."
                        } else {
                            "Grant Notification Access to sync live track titles, elapsed time, and transport controls across Spotify, YouTube Music, and other players."
                        },
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF050505), RoundedCornerShape(3.dp))
                                .border(1.dp, Color(0xFF4A4A60), RoundedCornerShape(3.dp))
                                .clickable {
                                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (granted) "ACCESS SETTINGS" else "GRANT ACCESS",
                                color = Color(0xFF00E600),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(0.7f)
                                .background(Color(0xFFC5CBD3), RoundedCornerShape(3.dp))
                                .border(1.dp, Color.White, RoundedCornerShape(3.dp))
                                .clickable { finish() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "DONE",
                                color = Color(0xFF222222),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}
