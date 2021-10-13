package com.rockethat.ornaassistant

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject
import org.json.JSONTokener
import java.util.ArrayList

object OrnaViewFactory {
    fun create(
        type: OrnaViewType?,
        data: ArrayList<ScreenData>,
        wm: WindowManager,
        ctx: Context
    ): OrnaViewBase? {

        return when (type) {
            OrnaViewType.ITEM -> OrnaViewItem(data, wm, ctx)
            OrnaViewType.WAYVESSEL -> OrnaViewWayvessel(data, wm, ctx)
            OrnaViewType.NOTIFICATIONS -> OrnaViewNotifications(data, wm, ctx)
            OrnaViewType.DUNGEON_ENTRY -> OrnaViewDungeonEntry(data, wm, ctx)
            OrnaViewType.INVENTORY -> OrnaViewInventory(data, wm, ctx)
            OrnaViewType.KINGDOM_GAUNTLET -> OrnaViewKingdomGauntlet(data, wm, ctx)
            null -> null
        }
    }

    fun getType(data: ArrayList<ScreenData>): OrnaViewType? {
        return when {
            data.any { it.name == "ACQUIRED" } -> {
                OrnaViewType.ITEM
            }
            data.any { it.name == "New" } -> {
                OrnaViewType.INVENTORY
            }
            data.any { it.name == "Notifications" } -> {
                OrnaViewType.NOTIFICATIONS
            }
            data.any { it.name.lowercase().contains("this wayvessel is active") } -> {
                OrnaViewType.WAYVESSEL
            }
            data.any { it.name.lowercase().contains("special dungeon") } -> {
                OrnaViewType.DUNGEON_ENTRY
            }
            data.any { it.name.lowercase().contains("world dungeon") } -> {
                OrnaViewType.DUNGEON_ENTRY
            }
            data.any { it.name.lowercase().contains("losses:") } -> {
                OrnaViewType.KINGDOM_GAUNTLET
            }
            data.any { it.name.lowercase().contains("gauntlet") } -> {
                OrnaViewType.DUNGEON_ENTRY
            }
            else -> {
                null
            }
        }
    }
}

enum class OrnaViewType {
    INVENTORY, ITEM, WAYVESSEL, NOTIFICATIONS, DUNGEON_ENTRY, KINGDOM_GAUNTLET
}

enum class OrnaViewUpdateType {

    NOTIFICATIONS_INVITERS,

    DUNGEON_MODE_CHANGED, DUNGEON_ENTERED, DUNGEON_GODFORGE, DUNGEON_DONE, DUNGEON_NEW_FLOOR, DUNGEON_EXPERIENCE, DUNGEON_ORNS, DUNGEON_GOLD,

    KINGDOM_GAUNTLET_LIST

}

abstract class OrnaViewBase(val type: OrnaViewType, val wm: WindowManager, val ctx: Context) {
    var mLayout: ViewGroup? = null

    fun close() {
        if (mLayout != null) {
            wm.removeView(mLayout)
        }
    }

    open fun drawOverlay() {}
    open fun update(
        data: ArrayList<ScreenData>,
        updateResults: (MutableMap<OrnaViewUpdateType, Any?>) -> Unit
    ): Boolean {
        return false
    }
}

fun String.startsWithUppercaseLetter(): Boolean {
    return this.matches(Regex("[A-Z]{1}.*"))
}

class OrnaViewInventory(data: ArrayList<ScreenData>, wm: WindowManager, ctx: Context) :
    OrnaViewBase(OrnaViewType.INVENTORY, wm, ctx) {
}

class OrnaViewWayvessel(data: ArrayList<ScreenData>, wm: WindowManager, ctx: Context) :
    OrnaViewBase(OrnaViewType.WAYVESSEL, wm, ctx) {
}

class OrnaViewNotifications(data: ArrayList<ScreenData>, wm: WindowManager, ctx: Context) :
    OrnaViewBase(OrnaViewType.NOTIFICATIONS, wm, ctx) {

    override fun update(
        data: ArrayList<ScreenData>,
        updateResults: (MutableMap<OrnaViewUpdateType, Any?>) -> Unit
    ): Boolean {

        var inviters = mutableMapOf<String, Rect>()
        var inviter: ScreenData? = null
        for (item in data) {
            if (item.name.contains("invited you")) {
                inviter = item
            } else if (inviter != null && item.name.lowercase().contains("accept")) {
                val inviterName = inviter.name.replace(" has invited you to their party.", "")
                inviters[inviterName] = inviter.position
            }
        }

        if (inviters.isNotEmpty()) {
            updateResults(mutableMapOf(OrnaViewUpdateType.NOTIFICATIONS_INVITERS to inviters))
        }

        return false
    }
}

