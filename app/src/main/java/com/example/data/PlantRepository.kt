package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class PlantRepository(private val plantDao: PlantDao) {
    val allPlants: Flow<List<Plant>> = plantDao.getAllPlants()
    val latestAccessed: Flow<Plant?> = plantDao.getLatestAccessedPlant()

    fun getPlantById(id: Int): Flow<Plant?> = plantDao.getPlantById(id)

    suspend fun insert(plant: Plant): Long {
        return plantDao.insertPlant(plant)
    }

    suspend fun update(plant: Plant) {
        plantDao.updatePlant(plant)
    }

    suspend fun delete(plant: Plant) {
        plantDao.deletePlant(plant)
    }

    suspend fun deleteById(id: Int) {
        plantDao.deletePlantById(id)
    }

    suspend fun updateLastAccessed(id: Int, timestamp: Long) {
        plantDao.updateLastAccessed(id, timestamp)
    }

    suspend fun importBackup(context: Context, uri: android.net.Uri): Result<Int> {
        return BackupHelper.importBackup(context, uri, plantDao)
    }
}
