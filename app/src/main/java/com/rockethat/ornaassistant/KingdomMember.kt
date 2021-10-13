package com.rockethat.ornaassistant

data class KingdomMember(val character: String, var floors: MutableMap<Int,String>)
{
    var immunity: Boolean = false
    var endTimeLeftSeconds: Long = 0
    var discordName = ""
    var seenCount = 0
    val numFloors
        get() = floors.size
    val zerk
        get() = floors.any { it-> it.value.lowercase().contains("berserk") }
}

