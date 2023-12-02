package com.rockethat.ornaassistant.ui.fragment

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.rockethat.ornaassistant.R

class SettingFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preference, rootKey)

        // Set a default value for a specific preference
        context?.let { nonNullContext ->
            val prefs = PreferenceManager.getDefaultSharedPreferences(nonNullContext)
            val editor: SharedPreferences.Editor = prefs.edit()
            editor.putBoolean("PREF_NAME", false)
            editor.commit()
        }

        // Listen for changes in the theme preference
        findPreference<ListPreference>("theme_preference")?.setOnPreferenceChangeListener { _, newValue ->
            applyTheme(newValue as String)
            true
        }
    }
    private fun applyTheme(themeValue: String) {
        when (themeValue) {
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "device" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}
