package com.corvix.player

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.corvix.player.model.AppScreen
import com.corvix.player.ui.CorvixRootApp
import com.corvix.player.viewmodel.CorvixViewModel
import com.corvix.player.widget.CorvixWidgetBridge

class MainActivity : ComponentActivity() {

    private val viewModel: CorvixViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        handleWidgetTarget(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            CorvixRootApp(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleWidgetTarget(intent)
    }

    private fun handleWidgetTarget(intent: Intent?) {
        when (intent?.getStringExtra(CorvixWidgetBridge.EXTRA_WIDGET_TARGET)) {
            "eq" -> viewModel.openSettingsEq()
            "playlists" -> viewModel.navigate(AppScreen.PLAYLISTS)
            "now_playing" -> viewModel.navigate(AppScreen.NOW_PLAYING)
        }
    }
}
