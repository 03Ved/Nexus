package com.example.nexus.data.local

import androidx.room.TypeConverter
import com.example.nexus.domain.model.PollOption
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room TypeConverters
 * Handles conversion of complex types to/from types Room can store in SQLite
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(it, type)
        }
    }

    @TypeConverter
    fun fromPollOptionList(value: List<PollOption>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toPollOptionList(value: String?): List<PollOption>? {
        return value?.let {
            val type = object : TypeToken<List<PollOption>>() {}.type
            gson.fromJson(it, type)
        }
    }
}