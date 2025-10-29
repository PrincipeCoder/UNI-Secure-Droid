package com.example.unisecuredroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.unisecuredroid.ui.theme.UNISecureDroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UNISecureDroidTheme {
                AppController()
            }
        }
    }
}