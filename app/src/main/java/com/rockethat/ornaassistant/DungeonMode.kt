package com.rockethat.ornaassistant

class DungeonMode {

    constructor()
    {
    }

    constructor(mode : Modes)
    {
        mMode = mode
    }

    enum class Modes {
        NORMAL, BOSS, ENDLESS
    }
    var mMode: Modes = Modes.NORMAL
    var mbHard: Boolean = false

    override fun toString(): String {
        return if (mbHard) "HARD " else "" + mMode
    }

}