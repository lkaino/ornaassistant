package com.rockethat.ornaassistant.ui.fragment

import KingdomMemberDatabaseHelper
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rockethat.ornaassistant.KingdomMember
import com.rockethat.ornaassistant.R
import com.rockethat.ornaassistant.db.KingdomGauntletDatabaseHelper
import com.rockethat.ornaassistant.ui.viewadapters.KingdomSeenAdapter
import com.rockethat.ornaassistant.ui.viewadapters.KingdomSeenItem
import org.json.JSONObject
import org.json.JSONTokener
import java.time.LocalDateTime

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [KingdomFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class KingdomFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var mKGSeenList = mutableListOf<KingdomSeenItem>()
    private var mRv: RecyclerView? = null
    private lateinit var mKMDb: KingdomMemberDatabaseHelper
    private lateinit var mKGDb: KingdomGauntletDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        mKMDb = KingdomMemberDatabaseHelper(context as android.content.Context)
        mKGDb = KingdomGauntletDatabaseHelper(context as android.content.Context)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_kingdom, container, false)
        val import: Button = view.findViewById(R.id.btnKingdomImport)
        val export: Button = view.findViewById(R.id.btnKingdomExport)

        mRv = view.findViewById<RecyclerView>(R.id.rvKingdomData)
        if (mRv != null) {
            mRv!!.adapter = KingdomSeenAdapter(mKGSeenList)
            mRv!!.layoutManager = LinearLayoutManager(context)
        }

        updateStatusText(view)
        val lblStatus: TextView = view.findViewById(R.id.lblDiscordResult)
        lblStatus.text = ""

        export.setOnClickListener {
            var text = "{"
            val items = mKMDb.allData
            val rows = mutableListOf<String>()
            items.forEach {
                rows.add("\"${it.character}\": \"${it.discordName}\"")
            }
            text += rows.joinToString(",\n")
            text += "}"

            val clipboard: ClipboardManager? =
                context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
            val clip = ClipData.newPlainText("label", text)
            clipboard?.setPrimaryClip(clip)

            lblStatus.text = "${items.size} mappings copied."
        }

        import.setOnClickListener {

            val clipboard: ClipboardManager? =
                context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
            try {
                var numOfEntriesUpdated = 0
                val json =
                    JSONTokener(clipboard?.primaryClip?.getItemAt(0)?.text.toString()).nextValue() as JSONObject
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

        // Inflate the layout for this fragment
        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateStatusText(view: View) {
        val lblStatus: TextView = view.findViewById(R.id.lblDiscordStatus)

        val items = mKMDb.allData
        lblStatus.text = "${items.size} mappings currently stored."
        mKMDb.close()

        updateSeenList()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateSeenList() {
        mKGSeenList.clear()
        val now = LocalDateTime.now()
        val entries = mKGDb?.getEntriesBetween(now.minusMonths(1), now)

        val entryMap = mutableMapOf<String, Int>()
        entries.forEach { it ->
            if (entryMap.containsKey(it.name)) {
                val value = entryMap[it.name]
                if (value != null) {
                    entryMap[it.name] = value.plus(1)
                }
            } else {
                entryMap[it.name] = 1
            }
        }

        entryMap.forEach { (s, i) ->
            mKGSeenList.add(KingdomSeenItem(s, i.toString()))
        }

        mKGSeenList.sortByDescending { it.seenCount.toInt() }

        mRv?.adapter?.notifyDataSetChanged()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment KingdomFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            KingdomFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}