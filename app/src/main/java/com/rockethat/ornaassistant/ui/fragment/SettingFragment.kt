package com.rockethat.ornaassistant.ui.fragment

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.rockethat.ornaassistant.R

class SettingFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preference, rootKey)

        // Setup preference for enabling Accessibility Service
        findPreference<Preference>("enable_accessibility_service")?.setOnPreferenceClickListener {
            showAccessibilityExplanationDialog()
            true
        }

        // Setup preference for enabling Notification Service
        findPreference<Preference>("enable_notifications")?.setOnPreferenceClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                showNotificationPermissionDialog()
            } else {
                // For older versions, direct handling is not required as notification permission is granted by default
            }
            true
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

    private fun showAccessibilityExplanationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Accessibility Permission Needed")
            .setMessage("This permission is needed for additional features. Please enable the Accessibility Service in Settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNotificationPermissionDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Notification Permission Needed")
            .setMessage("This app requires notification permission to send you updates. Please enable notifications for this app in Settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent().apply {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
