package com.rockethat.ornaassistant

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.rockethat.ornaassistant.ui.fragment.FragmentAdapter
import com.rockethat.ornaassistant.ui.fragment.MainFragment


@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : AppCompatActivity() {
    private lateinit var tableLayout: TabLayout
    private lateinit var pager: ViewPager2
    private lateinit var adapter: FragmentAdapter
    private val TAG = "OrnaMainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupTabLayout()
        setupComposeView()
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
        if (item.itemId == R.id.item_preference) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }


}