package com.cherryblossomdev.greywire

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.cherryblossomdev.greywire.navigation.GreywireNavGraph
import com.cherryblossomdev.greywire.ui.theme.GreywireTheme

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dark app: light status/nav bar icons on a transparent bar
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            GreywireTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        // Test tags surface as resource-ids for Appium (see ../Android)
                        .semantics { testTagsAsResourceId = true },
                    color = MaterialTheme.colorScheme.background
                ) {
                    GreywireNavGraph()
                }
            }
        }
    }
}
