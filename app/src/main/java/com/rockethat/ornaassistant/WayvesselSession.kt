package com.rockethat.ornaassistant

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.rockethat.ornaassistant.db.WayvesselSessionDatabaseHelper
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@RequiresApi(Build.VERSION_CODES.O)
class WayvesselSession(val name: String, val mCtx: Context?) {
    var orns: Long = 0
    var gold: Long = 0
    var experience: Long = 0
    var mID: Long = 0

    var mStarted: LocalDateTime = LocalDateTime.now()
    var mDurationSeconds: Long = 0
    var mDungeonsVisited = 0

    init {
        if (mCtx != null)
        {
            val db = WayvesselSessionDatabaseHelper(mCtx)
            mID = db.insertData(this)
            db.close()
        }
    }

    constructor(name: String, id: Long) : this(name, null) {
        mID = id
    }

    fun finish() {
        mDurationSeconds = ChronoUnit.SECONDS.between(mStarted, LocalDateTime.now())
        if (mCtx != null)
        {
            val db = WayvesselSessionDatabaseHelper(mCtx)
            db.updateData(mID.toString(), this)
        }
    }

    override fun toString(): String {
        return "Wayvessel session with ID $mID started at $mStarted in $name, duration $mDurationSeconds s, gold $gold, experience $experience, orns $orns"
    }
}