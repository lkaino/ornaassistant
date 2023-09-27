import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import androidx.annotation.RequiresApi
import com.rockethat.ornaassistant.KingdomMember

class KingdomMemberDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_NAME (" +
                    "$COL_1 TEXT PRIMARY KEY," +
                    "$COL_2 TEXT," +
                    "$COL_3 INTEGER" +
                    ")"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 1 && newVersion == 2) {
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COL_3 INTEGER DEFAULT 1000");
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun insertData(entry: KingdomMember): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()

        if (getEntry(entry.character) != null) {
            updateData(entry)
        } else {
            contentValues.put(COL_1, entry.character.replace("'", "''"))
            contentValues.put(COL_2, entry.discordName.replace("'", "''"))
            contentValues.put(COL_3, entry.timezone)

            db.insert(TABLE_NAME, null, contentValues)
        }

        val entries = toEntries(
            db.rawQuery(
                "SELECT * FROM $TABLE_NAME " +
                        "WHERE ign = '${entry.character.replace("'", "''")}' " +
                        "AND discord = '${entry.discordName.replace("'", "''")}'",
                null
            )
        )

        return entries.isNotEmpty()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateData(entry: KingdomMember):
            Boolean {
        val db = this.writableDatabase

        val existing = getEntry(entry.character)
        if (existing != null && existing.discordName == entry.discordName && existing.timezone == entry.timezone) {
            return false
        } else {
            val contentValues = ContentValues()
            contentValues.put(COL_1, entry.character.replace("'", "''"))
            contentValues.put(COL_2, entry.discordName.replace("'", "''"))
            contentValues.put(COL_3, entry.timezone)
            db.update(
                TABLE_NAME,
                contentValues,
                "ign = ?",
                arrayOf(entry.character.replace("'", "''"))
            )
        }

        return true
    }

    fun deleteData(id: String): Int {
        val db = this.writableDatabase
        return db.delete(TABLE_NAME, "ID = ?", arrayOf(id))
    }

    fun deleteAllData() {
        val db = this.writableDatabase
        return db.execSQL("delete from $TABLE_NAME")
    }

    val allData: ArrayList<KingdomMember>
        @RequiresApi(Build.VERSION_CODES.O)
        get() {
            val db = this.writableDatabase
            return toEntries(db.rawQuery("SELECT * FROM $TABLE_NAME", null))
        }


    @RequiresApi(Build.VERSION_CODES.O)
    fun getEntry(ign: String): KingdomMember? {
        val db = this.writableDatabase
        return toEntries(
            db.rawQuery(
                "SELECT * FROM $TABLE_NAME WHERE ign = '${
                    ign.replace("'", "''")
                }'", null
            )
        ).firstOrNull()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun toEntries(cur: Cursor): ArrayList<KingdomMember> {
        val list = ArrayList<KingdomMember>()
        while (cur.moveToNext()) {
            var col = 0
            val ign = cur.getString(col++)
            val discord = cur.getString(col++)
            val tz = cur.getInt(col++)

            val member = KingdomMember(ign, mutableMapOf())
            member.discordName = discord
            member.timezone = tz

            list.add(member)
        }
        cur.close()

        return list
    }

    companion object {
        val DATABASE_NAME = "kingdom.db"
        val TABLE_NAME = "kingdom_member"
        val COL_1 = "ign"
        val COL_2 = "discord"
        val COL_3 = "tz"
        val VERSION = 2

    }
}
