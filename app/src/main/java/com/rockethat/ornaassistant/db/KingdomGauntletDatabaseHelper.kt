package com.rockethat.ornaassistant.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class KingdomGauntletDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, 2) {

    /**
     * Our onCreate() method.
     * Called when the database is created for the first time. This is
     * where the creation of tables and the initial population of the tables
     * should happen.
     */
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_NAME (" +
                    "time INTEGER," +
                    "name TEXT" +
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
    fun insertData(dt: LocalDateTime, name: String ) {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COL_1, dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000)
        contentValues.put(COL_2, name)
        db.insert(TABLE_NAME, null, contentValues)
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
    private fun toEntries(cur: Cursor): Map<LocalDateTime, String> {
        var map = mutableMapOf<LocalDateTime, String>()

        while (cur.moveToNext()) {
            var col = 0
            val started = cur.getLong(col++)
            val name = cur.getString(col++)

            val startedDt = LocalDateTime.ofInstant(Instant.ofEpochSecond(started), ZoneId.systemDefault())

            map[startedDt] = name
        }
        cur.close()

        return map
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getLastNEntries(n: Int): Map<LocalDateTime, String> {
        val db = this.writableDatabase
        return toEntries(
            db.rawQuery(
                "SELECT * FROM $TABLE_NAME " +
                        "ORDER BY time DESC LIMIT $n ",
                null
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getEntriesBetween(start: LocalDateTime, end: LocalDateTime, name: String): Map<LocalDateTime, String> {
        val startUnix = start.toEpochSecond(ZoneOffset.UTC)
        val endUnix = end.toEpochSecond(ZoneOffset.UTC)
        val db = this.writableDatabase
        return toEntries(
            db.rawQuery(
                "SELECT * FROM $TABLE_NAME " +
                        "WHERE time > $startUnix " +
                        "AND time < $endUnix " +
                        "AND name = '${name.replace("'", "''")}'",
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
        val DATABASE_NAME = "kingdomGauntlet.db"
        val TABLE_NAME = "kg"
        val COL_1 = "time"
        val COL_2 = "name"
    }
}