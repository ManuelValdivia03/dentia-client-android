package com.dentia.patient

import android.content.Context
import android.os.LocaleList
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dentia.patient.ui.DentiaApp
import com.dentia.patient.ui.theme.DentiaTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val spanishMexico = Locale.Builder()
            .setLanguage("es")
            .setRegion("MX")
            .build()
        Locale.setDefault(spanishMexico)
        val configuration = newBase.resources.configuration.apply {
            setLocale(spanishMexico)
            setLocales(LocaleList(spanishMexico))
        }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DentiaTheme {
                DentiaApp()
            }
        }
    }
}
