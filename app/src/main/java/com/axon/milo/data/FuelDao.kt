package com.axon.milo.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelDao {
    // Vehicle Operations
    @Query("SELECT * FROM vehicles ORDER BY name ASC")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getVehicleById(id: Int): Vehicle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle): Long

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)

    // Fuel Entry Operations
    @Query("SELECT * FROM fuel_entries WHERE vehicleId = :vehicleId ORDER BY odometer ASC, timestamp ASC")
    fun getFuelEntriesForVehicle(vehicleId: Int): Flow<List<FuelEntry>>

    @Query("SELECT * FROM fuel_entries ORDER BY timestamp DESC")
    fun getAllFuelEntries(): Flow<List<FuelEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelEntry(entry: FuelEntry): Long

    @Delete
    suspend fun deleteFuelEntry(entry: FuelEntry)
}
