package com.rockethat.ornaassistant

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
import com.rockethat.ornaassistant.overlays.KGOverlay
import com.rockethat.ornaassistant.overlays.SessionOverlay
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.net.Uri
import android.provider.Settings.Global.getString
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.rockethat.ornaassistant.ornaviews.OrnaViewDungeonEntry
import com.rockethat.ornaassistant.overlays.AssessOverlay
import org.json.JSONObject
import java.util.concurrent.LinkedBlockingDeque
import kotlin.concurrent.thread
import android.media.MediaPlayer
import androidx.legacy.content.WakefulBroadcastReceiver
import androidx.work.*
import java.util.concurrent.TimeUnit


@RequiresApi(Build.VERSION_CODES.O)
class MainState(
    private val mWM: WindowManager,
    private val mCtx: Context,
    mNotificationView: View,
    mSessionView: View,
    mKGView: View,
    mAssessView: View
) {
    private val TAG = "OrnaMainState"
    private val CHANNEL_ID = "ornaassistant_channel_"
    private val mNotificationID = 101
    private val mDungeonDbHelper = DungeonVisitDatabaseHelper(mCtx)
    private var mCurrentView: OrnaView? = null
    private var mDungeonVisit: DungeonVisit? = null
    private var mSession: WayvesselSession? = null

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

    private val mKGOverlay = KGOverlay(mWM, mCtx, mKGView, 0.7)
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
    var mKingdomGauntlet = KingdomGauntlet(mCtx)

    var sharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                "kg" -> if (!sharedPreferences.getBoolean(key, true)) mKGOverlay.hide()
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

                if (data != null) {
                    handleOrnaData(data)
                }
            }
        }

        thread {
            while (true) {
                val data: ArrayList<ScreenData>? = mDiscordQueue.take()

                if (data != null) {
                    mKingdomGauntlet.handleDiscordData(data)
                }
            }
        }
    }


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

                    if (mSharedPreference.getBoolean("session", true)) {
                        mSessionOverlay.update(mSession, mDungeonVisit)
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
                    done = true
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
                OrnaViewUpdateType.KINGDOM_GAUNTLET_LIST -> {
                    updateKG(data as List<KingdomMember>)
                }
                OrnaViewUpdateType.ITEM_ASSESS_RESULTS -> {
                    mAssessOverlay.update(data as JSONObject)
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

    private fun updateKG(items: List<KingdomMember>) {
        val dtNow = LocalDateTime.now()
        val new = KingdomGauntlet(mCtx)
        val uniqueThis = mutableListOf<KingdomMember>()
        val uniqueOther = mutableListOf<KingdomMember>()
        new.updateItems(items)
        mKingdomGauntlet.diffFloors(new, uniqueThis, uniqueOther)

        if (dtNow.isBefore(mKingdomGauntlet.mLastUpdate.plusSeconds(30)) &&
            uniqueThis.size > 1
        ) {
            // Ignore the data before 5 seconds has passed since last update
            Log.i(TAG, "Discarding kingdom gauntlet data: ${uniqueThis.size}")
            return
        } else if (new.mList.isEmpty()) {
            return
        }

        if (uniqueThis.size > 0 || uniqueOther.size > 0 ||
            (!mKGOverlay.mVisible.get() && dtNow.isAfter(mKGNextUpdate))
        ) {
            var shuffle = false

            Log.i(TAG, "mKingdomGauntlet (${items.size}): ${mKingdomGauntlet.mList}")
            Log.i(TAG, "new (${items.size}): ${new.mList}")
            Log.i(TAG, "uniqueThis: $uniqueThis")
            Log.i(TAG, "uniqueOther: $uniqueOther")

            if ((uniqueThis.size == 1) && (uniqueThis.first().floors.size == 1)) {
                Log.i(TAG, "One floor was removed!")
                // One floor was removed
                val otherMember = uniqueOther.firstOrNull { it.floors.containsKey(40) }
                if (otherMember != null) {
                    // One of the new floors contains floor 40, this is a shuffle
                    Log.i(
                        TAG,
                        "One of the new floors contains floor 40, this is a shuffle"
                    )
                    if (mSharedPreference.getBoolean("discord", true)) {
                        shuffle = true
                        Log.i(TAG, "Creating shuffle discord post")
                        mKingdomGauntlet.createKGDiscordShufflePost(
                            uniqueThis.first(),
                            otherMember

                        )
                        if (mSharedPreference.getBoolean("nKGShuffle", true))
                        {
                            scheduleShuffleNotification(20)
                        }
                    }
                }
            }

            val sortedList = mKingdomGauntlet.updateItems(items)

            if (mSharedPreference.getBoolean("kg", true)) {
                Log.i(TAG, "Updated KG view with ${sortedList.size} items")
                mKGOverlay.update(sortedList)
            }

            mKGNextUpdate = if (!shuffle) {
                if (mSharedPreference.getBoolean("discord", true)) {
                    mKingdomGauntlet.createKGDiscordPost()
                }
                dtNow.plusSeconds(2)
            } else {
                dtNow.plusSeconds(20)
            }
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

    private fun getRandomShuffleSound() : String {

        return "android.resource://" + mCtx.packageName + "/" + mShuffleRes[(mShuffleRes.indices).random()]
    }

    private fun scheduleShuffleNotification(delayMinutes: Long) {

        val data = Data.Builder()
        data.putString("channelID", getRandomShuffleChannel())
        data.putString("title", "Shuffle")
        data.putString("description", "Time to shuffle!")
        val work =
            OneTimeWorkRequestBuilder<OneTimeScheduleWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .addTag("WorkTag")
                .setInputData(data.build())
                .build()

        WorkManager.getInstance(mCtx).enqueue(work)
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
