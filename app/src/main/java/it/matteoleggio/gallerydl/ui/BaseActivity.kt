package it.matteoleggio.gallerydl.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import it.matteoleggio.gallerydl.util.ThemeUtil

open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtil.updateTheme(this)
        super.onCreate(savedInstanceState)
    }
}