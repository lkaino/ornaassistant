package com.rockethat.ornaassistant

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.rockethat.ornaassistant.db.DungeonVisitDatabaseHelper
import com.rockethat.ornaassistant.overlays.InviterOverlay
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import com.rockethat.ornaassistant.overlays.KGOverlay
import com.rockethat.ornaassistant.overlays.SessionOverlay


class MainState(
    private val mWM: WindowManager,
    private val mCtx: Context,
    mNotificationView: View,
    mSessionView: View,
    mKGView: View
) {
    private val TAG = "OrnaMainState"
    private val mDungeonDbHelper = DungeonVisitDatabaseHelper(mCtx)
    private var mCurrentView: OrnaViewBase? = null
    private var mDungeonVisit: DungeonVisit? = null
    private var mSession: WayvesselSession? = null

    private val mKGOverlay = KGOverlay(mWM, mCtx, mKGView, 0.7)
    private val mInviterOverlay = InviterOverlay(mWM, mCtx, mNotificationView, 0.8)
    private val mSessionOverlay = SessionOverlay(mWM, mCtx, mSessionView, 0.4)

    @RequiresApi(Build.VERSION_CODES.O)
    var mKGLastUpdated: LocalDateTime = LocalDateTime.now()
    var mSleepers = mutableMapOf<String, Sleeper>()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleOrnaData(data: ArrayList<ScreenData>) {
        updateView(data)

        val wayvessel = data.filter { it.name.lowercase().contains("'s wayvessel") }.firstOrNull()
        if (wayvessel != null && (wayvessel.position.left > 70) /* to ignore own wayvessel menu */) {
            if (data.none { it.name.lowercase().contains("this wayvessel is active") }) {
                val name = wayvessel.name.replace("'s Wayvessel", "")
                try {
                    if (mSession == null || mSession!!.name != name) {
                        if (mSession != null) {
                            mSession!!.finish()
                            mSessionOverlay.hide()
                        }
                        mSession = WayvesselSession(name, mCtx)
                        if (mDungeonVisit != null) {
                            mDungeonVisit!!.sessionID = mSession!!.mID
                        }
                        mSessionOverlay.show()
                        mSessionOverlay.update(mSession, mDungeonVisit)
                        Log.i(TAG, "At wayvessel: $name")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception: $e")
                }
            }
        }

        var leaveParty = data.firstOrNull {
            it.name.lowercase().contains("are you sure you would like to leave this party?")
        }
        if (leaveParty == null) {
            leaveParty =
                data.firstOrNull {
                    it.name.lowercase().contains("are you sure you would return home")
                }
        }
        if (leaveParty != null) {
            Log.d(TAG, "Finished session $mSession")
            mSession?.finish()
            mSessionOverlay.hide()
            mSession = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleDiscordData(data: ArrayList<ScreenData>) {
        if (data.size == 1 && data[0].name.contains("Sleepers")) {
            val sd = data[0]
            val split = sd.name.split("\n")
            var date: LocalDateTime? = null
            var bNames = false

            split.forEach {
                if (!bNames) {
                    if (it.startsWith("Current")) {
                        val match =
                            Regex("Current:\\s([0-9]+)-([0-9]+)-([0-9]+)\\s([0-9]+):([0-9]+)").findAll(
                                it
                            ).firstOrNull()
                        if (match != null && match.groups.size == 6) {
                            val year = match.groups[1]?.value?.toInt()
                            val month = match.groups[2]?.value?.toInt()
                            val day = match.groups[3]?.value?.toInt()
                            val hour = match.groups[4]?.value?.toInt()
                            val minute = match.groups[5]?.value?.toInt()
                            if (year != null && month != null && day != null && hour != null && minute != null) {
                                date = LocalDateTime.of(year, month, day, hour, minute)
                            }
                        }
                    } else if (it.contains("Dur Name")) {
                        bNames = true
                    }
                } else if (date != null) {
                    val match =
                        Regex("([*]?)\\s+([0-9]+)h([0-9]+)m\\s(.*)").findAll(
                            it
                        ).firstOrNull()
                    if (match != null && match.groups.size == 5) {
                        val immunity = match.groups[1]?.value
                        val hours = match.groups[2]?.value?.toLong()
                        val minutes = match.groups[3]?.value?.toLong()
                        val name = match.groups[4]?.value
                        if (hours != null && minutes != null && name != null) {
                            val endTime = date!!.plusHours(hours).plusMinutes(minutes)
                            val sleeper =
                                Sleeper(name, immunity != null && immunity == "*", endTime)
                            mSleepers[name] = sleeper
                        }
                    }
                }
            }
        }

        Log.d(TAG, "Sleepers: $mSleepers")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun processData(packageName: String, data: ArrayList<ScreenData>) {
        if (packageName.contains("orna")) {
            handleOrnaData(data)
        } else if (packageName.contains("discord")) {
            handleDiscordData(data)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateView(data: ArrayList<ScreenData>) {
        // pass the data to current view (if there is one)
        val bDone = mCurrentView?.update(data, ::processUpdate)
        if (bDone != null && bDone) {
            mCurrentView!!.close()
            mCurrentView = null
        }

        // check if a new view could be created using this data
        val newType = OrnaViewFactory.getType(data)

        if (mCurrentView == null || (newType != null && newType != mCurrentView!!.type)) {
            val view = OrnaViewFactory.create(newType, data, mWM, mCtx)
            mCurrentView?.update(data, ::processUpdate)

            if (view != null) {
                if (mCurrentView != null) mCurrentView!!.close()
                mCurrentView = view
                mCurrentView!!.drawOverlay()
                Log.d(TAG, "VIEW CHANGED TO ${view.type}")
                if (view.type != OrnaViewType.NOTIFICATIONS) {
                    mInviterOverlay.hide()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun processUpdate(update: MutableMap<OrnaViewUpdateType, Any?>) {
        var done = false
        update.forEach { (type, data) ->
            when (type) {
                OrnaViewUpdateType.DUNGEON_ENTERED -> {
                    if (mDungeonVisit == null) {
                        val view: OrnaViewDungeonEntry = mCurrentView as OrnaViewDungeonEntry
                        var sessionID: Long? = null
                        if (mSession != null) {
                            sessionID = mSession!!.mID
                        }
                        mDungeonVisit = DungeonVisit(sessionID, view.mDungeonName, view.mMode)
                        Log.d(TAG, "Entered: $mDungeonVisit")
                    }

                    mSessionOverlay.update(mSession, mDungeonVisit)
                }
                OrnaViewUpdateType.DUNGEON_MODE_CHANGED -> {
                    if (mDungeonVisit != null) {
                        val view: OrnaViewDungeonEntry = mCurrentView as OrnaViewDungeonEntry
                        mDungeonVisit!!.mode = view.mMode
                    }
                }
                OrnaViewUpdateType.DUNGEON_GODFORGE -> if (mDungeonVisit != null) mDungeonVisit!!.godforges++
                OrnaViewUpdateType.DUNGEON_DONE -> if (mDungeonVisit != null) {
                    if (mSession == null) {
                        mSessionOverlay.hide()
                    }
                    done = true
                }
                OrnaViewUpdateType.DUNGEON_NEW_FLOOR -> if (mDungeonVisit != null) mDungeonVisit!!.floor =
                    (data as Int).toLong()
                OrnaViewUpdateType.DUNGEON_EXPERIENCE -> {
                    if (mDungeonVisit != null) mDungeonVisit!!.experience += data as Int
                    if (mSession != null) mSession!!.experience += data as Int
                    mSessionOverlay.update(mSession, mDungeonVisit)
                }
                OrnaViewUpdateType.DUNGEON_ORNS -> {
                    if (mDungeonVisit != null) mDungeonVisit!!.orns += data as Int
                    if (mSession != null) mSession!!.orns += data as Int
                    mSessionOverlay.update(mSession, mDungeonVisit)
                }
                OrnaViewUpdateType.DUNGEON_GOLD -> {
                    if (mDungeonVisit != null) mDungeonVisit!!.gold += data as Int
                    if (mSession != null) mSession!!.gold += data as Int
                    mSessionOverlay.update(mSession, mDungeonVisit)
                }
                OrnaViewUpdateType.NOTIFICATIONS_INVITERS -> {
                    mInviterOverlay.update(data as MutableMap<String, Rect>)
                }
                OrnaViewUpdateType.KINGDOM_GAUNTLET_LIST -> {
                    val dtNow = LocalDateTime.now()
                    if (dtNow.isAfter(mKGLastUpdated.plusSeconds(2)))
                    {
                        mKGLastUpdated = dtNow
                        Log.i(TAG, "@$data")

                        val sortedList = Kingdom(mCtx).sortKGItems(data as List<KingdomMember>, mSleepers)
                        mKGOverlay.update(sortedList)
                        Kingdom(mCtx).createKGDiscordPost(sortedList)
                    }
                }
            }
        }

        if (done) {
            if (mDungeonVisit != null) {
                mDungeonVisit!!.finish()
                mDungeonDbHelper.insertData(mDungeonVisit!!)
                Log.i(TAG, "Stored: $mDungeonVisit")
            }
            mDungeonVisit = null
        }
    }
}