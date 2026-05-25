package com.swiftpdf.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.swiftpdf.app.navigation.SwiftPdfApp
import com.swiftpdf.app.ui.theme.SwiftPdfTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SwiftPdfTheme {
                SwiftPdfApp()
            }
        }
    }
}