class OrnaViewDungeonEntry : OrnaViewBase {
    var mDungeonName = ""
    var mbEntered = false
    var mbEnteringNewDungeon = true
    var mbDone = false
    var mMode = DungeonMode()
    var mFloorNumber = 1
    var mVictoryScreenHandledForFloor = false

    constructor(
        data: ArrayList<ScreenData>,
        wm: WindowManager,
        ctx: Context
    ) : super(OrnaViewType.DUNGEON_ENTRY, wm, ctx) {
        val name = getName(data)
        if (name != null) {
            mDungeonName = name
        }

        Log.d(
            this.javaClass.toString().split(".").last(),
            this.toString()
        )
    }

    private fun getName(data: ArrayList<ScreenData>): String? {
        var name: String? = null
        var bNameNext = false
        for (item in data) {
            if (bNameNext) {
                name = item.name
                break
            } else if (item.name.lowercase().contains("world dungeon") ||
                item.name.lowercase().contains("special dungeon")
            ) {
                bNameNext = true
            }
        }

        return name
    }

    override fun update(
        data: ArrayList<ScreenData>,
        updateResults: (MutableMap<OrnaViewUpdateType, Any?>) -> Unit
    ): Boolean {
        var modeCandidate: DungeonMode? = null
        var bHardCandidate = false
        var bModeChanged = false

        var updateMap = mutableMapOf<OrnaViewUpdateType, Any?>()

        val nameFromData = getName(data)
        if (nameFromData != null && nameFromData != mDungeonName) {
            return true
        }

        var bDone = false

        if (data.any { it.name.lowercase().contains("continue floor") }) {
            mbEnteringNewDungeon = false
        } else if (data.any { it.name.lowercase().contains("hold to enter") }) {
            mbEnteringNewDungeon = true
        }

        if (data.any { it.name.lowercase().contains("godforged") }) {
            Log.d(this.javaClass.toString().split(".").last(), this.toString())
            updateMap[OrnaViewUpdateType.DUNGEON_GODFORGE] = null
        }

        if (data.any { it.name.lowercase().contains("complete") }) {
            if (!mVictoryScreenHandledForFloor && mbEntered) {
                Log.d(this.javaClass.toString().split(".").last(), this.toString() + " complete!")
                parseLoot(data, updateMap)
                mVictoryScreenHandledForFloor = true
            }
            Log.d(this.javaClass.toString().split(".").last(), this.toString())
            updateMap[OrnaViewUpdateType.DUNGEON_DONE] = null
            mbDone = true
            bDone = true
        }

        if (data.any { it.name.lowercase().contains("defeat") }) {
            Log.d(this.javaClass.toString().split(".").last(), this.toString())
            updateMap[OrnaViewUpdateType.DUNGEON_DONE] = null
            mbDone = true
            bDone = true
        }

        if (data.any { it.name.lowercase().contains("victory") }) {
            if (!mVictoryScreenHandledForFloor && mbEntered) {
                Log.d(this.javaClass.toString().split(".").last(), this.toString() + " victory!")
                parseLoot(data, updateMap)
                mVictoryScreenHandledForFloor = true
            }
        }

        val floor = data.filter { it.name.lowercase().contains("floor") }.firstOrNull()
        if (floor != null) {
            // Floor: 18 / 22
            val match = Regex("Floor:\\s([0-9]+)\\s/\\s([0-9]+)").findAll(floor.name).firstOrNull()
            if (match != null && match.groups.size == 3) {
                val number = match.groups[1]?.value?.toInt()
                val outOf = match.groups[2]?.value?.toInt()
                if (!mbEntered && (number == 1)) {
                    mbEntered = true
                    updateMap[OrnaViewUpdateType.DUNGEON_ENTERED] = null
                }
                if (number != null && number != mFloorNumber) {
                    updateMap[OrnaViewUpdateType.DUNGEON_NEW_FLOOR] = number
                    mFloorNumber = number
                    mVictoryScreenHandledForFloor = false
                }
            }
        }

        for (item in data) {
            if ((modeCandidate != null) || bHardCandidate) {
                if (item.name.contains("✓")) {
                    if (bHardCandidate) {
                        bModeChanged = !mMode.mbHard
                        mMode.mbHard = true
                    }
                    if (modeCandidate != null) {
                        bModeChanged = mMode.mMode != modeCandidate.mMode
                        mMode.mMode = modeCandidate.mMode
                    }
                } else {
                    if (bHardCandidate) {
                        bModeChanged = mMode.mbHard
                        mMode.mbHard = false
                    }
                    if (modeCandidate != null) {
                        if (modeCandidate.mMode == mMode.mMode) {
                            mMode.mMode = DungeonMode.Modes.NORMAL
                            bModeChanged = mMode.mMode != modeCandidate.mMode
                        }
                    }
                }
                bHardCandidate = false
                modeCandidate = null
            } else if (item.name.lowercase().contains("mode")) {
                when (item.name.lowercase().replace(" mode", "")) {
                    "hard" -> bHardCandidate = true
                    "boss" -> modeCandidate = DungeonMode(DungeonMode.Modes.BOSS)
                    "endless" -> modeCandidate = DungeonMode(DungeonMode.Modes.ENDLESS)
                }
            }
        }
        if (bModeChanged) {
            Log.d(
                this.javaClass.toString().split(".").last(),
                "Dungeon mode changed: $this"
            )

            updateMap[OrnaViewUpdateType.DUNGEON_MODE_CHANGED] = null
        }

        updateResults(updateMap)
        return bDone
    }

