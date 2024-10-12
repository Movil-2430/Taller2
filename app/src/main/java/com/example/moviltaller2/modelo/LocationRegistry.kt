package com.example.moviltaller2.modelo

import org.json.JSONObject
import java.time.LocalDateTime

class LocationRegistry(
    val latitude: Double,
    val longitude: Double,
    val currentDateTime: LocalDateTime
) {
    private lateinit var time: String
    private lateinit var date: String

    fun toJSON(): JSONObject{
        getDateAndTime()
        val json = JSONObject()
        json.put("latitude", latitude)
        json.put("longitude", longitude)
        json.put("time", time)
        json.put("date", date)
        return json
    }

    private fun getDateAndTime(){
        time = currentDateTime.toLocalTime().toString()
        date = currentDateTime.toLocalDate().toString()
    }
}