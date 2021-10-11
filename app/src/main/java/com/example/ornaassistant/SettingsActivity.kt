package com.example.ornaassistant

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        if (savedInstanceState == null)
        {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.content_preference, MainPreference())
                .commit()
        }
        else
        {
            title = savedInstanceState.getCharSequence(TAG_TITLE)
        }

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0)
            {
                setTitle(R.string.settings)
            }
        }

        setUpToolBar()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putCharSequence(TAG_TITLE, title)
    }

    private fun setUpToolBar() {
        supportActionBar?.setTitle(R.string.settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

    }

    class MainPreference : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.main_preference, rootKey)
        }

    }
    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat?,
        pref: Preference?
    ): Boolean {
        // Init the new fragment
        val args = pref?.extras
        val fragment = pref?.fragment?.let {
            supportFragmentManager.fragmentFactory.instantiate(
                classLoader,
                it
            )
        }?.apply {
            arguments = args
            setTargetFragment(caller, 0)
        }

        fragment?.let {
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_preference, it)
                .addToBackStack(null)
                .commit()
        }

        title = pref?.title
        return true
    }

    companion object{
        private val TAG_TITLE = "SETTINGS_ACTIVITY"
    }
}