    fun parseLoot(data: ArrayList<ScreenData>, updateMap: MutableMap<OrnaViewUpdateType, Any?>) {
        // handle orns etc.
        var numberValue: Int? = null
        for (item in data) {
            if (numberValue != null) {
                when (item.name) {
                    " experience" -> updateMap[OrnaViewUpdateType.DUNGEON_EXPERIENCE] =
                        numberValue
                    " party experience" -> updateMap[OrnaViewUpdateType.DUNGEON_EXPERIENCE] =
                        numberValue
                    " gold" -> updateMap[OrnaViewUpdateType.DUNGEON_GOLD] = numberValue
                    " orns" -> updateMap[OrnaViewUpdateType.DUNGEON_ORNS] = numberValue
                }
            }
            try {
                numberValue = item.name.replace(" ", "").toInt()
            } catch (e: NumberFormatException) {
                numberValue = null
            }

        }
    }

    override fun toString(): String {
        return "${mDungeonName}: ${if (mMode.mbHard) "Hard " else ""}${mMode.mMode}, " + if (mbEnteringNewDungeon) "New" else "Continued"
    }
}

class OrnaViewItem : OrnaViewBase {
    val TAG = "OrnaViewItem"
    var itemName: String? = null
    var nameLocation: Rect? = null
    var attributes: MutableMap<String, Int> = mutableMapOf()
    var level: Int = 1

    constructor(
        data: ArrayList<ScreenData>,
        wm: WindowManager,
        ctx: Context
    ) : super(OrnaViewType.ITEM, wm, ctx) {
        val cleanedData = data
            .filter { it.name.startsWithUppercaseLetter() }
            .filterNot { it.name.startsWith("Inventory") }
            .filterNot { it.name.startsWith("Knights of Inferno") }
            .filterNot { it.name.startsWith("Earthen Legion") }
            .filterNot { it.name.startsWith("FrozenGuard") }
            .filterNot { it.name.startsWith("Party") }
            .filterNot { it.name.startsWith("Arena") }
            .filterNot { it.name.startsWith("Codex") }
            .filterNot { it.name.startsWith("Runeshop") }
            .filterNot { it.name.startsWith("Options") }
            .filterNot { it.name.startsWith("Gauntlet") }
        getName(cleanedData)
        getAttributes(cleanedData)
    }

