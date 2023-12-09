package com.rockethat.ornaassistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.rockethat.ornaassistant.ui.fragment.FragmentAdapter
import com.rockethat.ornaassistant.ui.fragment.MainFragment

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : AppCompatActivity() {
    private lateinit var tableLayout: TabLayout
    private lateinit var pager: ViewPager2
    private lateinit var adapter: FragmentAdapter
    private val NOTIFICATION_ID = 1234
    private val CHANNEL_ID = "persistent_notification_channel"

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val enabled = intent?.getBooleanExtra("enabled", false) ?: false
            handlePersistentNotificationPreference(enabled)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupTabLayout()
        setupComposeView()
        createNotificationChannel()

        // Register the broadcast receiver
        val filter = IntentFilter("com.rockethat.ornaassistant.UPDATE_NOTIFICATION")
        registerReceiver(notificationReceiver, filter)
    }

    private fun initializeViews() {
        tableLayout = findViewById(R.id.tab_layout)
        pager = findViewById(R.id.pager)
        adapter = FragmentAdapter(supportFragmentManager, lifecycle)
        pager.adapter = adapter
    }

    private fun setupTabLayout() {
        tableLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                pager.currentItem = when (tab.text) {
                    "Main" -> 0
                    // Add cases for other tabs if needed
                    else -> 0
                }
                updateMainFragment()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tableLayout.selectTab(tableLayout.getTabAt(position))
                updateMainFragment()
            }
        })
    }

    private fun setupComposeView() {
        val composeView = findViewById<ComposeView>(R.id.compose_view)
        composeView.setContent {
            CustomModalDrawer(this@MainActivity)
        }
    }

    private fun updateMainFragment() {
        if (pager.currentItem == 0 && adapter.frags.size >= 1) {
            (adapter.frags[0] as MainFragment).drawWeeklyChart()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_preference -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Persistent Notification"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, channelName, importance).apply {
                description = "Channel for persistent notification"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun handlePersistentNotificationPreference(enabled: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (enabled) {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("App is Running")
                .setContentText("Tap to open.")
                .setSmallIcon(R.drawable.ric_notification)
                .setOngoing(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } else {
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the broadcast receiver
        unregisterReceiver(notificationReceiver)
    }
}