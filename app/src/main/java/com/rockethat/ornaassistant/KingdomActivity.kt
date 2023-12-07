package com.rockethat.ornaassistant

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rockethat.ornaassistant.db.KingdomMemberDatabaseHelper
import com.rockethat.ornaassistant.db.KingdomGauntletDatabaseHelper
import com.rockethat.ornaassistant.ui.viewadapters.KingdomSeenAdapter
import com.rockethat.ornaassistant.ui.viewadapters.KingdomSeenItem
import org.json.JSONTokener
import org.json.JSONObject
import java.time.LocalDateTime

@RequiresApi(Build.VERSION_CODES.O)
class KingdomActivity : AppCompatActivity() {
    private var mKGSeenList = mutableListOf<KingdomSeenItem>()
    private lateinit var mRv: RecyclerView
    private lateinit var mKMDb: KingdomMemberDatabaseHelper
    private lateinit var mKGDb: KingdomGauntletDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kingdom)

        mKMDb = KingdomMemberDatabaseHelper(this)
        mKGDb = KingdomGauntletDatabaseHelper(this)

        val importButton: Button = findViewById(R.id.btnKingdomImport)
        val exportButton: Button = findViewById(R.id.btnKingdomExport)
        val lblStatus: TextView = findViewById(R.id.lblDiscordResult)
        mRv = findViewById(R.id.rvKingdomData)

        mRv.adapter = KingdomSeenAdapter(mKGSeenList)
        mRv.layoutManager = LinearLayoutManager(this)

        updateStatusText()
        lblStatus.text = ""

        exportButton.setOnClickListener {
            handleExport(lblStatus)
        }

        importButton.setOnClickListener {
            handleImport(lblStatus)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateStatusText() {
        val lblStatus: TextView = findViewById(R.id.lblDiscordStatus)
        val items = mKMDb.allData
        lblStatus.text = "${items.size} mappings currently stored."
        mKMDb.close()
        updateSeenList()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateSeenList() {
        mKGSeenList.clear()
        val now = LocalDateTime.now()
        val entries = mKGDb.getEntriesBetween(now.minusMonths(1), now)

        val entryMap = mutableMapOf<String, Int>()
        entries.forEach {
            entryMap[it.name] = entryMap.getOrDefault(it.name, 0) + 1
        }

        mKGSeenList.addAll(entryMap.map { KingdomSeenItem(it.key, it.value.toString()) })
        mKGSeenList.sortByDescending { it.seenCount.toInt() }
        mRv.adapter?.notifyDataSetChanged()
    }

    private fun handleExport(lblStatus: TextView) {
        val text = StringBuilder("{")
        val items = mKMDb.allData
        val rows = items.joinToString(",\n") { "\"${it.character}\": \"${it.discordName}\"" }
        text.append(rows).append("}")

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("label", text.toString())
        clipboard.setPrimaryClip(clip)

        lblStatus.text = "${items.size} mappings copied."
    }

    private fun handleImport(lblStatus: TextView) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        try {
            var numOfEntriesUpdated = 0
            val json = JSONTokener(clipboard.primaryClip?.getItemAt(0)?.text.toString()).nextValue() as JSONObject
            json.keys().forEach { ign ->
                val entry = KingdomMember(ign, mutableMapOf())
                entry.discordName = json.getString(ign)
                val existing = mKMDb.getEntry(entry.character)
                if (existing != null) {
                    if (mKMDb.updateData(entry)) {
                        numOfEntriesUpdated++
                    }
                } else {
                    mKMDb.insertData(entry)
                    numOfEntriesUpdated++
                }
            }
            lblStatus.text = "$numOfEntriesUpdated mappings updated."
        } catch (e: Exception) {
            lblStatus.text = "Failed to read data: ${e.message}"
        }
        mKMDb.close()
    }
}
