package com.rockethat.ornaassistant.ui.fragment

import android.os.Bundle
import com.rockethat.ornaassistant.R
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import android.content.SharedPreferences

class SettingFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preference, rootKey)

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val editor: SharedPreferences.Editor = prefs.edit()
        editor.putBoolean("PREF_NAME", false)
        editor.apply() // Use apply() for asynchronous saving
    }
}