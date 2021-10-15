package com.rockethat.ornaassistant.overlays

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rockethat.ornaassistant.KingdomGauntletFloor
import com.rockethat.ornaassistant.KingdomMember
import com.rockethat.ornaassistant.R
import com.rockethat.ornaassistant.viewadapters.KGAdapter
import com.rockethat.ornaassistant.viewadapters.KGItem

class KGOverlay(
    mWM: WindowManager,
    mCtx: Context,
    mView: View,
    mWidth: Double
) :
    Overlay(mWM, mCtx, mView, mWidth) {

    var mRv = mView.findViewById<RecyclerView>(R.id.rvKG)
    var mKGList = mutableListOf<KGItem>()

    init {
        mRv.adapter = KGAdapter(mKGList, ::hide)
        mRv.layoutManager = LinearLayoutManager(mCtx)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    override fun show() {
        super.show()
        mRv.setOnClickListener {
            hide()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun update(data: List<KingdomMember>) {
        mKGList.clear()

        mKGList.add(
            KGItem(
                "Player",
                "Floors",
                "Im",
                "Sleep",
                false,
                "Seen"
            )
        )

        data.forEach { it ->
            var sleeptime = ""
            if (it.endTimeLeftSeconds > 0) {
                val sleepHours = it.endTimeLeftSeconds / 3600
                val sleepMinutes = (it.endTimeLeftSeconds - sleepHours * 3600) / 60
                sleeptime = "${sleepHours}h ${sleepMinutes}m"
            }

            var floorTexts = mutableListOf<String>()
            val floors =
                it.floors.filterValues { f -> !f.loss }.filterValues { f -> !f.win }.forEach { f ->
                    floorTexts.add(f.value.number.toString())
                }

            if (floorTexts.isNotEmpty())
            {
                mKGList.add(
                    KGItem(
                        it.character,
                        floorTexts.joinToString(", "),
                        if (it.immunity) "⭐" else "",
                        sleeptime,
                        it.zerk,
                        it.seenCount.toString()
                    )
                )
            }
        }

        mRv.post(Runnable {
            mRv.adapter?.notifyDataSetChanged()
        })
        show()
    }
}