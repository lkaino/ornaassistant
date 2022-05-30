package com.rockethat.ornaassistant.overlays

import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rockethat.ornaassistant.R
import com.rockethat.ornaassistant.viewadapters.AssessAdapter
import com.rockethat.ornaassistant.viewadapters.AssessItem
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

class AssessOverlay(
    mWM: WindowManager,
    mCtx: Context,
    mView: View,
    mWidth: Double
) :
    Overlay(mWM, mCtx, mView, mWidth) {

    var mRv = mView.findViewById<RecyclerView>(R.id.rvAssess)
    var mAssessList = mutableListOf<AssessItem>()

    init {
        mRv.adapter = AssessAdapter(mAssessList, ::hide)
        mRv.layoutManager = LinearLayoutManager(mCtx)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    override fun show() {
        super.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun update(json: JSONObject) {
        val list = mutableListOf<AssessItem>()

        val statsJson = (JSONTokener(json.getString("stats")).nextValue() as JSONObject)
        val statsMap = mutableMapOf<String, List<String>>()
        statsJson.keys().forEach { key ->
            val statBaseJson = (JSONTokener(statsJson.getString(key)).nextValue() as JSONObject)
            val statValuesArray =
                (JSONTokener(statBaseJson.getString("values")).nextValue() as JSONArray)
            statsMap[key.replaceFirstChar(Char::uppercase)] = listOf(
                statValuesArray.get(9).toString(),
                statValuesArray.get(10).toString(),
                statValuesArray.get(11).toString(),
                statValuesArray.get(12).toString(),
            )
        }

        val quality = json.getDouble("quality")

        val headerList = mutableListOf(
            "${(quality * 100).toInt()} %"
        )
        statsMap.keys.forEach { s ->
            headerList.add(s.take(3).replaceFirstChar(Char::uppercase))
        }
        headerList.add("Mats")

        list.add(AssessItem(headerList))

        for (i in 0..3) {
            val itemList = mutableListOf<String>()
            itemList.add(
                when (i) {
                    0 -> "10"
                    1 -> "MF"
                    2 -> "DF"
                    3 -> "GF"
                    else -> ""
                }
            )
            statsMap.forEach { (s, list) ->
                itemList.add(list[i])
            }

            itemList.add(
                when (i) {
                    0 -> "135"
                    1 -> (300 * quality).toInt().toString()
                    2 -> (666 * quality).toInt().toString()
                    3 -> ""
                    else -> ""
                }
            )
            list.add(AssessItem(itemList))
        }

        mRv.post(Runnable {
            mAssessList.clear()
            mAssessList.addAll(list)
            mRv.adapter?.notifyDataSetChanged()
        })
        show()
    }
}