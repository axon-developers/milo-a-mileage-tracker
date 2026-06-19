package com.axon.milo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // Sedan, SUV, Motorcycle, Truck, Van, etc.
    val fuelType: String, // Petrol, Diesel, Electric, Hybrid, LPG
    val initialOdometer: Double
)

@Entity(tableName = "fuel_entries")
data class FuelEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vehicleId: Int,
    val pricePerLiter: Double,
    val litersFilled: Double,
    val totalCost: Double,
    val odometer: Double,
    val location: String?,
    val timestamp: Long
)
