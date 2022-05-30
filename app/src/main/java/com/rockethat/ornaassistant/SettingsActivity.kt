package com.rockethat.ornaassistant

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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