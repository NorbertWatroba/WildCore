package com.wildcore.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SurvivalSpot::class], version = 1, exportSchema = false)
abstract class WildCoreDatabase : RoomDatabase() {
    abstract val survivalDao: SurvivalDao
}