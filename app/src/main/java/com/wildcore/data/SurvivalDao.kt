package com.wildcore.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SurvivalDao {
    @Query("SELECT * FROM survival_spots ORDER BY id DESC")
    fun getAllSpots(): Flow<List<SurvivalSpot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpot(spot: SurvivalSpot)

    @Delete
    suspend fun deleteSpot(spot: SurvivalSpot)
}