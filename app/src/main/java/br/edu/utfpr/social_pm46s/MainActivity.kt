package br.edu.utfpr.social_pm46s

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.edu.utfpr.social_pm46s.ui.LoginScreen
import br.edu.utfpr.social_pm46s.ui.theme.Social_pm46sTheme
import cafe.adriel.voyager.navigator.Navigator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Social_pm46sTheme {
                Navigator(LoginScreen)
            }
        }
    }
}