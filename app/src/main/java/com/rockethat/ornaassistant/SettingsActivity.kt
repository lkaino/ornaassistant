package com.rockethat.ornaassistant

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rockethat.ornaassistant.ui.fragment.SettingFragment

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.content_preference, SettingFragment())
            .commit()
        setContentView(R.layout.activity_settings)
    }
}