package com.rockethat.ornaassistant.overlays

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rockethat.ornaassistant.KingdomMember
import com.rockethat.ornaassistant.R
import com.rockethat.ornaassistant.db.DungeonVisitDatabaseHelper
import com.rockethat.ornaassistant.db.WayvesselSessionDatabaseHelper
import com.rockethat.ornaassistant.viewadapters.KGAdapter
import com.rockethat.ornaassistant.viewadapters.KGItem
import com.rockethat.ornaassistant.viewadapters.NotificationsAdapter
import com.rockethat.ornaassistant.viewadapters.NotificationsItem
import java.time.Duration
import java.time.LocalDateTime

class InviterOverlay(
    mWM: WindowManager,
    mCtx: Context,
    mView: View,
    mWidth: Double
) :
    Overlay(mWM, mCtx, mView, mWidth) {

    var mRv = mView.findViewById<RecyclerView>(R.id.rvList)
    var mList = mutableListOf<NotificationsItem>()

    init {
        mRv.adapter = NotificationsAdapter(mList, ::hide)
        mRv.layoutManager = LinearLayoutManager(mCtx)
    }

    override fun show() {
        super.show()
        mRv.setOnClickListener {
            hide()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun update(data: MutableMap<String, Rect>) {
        val wvDB = WayvesselSessionDatabaseHelper(mCtx)
        mList.clear()

        val lastSession = wvDB.getLastNSessions(1).firstOrNull()
        var dur = Duration.ofDays(1)
        if (lastSession != null) {
            dur = Duration.between(lastSession.mStarted, LocalDateTime.now())
        }

        var cooldownText = "CD"
        if (dur.seconds in 1..3599) {
            val left = 3600 - dur.seconds
            val minutes: Long = left / 60
            cooldownText = "$minutes min"
        }
        mList.add(
            NotificationsItem(
                "Inviter",
                "N",
                "VoG",
                "D",
                "BG",
                "UW",
                "CG",
                cooldownText, cooldownText != "CD"
            )
        )

        val dungeonDbHelper = DungeonVisitDatabaseHelper(mCtx)
        data.forEach { (inviter, rect) ->
            val sessions = wvDB.getLastNSessionsFor(inviter, 1)
            var cdEnd = LocalDateTime.now().minusYears(1)
            var normal = 0
            var vog = 0
            var bg = 0
            var d = 0
            var uw = 0
            var cg = 0
            sessions.forEach { s ->
                val visits = dungeonDbHelper.getVisitsForSession(s.mID)
                visits.forEach { v ->
                    val cdEndThis = v.coolDownEnds()
                    if (cdEndThis > cdEnd) {
                        cdEnd = cdEndThis
                    }

                    with(v.name.lowercase())
                    {
                        when {
                            endsWith("dungeon") -> normal++
                            contains("valley") -> vog++
                            contains("battle") -> bg++
                            contains("dragon") -> d++
                            contains("underworld") -> uw++
                            contains("chaos") -> cg++
                        }
                    }
                }
            }
            dur = Duration.between(LocalDateTime.now(), cdEnd)

            val hours = dur.toMinutes().toInt() / 60
            val minutes = kotlin.math.abs(dur.toMinutes().toInt() % 60)
            cooldownText = if (kotlin.math.abs(hours) < 24) "$hours:${
                minutes.toString().padStart(2, '0')
            }" else ""
            mList.add(
                NotificationsItem(
                    inviter,
                    if (normal > 0) normal.toString() else "",
                    if (vog > 0) vog.toString() else "",
                    if (d > 0) d.toString() else "",
                    if (bg > 0) bg.toString() else "",
                    if (uw > 0) uw.toString() else "",
                    if (cg > 0) cg.toString() else "",
                    cooldownText, !dur.isNegative
                )
            )
            dungeonDbHelper.close()
        }

        wvDB.close()
        mRv.adapter?.notifyDataSetChanged()
        show()
    }
}