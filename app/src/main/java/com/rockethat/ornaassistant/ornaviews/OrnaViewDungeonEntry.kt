package com.rockethat.ornaassistant.ornaviews

import android.content.Context
import android.util.Log
import android.view.WindowManager
import com.rockethat.ornaassistant.*
import java.util.ArrayList

class OrnaViewDungeonEntry : OrnaView {
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
            updateMap[OrnaViewUpdateType.DUNGEON_NEW_DUNGEON] = null
            updateResults(updateMap)
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

        var defeat = false
        if (data.any { it.name.lowercase().contains("defeat") }) {
            Log.d(this.javaClass.toString().split(".").last(), this.toString())
            updateMap[OrnaViewUpdateType.DUNGEON_FAIL] = null
            mbDone = true
            bDone = true
            defeat = true
        }

        if (data.any { it.name.lowercase().contains("victory") }) {
            if (!mVictoryScreenHandledForFloor) {
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
                if ((!mbEntered && (number == 1) && !defeat) || (!mbEntered && !mbEnteringNewDungeon)) {
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