    private fun getName(data: List<ScreenData>) {
        val qualities = listOf(
            "Broken ",
            "Poor ",
            "Superior ",
            "Famed ",
            "Legendary ",
            "Ornate ",
            "Masterforged ",
            "Demonforged ",
            "Godforged "
        )

        // https://discord.com/channels/448527960056791051/448548728861884426/870788096805969971
        /*
        Enchantment Prefixes
            Fire - burning, embered, fiery, flaming, infernal, scalding, warm
            Water - chilling, icy, oceanic, snowy, tidal, winter
            Earthen - balanced, earthly, grounded, natural, organic, rocky, stony
            Lightning - electric, shocking, sparking, stormy, thunderous
            Holy - angelic, bright, divine, moral, pure, purifying, revered, righteous, saintly, sublime
            Dark - corrupted, diabolic, demonic, gloomy, impious, profane, unhallowed, wicked
            Dragon - beastly, bestial, chimeric, dragonic, mighty, wild
            None - colorless, customary, normalized, origin, reformed, renewed, reworked
         */

        var prefixes = listOf(
            "burning",
            "embered",
            "fiery",
            "flaming",
            "infernal",
            "scalding",
            "warm",
            "chilling",
            "icy",
            "oceanic",
            "snowy",
            "tidal",
            "winter",
            "balanced",
            "earthly",
            "grounded",
            "natural",
            "organic",
            "rocky",
            "stony",
            "electric",
            "shocking",
            "sparking",
            "stormy",
            "thunderous",
            "angelic",
            "bright",
            "divine",
            "moral",
            "pure",
            "purifying",
            "revered",
            "righteous",
            "saintly",
            "sublime",
            "corrupted",
            "diabolic",
            "demonic",
            "gloomy",
            "impious",
            "profane",
            "unhallowed",
            "wicked",
            "beastly",
            "bestial",
            "chimeric",
            "dragonic",
            "mighty",
            "wild",
            "colorless",
            "customary",
            "normalized",
            "origin",
            "reformed",
            "renewed",
            "reworked"
        )
        val nameData = data.firstOrNull()
        var name = nameData?.name
        nameLocation = nameData?.position

        for (quality in qualities) {
            if (name?.startsWith(quality) == true) {
                name = name.replace(quality, "")
            }
        }

        for (prefix in prefixes) {
            if (name?.startsWith(prefix.capitalize()) == true) {
                name = name.replace(prefix.capitalize() + " ", "")
            }
        }

        itemName = name
    }

