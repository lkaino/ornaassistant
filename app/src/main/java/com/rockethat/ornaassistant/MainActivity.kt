package com.rockethat.ornaassistant

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.viewpager2.widget.ViewPager2
import com.rockethat.ornaassistant.ui.fragment.FragmentAdapter
import com.google.android.material.tabs.TabLayout
import androidx.compose.ui.platform.ComposeView
import com.rockethat.ornaassistant.ui.fragment.MainFragment
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import android.provider.Settings.SettingNotFoundException
import android.text.TextUtils.SimpleStringSplitter
import android.util.Log

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : AppCompatActivity() {

    private lateinit var tableLayout: TabLayout
    private lateinit var pager: ViewPager2
    private lateinit var adapter: FragmentAdapter
    private val TAG = "OrnaMainActivity"
    private val ACCESSIBILITY_SERVICE_NAME = "Orna Assistant"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize properties
        tableLayout = findViewById(R.id.tab_layout)
        pager = findViewById(R.id.pager)

        adapter = FragmentAdapter(supportFragmentManager, lifecycle)
        pager.adapter = adapter

        tableLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.text) {
                    "Main" -> {
                        pager.currentItem = 0
                        if (adapter.frags.size >= 1) {
                            (adapter.frags[0] as MainFragment).drawWeeklyChart()
                        }
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tableLayout.selectTab(tableLayout.getTabAt(position))
            }
        })

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)

        // Set up Compose in ComposeView
        val composeView = findViewById<ComposeView>(R.id.compose_view)
        composeView.setContent {
            CustomModalDrawer()
        }
    }

    private var sharedPreferenceChangeListener =
        OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "your_key") {
                // Write your code here
            }
        }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.item_preference) {
            goToSettingsActivity()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun goToSettingsActivity() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()

        if (!isAccessibilityEnabled()) {
            // startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        when (tableLayout.selectedTabPosition) {
            0 -> {
                if (adapter.frags.size >= 1) {
                    (adapter.frags[0] as MainFragment).drawWeeklyChart()
                }
            }
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
            var accessibilityEnabled = 0
            val accessibilityFound = false
            try {
                accessibilityEnabled =
                    Settings.Secure.getInt(this.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
                Log.d(TAG, "ACCESSIBILITY: $accessibilityEnabled")
            } catch (e: SettingNotFoundException) {
                Log.d(TAG, "Error finding setting, default accessibility to not found: " + e.message)
            }
            val mStringColonSplitter = SimpleStringSplitter(':')
            if (accessibilityEnabled == 1) {
                Log.d(TAG, "***ACCESSIBILIY IS ENABLED***: ")
                val settingValue: String = Settings.Secure.getString(
                    contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
                Log.d(TAG, "Setting: $settingValue")
                mStringColonSplitter.setString(settingValue)
                while (mStringColonSplitter.hasNext()) {
                    val accessabilityService = mStringColonSplitter.next()
                    Log.d(TAG, "Setting: $accessabilityService")
                    if (accessabilityService.contains(
                            packageName,
                            ignoreCase = true
                        )
                    ) {
                        Log.d(
                            TAG,
                            "We've found the correct setting - accessibility is switched on!"
                        )
                        return true
                    }
                }
                Log.d(TAG, "***END***")
            } else {
                Log.d(TAG, "***ACCESSIBILIY IS DISABLED***")
            }
            return accessibilityFound
        }
    }