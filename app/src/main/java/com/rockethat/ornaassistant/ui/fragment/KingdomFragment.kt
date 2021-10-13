package com.rockethat.ornaassistant.ui.fragment

import KingdomMemberDatabaseHelper
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.rockethat.ornaassistant.KingdomMember
import com.rockethat.ornaassistant.R
import org.json.JSONTokener
import org.json.JSONObject

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_kingdom, container, false)
        val import: Button = view.findViewById(R.id.btnKingdomImport)
        val export: Button = view.findViewById(R.id.btnKingdomExport)

        updateStatusText(view)
        val lblStatus: TextView = view.findViewById(R.id.lblDiscordResult)
        lblStatus.text = ""

        export.setOnClickListener {
            val kgDB = this.context?.let { it1 -> KingdomMemberDatabaseHelper(it1) }

            if (kgDB != null) {
                var text = "{"
                val items = kgDB.allData
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

                val lblResult: TextView = view.findViewById(R.id.lblDiscordResult)
                lblResult.text = "${items.size} mappings copied."

                kgDB.close()
            }
        }

        import.setOnClickListener {
            val kgDB = this.context?.let { it1 -> KingdomMemberDatabaseHelper(it1) }
            val lblResult: TextView = view.findViewById(R.id.lblDiscordResult)

            if (kgDB != null) {
                val clipboard: ClipboardManager? =
                    context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                try {
                    var numOfEntriesUpdated = 0
                    val json = JSONTokener(clipboard?.primaryClip?.getItemAt(0)?.text.toString()).nextValue() as JSONObject
                    json.keys().forEach { ign ->
                        val entry = KingdomMember(ign, mutableMapOf())
                        entry.discordName = json.getString(ign)
                        val existing = kgDB.getEntry(entry.character)
                        if (existing != null)
                        {
                            if (kgDB.updateData(entry))
                            {
                                numOfEntriesUpdated++
                            }
                        }
                        else
                        {
                            kgDB.insertData(entry)
                            numOfEntriesUpdated++
                        }
                    }
                    lblResult.text = "$numOfEntriesUpdated mappings updated."
                } catch (e: Exception) {
                    lblResult.text = "Failed to read data: ${e.message}"
                }
                kgDB.close()
            }
        }
        // Inflate the layout for this fragment
        return view
    }

    private fun updateStatusText(view: View) {
        val lblStatus: TextView = view.findViewById(R.id.lblDiscordStatus)

        val kgDB = this.context?.let { it1 -> KingdomMemberDatabaseHelper(it1) }
        if (kgDB != null)
        {
            val items = kgDB.allData
            lblStatus.text = "${items.size} mappings currently stored."
            kgDB.close()
        }
        else
        {
            lblStatus.text = "0 mappings currently stored."
        }
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