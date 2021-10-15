package com.rockethat.ornaassistant

import KingdomMemberDatabaseHelper
import android.util.Log

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.rockethat.ornaassistant.db.KingdomGauntletDatabaseHelper
import java.time.LocalDateTime
import java.util.ArrayList
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.math.floor

@RequiresApi(Build.VERSION_CODES.N)
class KingdomGauntlet(val mCtx: Context) {
    private val TAG = "OrnaKingdomGauntlet"
    var mSleepers = mutableMapOf<String, Sleeper>()
    var mList = listOf<KingdomMember>()
    var mMap = mutableMapOf<String, KingdomMember>()
    @RequiresApi(Build.VERSION_CODES.O)
    var mLastUpdate = LocalDateTime.now()

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateItems(items: List<KingdomMember>): List<KingdomMember> {
        val kmDB = KingdomMemberDatabaseHelper(mCtx)
        val dbList = kmDB.allData
        kmDB.close()

        items.forEach {
            var discordName = it.character

            dbList.forEach { dbItem ->
                if (dbItem.character == it.character) {
                    discordName = dbItem.discordName
                }
            }

            var sleeper: Sleeper? = null
            mSleepers.keys.forEach { sleeperDiscordName ->
                if (sleeperDiscordName.lowercase().contains(it.character.lowercase())) {
                    sleeper = mSleepers[sleeperDiscordName]
                }
            }
            if (sleeper == null && mSleepers.containsKey(discordName)) {
                sleeper = mSleepers[discordName]
            }

            val endTimeLeft = sleeper?.endTimeLeft()
            if (endTimeLeft != null) {
                it.endTimeLeftSeconds = if (endTimeLeft >= 0) endTimeLeft else 0
            }

            it.immunity = if (it.endTimeLeftSeconds > 0) sleeper?.immunity ?: false else false
            it.discordName = discordName
        }

        val kgDB = KingdomGauntletDatabaseHelper(mCtx)
        val lastEntry = kgDB.getLastNEntries(1)
        val dt = LocalDateTime.now()

        var insert = false
        if (lastEntry.isNotEmpty()) {
            if (dt.isAfter(lastEntry.first().dateTime.plusMinutes(15))) {
                insert = true
            }
        } else {
            insert = true
        }

        if (insert) {
            Log.d("OrnaKG", "Inserting ${items.size} items.")
            items.forEach {
                if (it.endTimeLeftSeconds <= 0) {
                    kgDB.insertData(dt, it.character)
                }
            }
        }

        items.forEach {
            it.seenCount = kgDB.getEntriesBetween(dt.minusMonths(1), dt, it.character).size
        }
        kgDB.close()

        mList = items.sortedWith(
            compareByDescending<KingdomMember> { it.zerk }
                .thenByDescending { it.immunity }
                .thenByDescending { it.endTimeLeftSeconds }
                .thenBy { it.numFloors }
                .thenByDescending { it.seenCount })

        mMap.clear()
        mList.forEach {
            mMap[it.character] = it
        }

        mLastUpdate = LocalDateTime.now()

        return mList
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createKGDiscordPost() {
        var text = ""

        mList.forEach {
            if (it.floors.filterValues { !it.loss }.filterValues { !it.win }.isNotEmpty()) {
                var sleepHours: Long? = null
                var sleepMinutes: Long? = null

                if (it.endTimeLeftSeconds > 0) {
                    sleepHours = it.endTimeLeftSeconds / 3600
                    sleepMinutes = (it.endTimeLeftSeconds - sleepHours * 3600) / 60
                }

                if (sleepHours != null && sleepMinutes != null) {
                    if (it.immunity) {
                        text += ":star: "
                    }
                    text += ":zzz: "
                    if (sleepHours > 0) {
                        text += "${sleepHours}h"
                        if (sleepMinutes <= 0) {
                            text += ", "
                        }
                    }
                    if (sleepMinutes > 0) {
                        text += "${sleepMinutes}m,  "
                    }
                }

                text += "@${it.discordName}   "

                text += if (it.floors.size == 1) {
                    "__**${it.floors.size}**__ floor: "
                } else {
                    "__**${it.floors.size}**__ floors: "
                }
                var floors = mutableListOf<String>()
                it.floors.forEach { (floorNum, floor) ->
                    if (!floor.loss && !floor.win) {
                        if (floor.mobName.lowercase().contains("berserk")) {
                            floors.add("$floorNum :warning:**ZERK ALERT**:warning:")
                        } else {
                            floors.add("$floorNum")
                        }
                    }
                }
                text += floors.joinToString()
                text += "\r\n"
            }
        }

        Log.d(TAG, text)

        val clipboard: ClipboardManager? =
            mCtx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clip = ClipData.newPlainText("label", text)
        clipboard?.setPrimaryClip(clip)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun handleDiscordData(data: ArrayList<ScreenData>) {
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

    fun diffFloors(
        other: KingdomGauntlet,
        uniqueThis: MutableList<KingdomMember>,
        uniqueOther: MutableList<KingdomMember>
    ) {
        mMap.forEach { (memberName, kingdomMember) ->
            if (other.mMap.containsKey(memberName)) {
                val floorsThis = mutableMapOf<Int, KingdomGauntletFloor>()
                val floorsOther = mutableMapOf<Int, KingdomGauntletFloor>()

                kingdomMember.floors.forEach { (floorNum, floor) ->
                    if (other.mMap[memberName]?.floors?.containsKey(floorNum) == false) {
                        // if shuffled, the floor keys are lowered by 1
                        if ((other.mMap[memberName]?.floors?.containsKey(floorNum - 1) == true) &&
                            other.mMap[memberName]?.floors?.get(floorNum - 1)?.mobName == floor.mobName
                        ) {
                            // the floorname is lower because of shuffle
                        } else {
                            floorsThis[floorNum] = floor
                        }
                    }
                }

                other.mMap[memberName]?.floors?.forEach { (floorNum, floor) ->
                    if (!kingdomMember.floors.containsKey(floorNum)) {
                        // if shuffled, the floor keys are lowered by 1
                        if (kingdomMember.floors.containsKey(floorNum + 1) &&
                            kingdomMember.floors[floorNum + 1]?.mobName == floor.mobName
                        ) {
                            // the floorname is higher because of shuffle
                        } else {
                            floorsOther[floorNum] = floor
                        }
                    }
                }

                if (floorsThis.isNotEmpty()) {
                    uniqueThis.add(KingdomMember(memberName, floorsThis))
                }
                if (floorsOther.isNotEmpty()) {
                    uniqueOther.add(KingdomMember(memberName, floorsOther))
                }
            } else {
                uniqueThis.add(kingdomMember)
            }
        }

        other.mMap.forEach { (s, kingdomMember) ->
            if (!mMap.containsKey(s)) {
                uniqueOther.add(kingdomMember)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createKGDiscordShufflePost(left: KingdomMember, right: KingdomMember) {
        var zerk = false
        val zerkMob =
            left.floors.values.filter { it.mobName.lowercase().contains("berserk") }.firstOrNull()
        if (zerkMob != null) {
            zerk = true
        }

        if (left.discordName.isEmpty()) {
            val db = KingdomMemberDatabaseHelper(mCtx)
            val entry = db.getEntry(left.character)
            if (entry != null) {
                left.discordName = entry.discordName
            }
        }

        if (right.discordName.isEmpty()) {
            val db = KingdomMemberDatabaseHelper(mCtx)
            val entry = db.getEntry(right.character)
            if (entry != null) {
                right.discordName = entry.discordName
            }
        }

        var text = "-s @"
        if (left.discordName.isNotEmpty()) {
            text += left.discordName
        } else {
            text += left.character
        }
        text += " @"
        if (right.discordName.isNotEmpty()) {
            text += right.discordName
        } else {
            text += right.character
        }

        if (zerk) {
            text += " free zerk"
        }

        val clipboard: ClipboardManager? =
            mCtx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clip = ClipData.newPlainText("discord_shuffle", text)
        clipboard?.setPrimaryClip(clip)
    }

}