    private fun getAttributes(data: List<ScreenData>) {
        var bAdornments = false
        val acceptedAttributes = listOf("Att", "Mag", "Def", "Res", "Dex", "Crit")

        for (item in data) {
            if (item.name.contains("ADORNMENTS")) {
                bAdornments = true
            } else if (item.name.contains("Level")) {
                level = item.name.replace("Level ", "").toInt()
            } else {
                var text = item.name.replace("−", "-").replace(" ", "")
                val match = Regex("([A-Za-z\\s]+):\\s(-?[0-9]+)").findAll(text)
                match.forEach {
                    if (it.groups.size == 3) {
                        val attName = it.groups[1]?.value.toString()
                        val attVal = it.groups[2]?.value?.toInt()
                        if (acceptedAttributes.contains(attName) && attVal != null) {
                            if (!bAdornments) {
                                if (attName == "Level") {
                                    level = attVal
                                } else {
                                    attributes[attName] = attVal
                                }
                            } else {

                                var newValue = attributes[attName]
                                if (newValue != null) {
                                    newValue -= attVal
                                    attributes[attName] = newValue
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun drawOverlay() {
        assessItem()
    }

    private fun assessItem() {
        val url = "https://orna.guide/api/v1/assess"
        val start = System.currentTimeMillis()

        var attName = ""
        var attValue = 0
        attributes!!.forEach { (k, v) ->
            if (v > attValue) {
                attName = k
                attValue = v
            }
        }

        // Post parameters
        // Form fields and values
        val params = HashMap<String, Any>()
        params["name"] = itemName!!
        when (attName) {
            "HP" -> params["hp"] = attValue
            "Mana" -> params["mana"] = attValue
            "Mana" -> params["mana"] = attValue
            "Att" -> params["attack"] = attValue
            "Def" -> params["defense"] = attValue
            "Res" -> params["resistance"] = attValue
            "Dex" -> params["dexterity"] = attValue
            else -> return
        }
        params["level"] = level
        val jsonObject = JSONObject(params as Map<*, *>)
        Log.d(TAG, "Assessing item ${jsonObject}")


        // Volley post request with parameters
        Log.v(TAG, "POSTING request!")
        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            { response ->
                // Process the json

                try {
                    Log.d(TAG, "Response in ${System.currentTimeMillis() - start} ms: $response")

                    val jsonObject = JSONTokener(response.toString()).nextValue() as JSONObject
                    val quality = jsonObject.getString("quality").toFloat()
                    Log.d(TAG, "textRect: ${nameLocation!!} $quality")
                    createLayout(
                        nameLocation!!.top,
                        nameLocation!!.left,
                        nameLocation!!.right - nameLocation!!.left,
                        nameLocation!!.bottom - nameLocation!!.top,
                        "%.0f %%".format(quality * 100)
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Exception: $e")
                }

            }, {
                // Error in request
                Log.e(TAG, "Volley error: $it")
            })


        // Volley request policy, only one time request to avoid duplicate transaction
        request.retryPolicy = DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
            // 0 means no retry
            1, // DefaultRetryPolicy.DEFAULT_MAX_RETRIES = 2
            1f // DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        // Add the volley post request to the request queue
        VolleySingleton.getInstance(ctx).addToRequestQueue(request)
    }

    private fun createLayout(x_: Int, y_: Int, width_: Int, height_: Int, text: String) {
        if (mLayout != null) {
            return
        }

        Log.d(TAG, "CREATING LAYOUT")

        mLayout = LinearLayout(ctx)
        val layout = mLayout as LinearLayout
        layout.setHorizontalGravity(Gravity.CENTER_HORIZONTAL)

        //mLayout!!.setBackgroundColor(Color.GREEN and 0x55FFFFFF)
        layout.setHorizontalGravity(Gravity.CENTER_HORIZONTAL)

        val layoutParams = WindowManager.LayoutParams()
        layoutParams.apply {
            y = x_
            x = y_
            width = width_
            height = height_
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            gravity = Gravity.TOP or Gravity.LEFT
            format = PixelFormat.TRANSPARENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }

        var textView = TextView(ctx)
        textView.text = text
        textView.setTextColor(Color.WHITE)

        layout.addView(textView)

        layout.isVisible = true

        try {
            wm.addView(layout, layoutParams)
        } catch (ex: Exception) {
            Log.i(TAG, "adding view failed", ex)
        }
    }
}

class OrnaViewKingdomGauntlet : OrnaViewBase {

    constructor(
        data: ArrayList<ScreenData>,
        wm: WindowManager,
        ctx: Context
    ) : super(OrnaViewType.KINGDOM_GAUNTLET, wm, ctx) {

        Log.d(
            this.javaClass.toString().split(".").last(),
            this.toString()
        )
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun update(
        data: ArrayList<ScreenData>,
        updateResults: (MutableMap<OrnaViewUpdateType, Any?>) -> Unit
    ): Boolean {
        var bDone = false

        var firstFloor = false
        var vsAfterFloor = false
        var character = ""
        var mob = ""
        var mobDepth = 0
        var prevItemName = ""
        var updateList = mutableMapOf<String, KingdomMember>()
        var number = 0
        for (item in data) {
            if (item.name.lowercase().contains("floor:") || item.name == "+") {
                val match = Regex("FLOOR:\\s([0-9]+)").findAll(item.name).firstOrNull()
                if (match != null && match.groups.size == 2) {
                    val num = match.groups[1]?.value?.toInt()
                    if (num != null)
                    {
                        number = num
                    }
                }

                firstFloor = true
                vsAfterFloor = false
                if (character.isNotEmpty() && mobDepth < 4) {
                    var member: KingdomMember? = updateList[character]
                    if (member == null)
                    {
                        member = KingdomMember(character, mutableMapOf())
                    }
                    if (item.name == "+")
                    {
                        number++
                    }
                    member.floors[number - 1] = mob
                    updateList[character] = member
                }
            } else if (firstFloor && item.name == "VS") {
                vsAfterFloor = true
            } else if (firstFloor && item.name.lowercase().contains("level ")) {
                if (!vsAfterFloor) {
                    character = prevItemName
                } else {
                    mob = prevItemName
                    mobDepth = item.depth
                }
            }
            prevItemName = item.name
        }

        if (updateList.isNotEmpty())
        {
            var list = mutableListOf<KingdomMember>()
            updateList.forEach { (s, item) ->
                list.add(item)
            }
            val update = mutableMapOf<OrnaViewUpdateType, Any?>(OrnaViewUpdateType.KINGDOM_GAUNTLET_LIST to list)
            updateResults(update)
        }

        return bDone
    }
}
