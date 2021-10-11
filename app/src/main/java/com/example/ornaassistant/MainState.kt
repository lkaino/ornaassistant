package com.example.ornaassistant

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ornaassistant.db.DungeonVisitDatabaseHelper
import com.example.ornaassistant.db.WayvesselSessionDatabaseHelper
import com.example.ornaassistant.viewadapters.KGAdapter
import com.example.ornaassistant.viewadapters.KGItem
import com.example.ornaassistant.viewadapters.NotificationsAdapter
import com.example.ornaassistant.viewadapters.NotificationsItem
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

class MainState(
    private val mWM: WindowManager,
    private val mCtx: Context,
    private val mNotificationView: View,
    private val mSessionView: View,
    private val mKGView: View
) {
    private var dbHelper = DungeonVisitDatabaseHelper(mCtx)
    private val TAG = "OrnaMainState"
    var mCurrentView: OrnaViewBase? = null
    var mDungeonVisit: DungeonVisit? = null
    var mSession: WayvesselSession? = null
    var mNotificationRv = mNotificationView.findViewById<RecyclerView>(R.id.rvList)
    var mNotificationList = mutableListOf<NotificationsItem>()
    var mKGRv = mKGView.findViewById<RecyclerView>(R.id.rvKG)
    var mKGList = mutableListOf<KGItem>()

    var mSessionGoldTv = mSessionView.findViewById<TextView>(R.id.tvGoldSession)
    var mSessionOrnTv = mSessionView.findViewById<TextView>(R.id.tvOrnsSession)
    var mDungeonGoldTv = mSessionView.findViewById<TextView>(R.id.tvGoldDungeon)
    var mDungeonOrnTv = mSessionView.findViewById<TextView>(R.id.tvOrnsDungeon)
    val windowManager by lazy {
        mCtx.getSystemService(Context.WINDOW_SERVICE)
                as WindowManager
    }
    var mSleepers = mutableMapOf<String, Sleeper>()

    init {
        mNotificationRv.adapter = NotificationsAdapter(mNotificationList, ::hideInviterOverlay)
        mNotificationRv.layoutManager = LinearLayoutManager(mCtx)

        mKGRv.adapter = KGAdapter(mKGList, ::hideKGOverlay)
        mKGRv.layoutManager = LinearLayoutManager(mCtx)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleOrnaData(data: ArrayList<ScreenData>) {
        updateView(data)

        val wayvessel = data.filter { it.name.lowercase().contains("'s wayvessel") }.firstOrNull()
        if (wayvessel != null && (wayvessel.position.left > 70) /* to ignore own wayvessel menu */) {
            if (data.none { it.name.lowercase().contains("this wayvessel is active") }) {
                val name = wayvessel.name.replace("'s Wayvessel", "")
                try {
                    wayvessel
                    if (mSession == null || mSession!!.name != name) {
                        if (mSession != null) {
                            mSession!!.finish()
                            hideSessionOverlay()
                        }
                        mSession = WayvesselSession(name, mCtx)
                        if (mDungeonVisit != null) {
                            mDungeonVisit!!.sessionID = mSession!!.mID
                        }
                        showSessionOverlay()
                        Log.i(TAG, "At wayvessel: $name")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception: $e")
                }
            }
        }

        var leaveParty = data.filter {
            it.name.lowercase().contains("are you sure you would like to leave this party?")
        }.firstOrNull()
        if (leaveParty == null) {
            leaveParty =
                data.filter { it.name.lowercase().contains("are you sure you would return home") }
                    .firstOrNull()
        }
        if (leaveParty != null) {
            Log.d(TAG, "Finished session $mSession")
            mSession?.finish()
            hideSessionOverlay()
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

            var duration: Duration = Duration.ZERO
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
                    hideInviterOverlay()
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
                        if (view != null) {
                            var sessionID: Long? = null
                            if (mSession != null) {
                                sessionID = mSession!!.mID
                            }
                            mDungeonVisit = DungeonVisit(sessionID, view.mDungeonName, view.mMode)
                            Log.d(TAG, "Entered: $mDungeonVisit")
                        }
                    }

                    showSessionOverlay()
                    updateSessionOverlay()
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
                        hideSessionOverlay()
                    }
                    done = true
                }
                OrnaViewUpdateType.DUNGEON_NEW_FLOOR -> if (mDungeonVisit != null) mDungeonVisit!!.floor =
                    (data as Int).toLong()
                OrnaViewUpdateType.DUNGEON_EXPERIENCE -> {
                    if (mDungeonVisit != null) mDungeonVisit!!.experience += data as Int
                    if (mSession != null) mSession!!.experience += data as Int
                    updateSessionOverlay()
                }
                OrnaViewUpdateType.DUNGEON_ORNS -> {
                    if (mDungeonVisit != null) mDungeonVisit!!.orns += data as Int
                    if (mSession != null) mSession!!.orns += data as Int
                    updateSessionOverlay()
                }
                OrnaViewUpdateType.DUNGEON_GOLD -> {
                    if (mDungeonVisit != null) mDungeonVisit!!.gold += data as Int
                    if (mSession != null) mSession!!.gold += data as Int
                    updateSessionOverlay()
                }
                OrnaViewUpdateType.NOTIFICATIONS_INVITERS -> {
                    updateInviterOverlay(data as MutableMap<String, Rect>)
                }
                OrnaViewUpdateType.KINGDOM_GAUNTLET_LIST -> {
                    with(data as List<*>) {
                        Log.i(TAG, "@$data")
                    }

                    val list = Kingdom(mCtx).sortKGItems(data as List<KingdomMember>, mSleepers)
                    updateKGOverlay(list)
                    Kingdom(mCtx).createKGDiscordPost(list)
                }
            }
        }

        if (done) {
            if (mDungeonVisit != null) {
                mDungeonVisit!!.finish()
                dbHelper.insertData(mDungeonVisit!!)
                Log.i(TAG, "Stored: $mDungeonVisit")
            }
            mDungeonVisit = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateInviterOverlay(data: MutableMap<String, Rect>) {
        val dDB = DungeonVisitDatabaseHelper(mCtx)
        val wvDB = WayvesselSessionDatabaseHelper(mCtx)
        mNotificationList.clear()

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
        mNotificationList.add(
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
                val visits = dDB.getVisitsForSession(s.mID)
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
            mNotificationList.add(
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

        }

        mNotificationRv.adapter?.notifyDataSetChanged()
        showInviterOverlay()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private fun showInviterOverlay() {
        if (mNotificationView.parent == null) {
            val flags =
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            val paramFloat = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                flags,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            paramFloat.width = 800
            paramFloat.gravity = Gravity.TOP or Gravity.LEFT
            paramFloat.x = 0
            paramFloat.y = 0
            Log.i(TAG, "Adding view")
            mWM.addView(mNotificationView, paramFloat)

            mNotificationRv.setOnClickListener {
                hideInviterOverlay()
            }
        }
    }

    private fun hideInviterOverlay() {
        if (mNotificationView.parent != null) {
            Log.i(TAG, "Removing view")
            mWM.removeViewImmediate(mNotificationView)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateKGOverlay(data: List<KingdomMember>) {
        mKGList.clear()

        mKGList.add(
            KGItem(
                "Player",
                "Floors",
                "Im",
                "Sleep",
                false
            )
        )

        data.forEach {
            var sleeptime = ""
            if (it.endTimeLeftSeconds > 0) {
                val sleepHours = it.endTimeLeftSeconds / 3600
                val sleepMinutes = (it.endTimeLeftSeconds - sleepHours * 3600) / 60
                sleeptime = "${sleepHours}h ${sleepMinutes}m"
            }

            mKGList.add(
                KGItem(
                    it.character,
                    it.floors.keys.joinToString(", "),
                    if (it.immunity) "⭐" else "",
                    sleeptime,
                    it.zerk
                )
            )
        }

        mKGRv.adapter?.notifyDataSetChanged()
        showKGOverlay()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private fun showKGOverlay() {
        if (mKGView.parent == null) {
            val flags =
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            val paramFloat = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                flags,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            paramFloat.width = 1000
            paramFloat.gravity = Gravity.TOP or Gravity.LEFT
            paramFloat.x = 0
            paramFloat.y = 0
            Log.i(TAG, "Adding view")
            mWM.addView(mKGView, paramFloat)

            mKGRv.setOnClickListener {
                hideKGOverlay()
            }
        }
    }

    private fun hideKGOverlay() {
        if (mKGView.parent != null) {
            Log.i(TAG, "Removing view")
            mWM.removeViewImmediate(mKGView)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private fun showSessionOverlay() {
        if (mSessionView.parent == null) {
            val flags =
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            val paramFloat = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                flags,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            paramFloat.width = 550
            paramFloat.gravity = Gravity.TOP or Gravity.LEFT
            paramFloat.x = 0
            paramFloat.y = 470
            Log.i(TAG, "Adding session view")
            mWM.addView(mSessionView, paramFloat)
            updateSessionOverlay()
        }
    }

    fun createSessionGoldOrnNumberString(value: Long): String {
        if (value > 1000000) {
            return "%.1f m".format(value.toFloat() / 1000000)
        } else if (value > 1000) {
            return "%.1f k".format(value.toFloat() / 1000)
        } else {
            return "$value"
        }
    }

    private fun updateSessionOverlay() {
        if (mSession != null) {
            mSessionGoldTv.text = createSessionGoldOrnNumberString(mSession!!.gold)
            mSessionOrnTv.text = createSessionGoldOrnNumberString(mSession!!.orns)
        } else {
            mSessionGoldTv.text = "0"
            mSessionOrnTv.text = "0"
        }

        if (mDungeonVisit != null) {
            mDungeonGoldTv.text = createSessionGoldOrnNumberString(mDungeonVisit!!.gold)
            mDungeonOrnTv.text = createSessionGoldOrnNumberString(mDungeonVisit!!.orns)
        } else {
            if (mSession == null) {
                mDungeonGoldTv.text = "0"
                mDungeonOrnTv.text = "0"
            } else {
                // Keep the last dungeon data when we are in session
            }
        }
    }

    private fun hideSessionOverlay() {
        if (mSessionView.parent != null) {
            Log.i(TAG, "Removing view")
            mWM.removeViewImmediate(mSessionView)
        }
    }
}