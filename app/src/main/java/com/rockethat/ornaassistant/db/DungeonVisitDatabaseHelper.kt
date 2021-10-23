package com.rockethat.ornaassistant.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import androidx.annotation.RequiresApi
import com.rockethat.ornaassistant.DungeonMode
import com.rockethat.ornaassistant.DungeonVisit
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class DungeonVisitDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, VERSION) {

    /**
     * Our onCreate() method.
     * Called when the database is created for the first time. This is
     * where the creation of tables and the initial population of the tables
     * should happen.
     */
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_NAME (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "started INTEGER," +
                    "duration INTEGER," +
                    "session INTEGER," +
                    "name TEXT," +
                    "hard INTEGER," +
                    "type TEXT," +
                    "orns INTEGER," +
                    "gold INTEGER," +
                    "experience INTEGER," +
                    "floor INTEGER," +
                    "godforges INTEGER" +
                    "completed INTEGER" +
                    ")"
        )
    }

    /**
     * Let's create Our onUpgrade method
     * Called when the database needs to be upgraded. The implementation should
     * use this method to drop tables, add tables, or do anything else it needs
     * to upgrade to the new schema version.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 2 && newVersion == 3)
        {
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COL_13 INTEGER DEFAULT 0");
        }
    }

    /**
     * Let's create our insertData() method.
     * It Will insert data to SQLIte database.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun insertData(entry: DungeonVisit) {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COL_2, entry.mStarted.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000)
        contentValues.put(COL_3, entry.mDurationSeconds)
        contentValues.put(COL_4, if (entry.sessionID != null) entry.sessionID else -1)
        contentValues.put(COL_5, entry.name)
        contentValues.put(COL_6, entry.mode.mbHard)
        contentValues.put(COL_7, entry.mode.mMode.toString())
        contentValues.put(COL_8, entry.orns)
        contentValues.put(COL_9, entry.gold)
        contentValues.put(COL_10, entry.experience)
        contentValues.put(COL_11, entry.floor)
        contentValues.put(COL_12, entry.godforges)
        contentValues.put(COL_13, if (entry.completed) 1 else 0)
        db.insert(TABLE_NAME, null, contentValues)
    }

    /**
     * Let's create  a method to update a row with new field values.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun updateData(id: String, entry: DungeonVisit):
            Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COL_1, id)
        contentValues.put(COL_2, entry.mStarted.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000)
        contentValues.put(COL_3, entry.mDurationSeconds)
        contentValues.put(COL_4, if (entry.sessionID != null) entry.sessionID else -1)
        contentValues.put(COL_5, entry.name)
        contentValues.put(COL_6, entry.mode.mbHard)
        contentValues.put(COL_7, entry.mode.mMode.toString())
        contentValues.put(COL_8, entry.orns)
        contentValues.put(COL_9, entry.gold)
        contentValues.put(COL_10, entry.experience)
        contentValues.put(COL_11, entry.floor)
        contentValues.put(COL_12, entry.godforges)
        contentValues.put(COL_13, if (entry.completed) 1 else 0)
        db.update(TABLE_NAME, contentValues, "ID = ?", arrayOf(id))
        return true
    }

    /**
     * Let's create a function to delete a given row based on the id.
     */
    fun deleteData(id: String): Int {
        val db = this.writableDatabase
        return db.delete(TABLE_NAME, "ID = ?", arrayOf(id))
    }

    fun deleteAllData() {
        val db = this.writableDatabase
        return db.execSQL("delete from $TABLE_NAME")
    }

    /**
     * The below getter property will return a Cursor containing our dataset.
     */
    val allData: Cursor
        get() {
            val db = this.writableDatabase
            val res = db.rawQuery("SELECT * FROM $TABLE_NAME", null)
            return res
        }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun toEntries(cur: Cursor): ArrayList<DungeonVisit> {
        var list = ArrayList<DungeonVisit>()
        while (cur.moveToNext()) {
            var col = 1
            val started = cur.getLong(col++)
            val duration = cur.getLong(col++)
            val sessionId = cur.getLong(col++)
            val name = cur.getString(col++)
            val hard = cur.getInt(col++) == 1
            val type = cur.getString(col++)
            val orns = cur.getLong(col++)
            val gold = cur.getLong(col++)
            val experience = cur.getLong(col++)
            val floor = cur.getLong(col++)
            val godforges = cur.getLong(col++)
            val completed = cur.getInt(col++) == 1

            val mode = DungeonMode(DungeonMode.Modes.valueOf(type))
            mode.mbHard = hard
            val visit = DungeonVisit(sessionId, name, mode)
            visit.mStarted = LocalDateTime.ofInstant(Instant.ofEpochSecond(started), ZoneId.systemDefault())
            visit.mDurationSeconds = duration
            visit.orns = orns
            visit.gold = gold
            visit.experience = experience
            visit.floor = floor
            visit.godforges = godforges
            visit.completed = completed

            list.add(visit)
        }

        cur.close()

        return list
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getEntriesBetween(start: LocalDateTime, end: LocalDateTime): ArrayList<DungeonVisit> {
        val startUnix = start.toEpochSecond(ZoneOffset.UTC)
        val endUnix = end.toEpochSecond(ZoneOffset.UTC)
        val db = this.writableDatabase
        return toEntries(
            db.rawQuery(
                "SELECT * FROM $TABLE_NAME " +
                        "WHERE started > $startUnix " +
                        "AND started < $endUnix ",
                null
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getVisitsForSession(session: Long): ArrayList<DungeonVisit> {
        val db = this.writableDatabase
        return toEntries(
                db.rawQuery(
                        "SELECT * FROM $TABLE_NAME " +
                                "WHERE session='$session' ",
                        null
                )
        )
    }

    /**
     * Let's create a companion object to hold our static fields.
     * A Companion object is an object that is common to all instances of a given
     * class.
     */
    companion object {
        val DATABASE_NAME = "dungeonVisits.db"
        val TABLE_NAME = "dungeon"
        val COL_1 = "ID"
        val COL_2 = "started"
        val COL_3 = "duration"
        val COL_4 = "session"
        val COL_5 = "name"
        val COL_6 = "hard"
        val COL_7 = "type"
        val COL_8 = "orns"
        val COL_9 = "gold"
        val COL_10 = "experience"
        val COL_11 = "floor"
        val COL_12 = "godforges"
        val COL_13 = "completed"
        val VERSION = 3
    }
}