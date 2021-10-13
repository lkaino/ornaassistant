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

@RequiresApi(Build.VERSION_CODES.N)
class Kingdom(val mCtx: Context) {
    private val TAG = "OrnaKingdom"
    val nameToDiscord = mapOf<String, String>(
        "napalminator" to "napalminator#0807",
        "Katsuke_" to "Katsuke_#2800",
        "StillAnAnimal" to "RandomUser352#6053",
        "Mr AJ" to "Mr. AsianJew#5544",
        "colornokia" to "colornokia#1423",
        "StuartJC" to "StuartJC#7470",
        "-- Loligeddon --" to "ロリゲッドン#1000",
        "HollowGlory" to "dr4kun#2984",
        "astrallz" to "astral_lz#4954",
        "BaseSolution" to "BaseSolution#9249",
        "Der rote Baron" to "Der rote Baron#9122",
        "PrimeMover" to "PrimeMover#0933",
        "Meow47" to "Meow#5380",
        "agentsands" to "agentsands#9749",
        "Apostate" to "Apostate#9893",
        "Gurnnink" to "Gurnnink#1908",
        "Psdo" to "Psdo [-7]#3849",
        "Sage Tellah" to "borensoren#2247",
        "PRDIAS" to "prmd#7706",
        "Delirax" to "GEESE#5365",
        "Schism" to "Sublime#8823",
        "Ibrcadabrah" to "AndresCM#3560",
        "Big Nige" to "Big Nige#6963",
        "Dialda" to "Dialda#6169",
        "Jardar" to "Jardar#1978",
        "f1337foot" to "f1337foot#2280",
        "WarriorIska" to "WarriorIska#1878",
        "Glorimreal" to "Pixel_Dave#3859",
        "Wooyek" to "Wooya#9554",
        "Gutss" to "dibster#2620",
        "Hevler" to "hevler#7036",
        "Lord Kramer" to "golaf#4875",
        "Fuga de la Fixa" to "ALoONe#0847",
        "lauka" to "lauka#3803",
        "GaiaMike" to "🍍Mike🍍#1308",
        "Tsadkiel" to "Tsadkiel#6849",
        "AnthraxScare" to "Acidicorrosion#7461",
        "Krawuzor" to "Lacky#3727",
        "BlueLightning" to "BlueLightning#9608",
        "Skyharim" to "Skyharim#6783",
        "Quczyna vel Kuczyna" to "Quczyna vel Kuczyna#0902",
        "Colletos" to "Collet#7017",
        "Ernesto2" to "ernesto#2975",
        "Tiruriru" to "WolFix#2829",
        "Souzou" to "Souzou#3615",
        "Jilet Ahmet" to "JiletAhmet#3882",
        "BBIYAK" to "BBIYAK [+9]#1242",
        "Gutsy Guts" to "dibster#2620",
        "Glorimrael" to "Pixel_Dave#3859",
        "Flossoraptor" to "Flossoraptor-5#2534",
        "Shinto" to "Shinto#3585",
        "Tsadkiel, O Capeta" to "Tsadkiel#6849",
        "NAPALMINATOR[ACW]" to "napalminator#0807",
        "Ghost Hunter: WarriorIska" to "WarriorIska#1878",
        "Ibracadabrah" to "AndresCM#3560",
        "Arisen Tellah" to "borensoren#2247",
    )

    var discordToName = mutableMapOf<String, String>()

    init {
        nameToDiscord.forEach { (name, discName) ->
            discordToName[discName] = name
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun sortKGItems(
        items: List<KingdomMember>,
        sleepers: Map<String, Sleeper>
    ): List<KingdomMember> {
        val kmDB =  KingdomMemberDatabaseHelper(mCtx)
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
            sleepers.keys.forEach { sleeperDiscordName ->
                if (sleeperDiscordName.lowercase().contains(it.character.lowercase())) {
                    sleeper = sleepers[sleeperDiscordName]
                }
            }
            if (sleeper == null && sleepers.containsKey(discordName)) {
                sleeper = sleepers[discordName]
            }

            val endTimeLeft = sleeper?.endTimeLeft()
            if (endTimeLeft != null)
            {
                it.endTimeLeftSeconds = if (endTimeLeft >= 0) endTimeLeft else 0
            }

            it.immunity = if (it.endTimeLeftSeconds > 0) sleeper?.immunity ?: false else false
            it.discordName = discordName
        }

        val kgDB = KingdomGauntletDatabaseHelper(mCtx)
        val lastEntry = kgDB.getLastNEntries(1)
        val dt = LocalDateTime.now()

        var insert = false
        if (lastEntry.isNotEmpty())
        {
            if (dt.isAfter(lastEntry.keys.first().plusMinutes(15)))
            {
                insert = true
            }
        }
        else
        {
            insert = true
        }

        if (insert)
        {
            Log.d("OrnaKG", "Inserting ${items.size} items.")
            items.forEach {
                if (it.endTimeLeftSeconds <= 0)
                {
                    kgDB.insertData(dt, it.character)
                }
            }
        }

        items.forEach {
            it.seenCount = kgDB.getEntriesBetween(dt.minusMonths(1), dt, it.character).size
        }
        kgDB.close()

        val sorted = items.sortedWith(
            compareByDescending<KingdomMember> { it.zerk }
                .thenByDescending { it.immunity }
                .thenByDescending { it.endTimeLeftSeconds }
                .thenBy { it.numFloors }
                .thenByDescending { it.seenCount })



        return sorted
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createKGDiscordPost(items: List<KingdomMember>) {
        var text = ""

        items.forEach {
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
            it.floors.forEach { (floor, mob) ->
                if (mob.lowercase().contains("berserk")) {
                    floors.add("$floor **ZERK ALERT**")
                } else {
                    floors.add("$floor")
                }
            }
            text += floors.joinToString()
            text += "\r\n"
        }

        Log.d(TAG, text)

        val clipboard: ClipboardManager? =
            mCtx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clip = ClipData.newPlainText("label", text)
        clipboard?.setPrimaryClip(clip)
    }
}