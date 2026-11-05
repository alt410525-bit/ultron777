package com.ultron.android.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.ultron.android.theme.UltronTheme
import com.ultron.android.ui.navigation.Nav

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UltronTheme(darkTheme = true) {
                Nav(chatViewModel = chatViewModel)
            }
        }
    }
}
