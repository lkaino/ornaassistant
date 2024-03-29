package com.rockethat.ornaassistant

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.rockethat.ornaassistant.db.DungeonVisitDatabaseHelper
import com.rockethat.ornaassistant.overlays.InviterOverlay
import java.time.LocalDateTime
import java.util.*
import com.rockethat.ornaassistant.overlays.SessionOverlay
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.rockethat.ornaassistant.ornaviews.OrnaViewDungeonEntry
import com.rockethat.ornaassistant.overlays.AssessOverlay
import org.json.JSONObject
import java.util.concurrent.LinkedBlockingDeque
import kotlin.concurrent.thread
import androidx.work.*
import java.util.concurrent.TimeUnit


@RequiresApi(Build.VERSION_CODES.O)
class MainState(
    private val mWM: WindowManager,
    private val mCtx: Context,
    mNotificationView: View,
    mSessionView: View,
    mAssessView: View,
    mAS: AccessibilityService
) {
    private val TAG = "OrnaMainState"
    private val mDungeonDbHelper = DungeonVisitDatabaseHelper(mCtx)
    private var mCurrentView: OrnaView? = null
    private var mDungeonVisit: DungeonVisit? = null
    private var mOnholdVisits = mutableMapOf<String, DungeonVisit>()
    private var mSession: WayvesselSession? = null
    private var mBattle = Battle(mAS)

    private val mShuffleRes = listOf(
        R.raw.shuffle_1,
        R.raw.shuffle_2,
        R.raw.shuffle_3,
        R.raw.shuffle_4,
        R.raw.shuffle_5,
        R.raw.shuffle_6,
        R.raw.shuffle_7,
    )

    private val mWayvesselNotificationChannelName = "ornaassistant_channel_wayvessel"
    private val mShuffleNotificationChannelNameBase = "ornaassistant_channel_shuffle_"
    private val mShuffleNotificationChannelNames = mutableListOf<String>()

    private val mInviterOverlay = InviterOverlay(mWM, mCtx, mNotificationView, 0.8)
    private val mSessionOverlay = SessionOverlay(mWM, mCtx, mSessionView, 0.4)
    private val mAssessOverlay = AssessOverlay(mWM, mCtx, mAssessView, 0.7)

    val mOrnaQueue = LinkedBlockingDeque<ArrayList<ScreenData>>()
    val mDiscordQueue = LinkedBlockingDeque<ArrayList<ScreenData>>()

    private val mSharedPreference: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(mCtx)

    @RequiresApi(Build.VERSION_CODES.O)
    var mKGNextUpdate: LocalDateTime = LocalDateTime.now()

    @RequiresApi(Build.VERSION_CODES.N)

    var mInBattle = LocalDateTime.now().minusDays(1)

    var sharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                "invites" -> if (!sharedPreferences.getBoolean(key, true)) mInviterOverlay.hide()
                "session" -> if (!sharedPreferences.getBoolean(key, true)) mSessionOverlay.hide()
            }
        }

    init {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mCtx)
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)

        createNotificationChannel()

        thread {
            while (true) {
                val data: ArrayList<ScreenData>? = mOrnaQueue.take()
                data?.let { handleOrnaData(it) }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleOrnaData(data: ArrayList<ScreenData>) {
        if (Battle.inBattle(data))
        {
            mInBattle = LocalDateTime.now()
        }

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
                        if (mSharedPreference.getBoolean("nWayvessel", true))
                        {
                            scheduleWayvesselNotification(60)
                        }
                        if (mDungeonVisit != null) {
                            mDungeonVisit!!.sessionID = mSession!!.mID
                        }
                        if (mSharedPreference.getBoolean("session", true)) {
                            mSessionOverlay.update(mSession, mDungeonVisit)
                        }
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

    var maxSize = 0

    @RequiresApi(Build.VERSION_CODES.O)
    fun processData(packageName: String, data: ArrayList<ScreenData>) {
        if (packageName.contains("orna")) {
            mOrnaQueue.put(data)
            if (mOrnaQueue.size > maxSize) {
                maxSize = mOrnaQueue.size
                Log.i(TAG, "QUEUE $maxSize")
            }
        } else if (packageName.contains("discord")) {
            mDiscordQueue.put(data)
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
            var update = true
            if (mCurrentView != null) {
                if (newType == OrnaViewType.ITEM) {
                    if (!mSharedPreference.getBoolean("assess", true)) {
                        update = false
                    }
                }
                if (mCurrentView!!.type == OrnaViewType.ITEM) {
                    mAssessOverlay.hide()
                }
            }


            if (view != null) {
                if (mCurrentView != null) mCurrentView!!.close()
                mCurrentView = view
                if (update) {
                    mCurrentView?.update(data, ::processUpdate)
                }
                Log.d(TAG, "VIEW CHANGED TO ${view.type}")
                if (view.type != OrnaViewType.NOTIFICATIONS) {
                    mInviterOverlay.hide()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun processUpdate(update: MutableMap<OrnaViewUpdateType, Any?>) {
        var dungeonDone = false
        var dungeonFailed = false
        update.forEach { (type, data) ->
            when (type) {
                OrnaViewUpdateType.DUNGEON_ENTERED -> {
                    if (mDungeonVisit == null) {
                        val view: OrnaViewDungeonEntry = mCurrentView as OrnaViewDungeonEntry
                        if (mOnholdVisits.containsKey(view.mDungeonName))
                        {
                            Log.i(TAG, "Reloading on hold visit to ${view.mDungeonName}.")
                            mDungeonVisit = mOnholdVisits[view.mDungeonName]
                            mOnholdVisits.remove(view.mDungeonName)
                        }
                        else
                        {
                            var sessionID: Long? = null
                            if (mSession != null) {
                                sessionID = mSession!!.mID
                                mSession!!.mDungeonsVisited++
                            }
                            mDungeonVisit = DungeonVisit(sessionID, view.mDungeonName, view.mMode)
                            Log.d(TAG, "Entered: $mDungeonVisit")
                        }
                    }

                    if (mSharedPreference.getBoolean("session", true)) {
                        mSessionOverlay.update(mSession, mDungeonVisit)
                    }
                }
                OrnaViewUpdateType.DUNGEON_NEW_DUNGEON -> {
                    if (mDungeonVisit != null) {
                        Log.i(TAG, "Putting on hold visit to ${mDungeonVisit!!.name}.")
                        mOnholdVisits[mDungeonVisit!!.name] = mDungeonVisit!!
                        mDungeonVisit = null
                    }
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
                    dungeonDone = true
                }
                OrnaViewUpdateType.DUNGEON_FAIL -> if (mDungeonVisit != null) {
                    if (mSession == null) {
                        mSessionOverlay.hide()
                    }
                    dungeonDone = true
                    dungeonFailed = true
                }
                OrnaViewUpdateType.DUNGEON_NEW_FLOOR -> if (mDungeonVisit != null) mDungeonVisit!!.floor =
                    (data as Int).toLong()
                OrnaViewUpdateType.DUNGEON_EXPERIENCE -> {
                    if (mDungeonVisit != null) mDungeonVisit!!.experience += data as Int
                    if (mSession != null) mSession!!.experience += data as Int
                    if (mSharedPreference.getBoolean("session", true)) {
                        mSessionOverlay.update(mSession, mDungeonVisit)
                    }
                }
                OrnaViewUpdateType.DUNGEON_ORNS -> {
                    if (mDungeonVisit != null) mDungeonVisit!!.orns += data as Int
                    if (mSession != null) mSession!!.orns += data as Int
                    if (mSharedPreference.getBoolean("session", true)) {
                        mSessionOverlay.update(mSession, mDungeonVisit)
                    }
                }
                OrnaViewUpdateType.DUNGEON_GOLD -> {
                    if (mDungeonVisit != null) mDungeonVisit!!.gold += data as Int
                    if (mSession != null) mSession!!.gold += data as Int
                    if (mSharedPreference.getBoolean("session", true)) {
                        mSessionOverlay.update(mSession, mDungeonVisit)
                    }
                }
                OrnaViewUpdateType.NOTIFICATIONS_INVITERS -> {
                    if (mSharedPreference.getBoolean("invites", true)) {
                        mInviterOverlay.update(data as MutableMap<String, Rect>)
                    }
                }
                OrnaViewUpdateType.ITEM_ASSESS_RESULTS -> {
                    mAssessOverlay.update(data as JSONObject)
                }

                OrnaViewUpdateType.KINGDOM_GAUNTLET_LIST -> TODO()
            }
        }

        if (dungeonDone) {
            if (mDungeonVisit != null) {
                mDungeonVisit!!.completed = !dungeonFailed
                mDungeonVisit!!.finish()
                mDungeonDbHelper.insertData(mDungeonVisit!!)
                Log.i(TAG, "Stored: $mDungeonVisit")
            }
            mDungeonVisit = null
        }
    }

    private fun createNotificationChannel() {
        //val sound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + mCtx.packageName + "/" + R.raw.FILE_NAME);  //Here is FILE_NAME is the name of file that you want to play
        Log.i(TAG, "createNotificationChannel")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.i(TAG, "createNotificationChannel creating")
            // Create the NotificationChannel

            for (i in mShuffleRes.indices) {
                val channelName = mShuffleNotificationChannelNameBase + i
                mShuffleNotificationChannelNames.add(channelName)
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val mChannel = NotificationChannel(channelName, channelName, importance)
                mChannel.description = channelName
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                mChannel.setSound(
                    Uri.parse("android.resource://" + mCtx.packageName + "/" + mShuffleRes[i]),
                    attributes
                )
                mChannel.enableVibration(true)
                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this
                val notificationManager =
                    mCtx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(mChannel)
            }

            val channelName = mWayvesselNotificationChannelName
            mShuffleNotificationChannelNames.add(channelName)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(channelName, channelName, importance)
            mChannel.description = channelName
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            mChannel.setSound(
                Uri.parse("android.resource://" + mCtx.packageName + "/" + R.raw.wv_1),
                attributes
            )
            mChannel.enableVibration(true)
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager =
                mCtx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)

        }
    }

    private fun getRandomShuffleChannel() : String {

        return mShuffleNotificationChannelNames[(mShuffleNotificationChannelNames.indices).random()]
    }

    private fun scheduleWayvesselNotification(delayMinutes: Long) {

        val data = Data.Builder()
        data.putString("channelID", mWayvesselNotificationChannelName)
        data.putString("title", "Wayvessel")
        data.putString("description", "Wayvessel is open!")
        val work =
            OneTimeWorkRequestBuilder<OneTimeScheduleWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .addTag("WorkTag")
                .setInputData(data.build())
                .build()

        WorkManager.getInstance(mCtx).enqueue(work)
    }


    class OneTimeScheduleWorker(
        val context: Context,
        workerParams: WorkerParameters
    ) : Worker(context, workerParams) {

        override fun doWork(): Result {
            val channelID = inputData.getString("channelID")
            val builder = NotificationCompat.Builder(context, channelID.toString())
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(inputData.getString("title"))
                .setContentText(inputData.getString("description"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            with(NotificationManagerCompat.from(context)) {
                notify(101, builder.build())
            }

            return Result.success()
        }

    }
}
