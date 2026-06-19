package com.axon.milo.data

import kotlinx.coroutines.flow.Flow

class FuelRepository(private val fuelDao: FuelDao) {
    val allVehicles: Flow<List<Vehicle>> = fuelDao.getAllVehicles()
    val allFuelEntries: Flow<List<FuelEntry>> = fuelDao.getAllFuelEntries()

    fun getFuelEntriesForVehicle(vehicleId: Int): Flow<List<FuelEntry>> {
        return fuelDao.getFuelEntriesForVehicle(vehicleId)
    }

    suspend fun getVehicleById(id: Int): Vehicle? {
        return fuelDao.getVehicleById(id)
    }

    suspend fun insertVehicle(vehicle: Vehicle): Long {
        return fuelDao.insertVehicle(vehicle)
    }

    suspend fun deleteVehicle(vehicle: Vehicle) {
        fuelDao.deleteVehicle(vehicle)
    }

    suspend fun insertFuelEntry(entry: FuelEntry): Long {
        return fuelDao.insertFuelEntry(entry)
    }

    suspend fun deleteFuelEntry(entry: FuelEntry) {
        fuelDao.deleteFuelEntry(entry)
    }
}
