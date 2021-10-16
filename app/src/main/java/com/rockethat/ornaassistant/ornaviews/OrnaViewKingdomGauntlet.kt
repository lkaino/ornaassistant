package com.rockethat.ornaassistant.ornaviews

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.rockethat.ornaassistant.*
import java.util.ArrayList

class OrnaViewKingdomGauntlet : OrnaView {

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
        var prevItemDepth = 0
        var characterDepth = 0
        var updateList = mutableMapOf<String, KingdomMember>()
        var number = 0

        var highestDepth = 0

        for (item in data) {
            if (item.depth > highestDepth) {
                highestDepth = item.depth
            }
        }

        for (item in data) {
            if (item.name.lowercase().contains("floor:") || item.name == "+") {
                val match = Regex("FLOOR:\\s([0-9]+)").findAll(item.name).firstOrNull()
                if (match != null && match.groups.size == 2) {
                    val num = match.groups[1]?.value?.toInt()
                    if (num != null) {
                        number = num
                    }
                }

                firstFloor = true
                vsAfterFloor = false
                if (character.isNotEmpty()) {
                    var member: KingdomMember? = updateList[character]
                    if (member == null) {
                        member = KingdomMember(character, mutableMapOf())
                    }

                    var win = false
                    if (mobDepth >= highestDepth) {
                        win = true
                    }

                    var loss = false
                    if (characterDepth >= highestDepth) {
                        loss = true
                    }

                    if (item.name == "+") {
                        number++
                    }

                    member.floors[number - 1] = KingdomGauntletFloor(number - 1, mob, loss, win)
                    updateList[character] = member
                }
            } else if (firstFloor && item.name == "VS") {
                vsAfterFloor = true
            } else if (firstFloor && item.name.lowercase().contains("level ")) {
                if (!vsAfterFloor) {
                    character = prevItemName
                    characterDepth = prevItemDepth
                } else {
                    mob = prevItemName
                    mobDepth = item.depth
                }
            }
            prevItemName = item.name
            prevItemDepth = item.depth
        }

        if (updateList.isNotEmpty()) {
            var list = mutableListOf<KingdomMember>()
            updateList.forEach { (s, item) ->
                list.add(item)
            }
            val update =
                mutableMapOf<OrnaViewUpdateType, Any?>(OrnaViewUpdateType.KINGDOM_GAUNTLET_LIST to list)
            updateResults(update)
        }

        return bDone
    }
}