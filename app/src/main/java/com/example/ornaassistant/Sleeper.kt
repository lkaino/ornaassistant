package com.example.ornaassistant

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

data class Sleeper(val discordName: String, val immunity: Boolean, val endTime: LocalDateTime) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun endTimeLeft(): Long {
        val nowUTC = LocalDateTime.now(ZoneOffset.UTC)
        return -ChronoUnit.SECONDS.between(endTime, nowUTC)
    }
}
