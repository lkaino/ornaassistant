package com.rockethat.ornaassistant.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import androidx.annotation.RequiresApi
import com.rockethat.ornaassistant.WayvesselSession
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class WayvesselSessionDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {

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
                    "name TEXT," +
                    "orns INTEGER," +
                    "gold INTEGER," +
                    "experience INTEGER" +
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
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    /**
     * Let's create our insertData() method.
     * It Will insert data to SQLIte database.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun insertData(entry: WayvesselSession): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        val started =
            entry.mStarted.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000
        contentValues.put(COL_2, started)
        contentValues.put(COL_3, entry.mDurationSeconds)
        contentValues.put(COL_4, entry.name)
        contentValues.put(COL_5, entry.orns)
        contentValues.put(COL_6, entry.gold)
        contentValues.put(COL_7, entry.experience)

        db.insert(TABLE_NAME, null, contentValues)
        val entries = toEntries(
            db.rawQuery(
                "SELECT * FROM $TABLE_NAME " +
                        "WHERE started = $started " +
                        "AND duration = ${entry.mDurationSeconds} " +
                        "AND name = '${entry.name}' " +
                        "AND orns = ${entry.orns} " +
                        "AND gold = ${entry.gold} " +
                        "AND experience = ${entry.experience} ",
                null
            )
        )

        return if (entries.size == 1) {
            entries.first().mID
        } else {
            -1
        }
    }

    /**
     * Let's create  a method to update a row with new field values.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun updateData(id: String, entry: WayvesselSession):
            Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COL_1, id)
        contentValues.put(
            COL_2,
            entry.mStarted.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000
        )
        contentValues.put(COL_3, entry.mDurationSeconds)
        contentValues.put(COL_4, entry.name)
        contentValues.put(COL_5, entry.orns)
        contentValues.put(COL_6, entry.gold)
        contentValues.put(COL_7, entry.experience)
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
    private fun toEntries(cur: Cursor): ArrayList<WayvesselSession> {
        var list = ArrayList<WayvesselSession>()
        while (cur.moveToNext()) {
            var col = 0
            val id = cur.getLong(col++)
            val started = cur.getLong(col++)
            val duration = cur.getLong(col++)
            val name = cur.getString(col++)
            val orns = cur.getLong(col++)
            val gold = cur.getLong(col++)
            val experience = cur.getLong(col++)

            val session = WayvesselSession(name, id)
            session.mStarted =
                LocalDateTime.ofInstant(Instant.ofEpochSecond(started), ZoneId.systemDefault())
            session.mDurationSeconds = duration
            session.orns = orns
            session.gold = gold
            session.experience = experience

            list.add(session)
        }

        cur.close()

        return list
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getEntriesBetween(start: LocalDateTime, end: LocalDateTime): ArrayList<WayvesselSession> {
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
    fun getLastNSessionsFor(name: String, n: Int): ArrayList<WayvesselSession> {
        val db = this.writableDatabase
        return toEntries(
            db.rawQuery(
                "SELECT * FROM $TABLE_NAME " +
                        "WHERE name='${name.replace("'", "''")}' " +
                        "ORDER BY ID DESC LIMIT $n ",
                null
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getLastNSessions(n: Int): ArrayList<WayvesselSession> {
        val db = this.writableDatabase
        return toEntries(
            db.rawQuery(
                "SELECT * FROM $TABLE_NAME " +
                        "ORDER BY ID DESC LIMIT $n ",
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
        val DATABASE_NAME = "wvSession.db"
        val TABLE_NAME = "wayvessel_session"
        val COL_1 = "ID"
        val COL_2 = "started"
        val COL_3 = "duration"
        val COL_4 = "name"
        val COL_5 = "orns"
        val COL_6 = "gold"
        val COL_7 = "experience"
    }
}