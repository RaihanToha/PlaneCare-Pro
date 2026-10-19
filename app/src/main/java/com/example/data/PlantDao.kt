package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {
    @Query("SELECT * FROM plants ORDER BY createdAt DESC")
    fun getAllPlants(): Flow<List<Plant>>

    @Query("SELECT * FROM plants WHERE id = :id")
    fun getPlantById(id: Int): Flow<Plant?>

    @Query("SELECT * FROM plants ORDER BY lastAccessed DESC LIMIT 1")
    fun getLatestAccessedPlant(): Flow<Plant?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlant(plant: Plant): Long

    @Update
    suspend fun updatePlant(plant: Plant)

    @Delete
    suspend fun deletePlant(plant: Plant)

    @Query("DELETE FROM plants WHERE id = :id")
    suspend fun deletePlantById(id: Int)
    
    @Query("UPDATE plants SET lastAccessed = :timestamp WHERE id = :id")
    suspend fun updateLastAccessed(id: Int, timestamp: Long)
}
