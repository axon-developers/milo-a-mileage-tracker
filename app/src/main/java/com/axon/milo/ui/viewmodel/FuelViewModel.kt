package com.axon.milo.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.axon.milo.data.FuelDatabase
import com.axon.milo.data.FuelEntry
import com.axon.milo.data.FuelRepository
import com.axon.milo.data.Vehicle
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class FuelViewModel(
    application: Application,
    private val repository: FuelRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("milo_prefs", android.content.Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "System") ?: "System")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _showMonthlySpend = MutableStateFlow(prefs.getBoolean("show_monthly_spend", true))
    val showMonthlySpend: StateFlow<Boolean> = _showMonthlySpend.asStateFlow()

    private val _show6MonthGraph = MutableStateFlow(prefs.getBoolean("show_6_month_graph", true))
    val show6MonthGraph: StateFlow<Boolean> = _show6MonthGraph.asStateFlow()

    private val _currencySymbol = MutableStateFlow(prefs.getString("currency_symbol", "$") ?: "$")
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    private val _distanceUnit = MutableStateFlow(prefs.getString("distance_unit", "km") ?: "km")
    val distanceUnit: StateFlow<String> = _distanceUnit.asStateFlow()

    private val _volumeUnit = MutableStateFlow(prefs.getString("volume_unit", "L") ?: "L")
    val volumeUnit: StateFlow<String> = _volumeUnit.asStateFlow()

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    fun setShowMonthlySpend(show: Boolean) {
        prefs.edit().putBoolean("show_monthly_spend", show).apply()
        _showMonthlySpend.value = show
    }

    fun setShow6MonthGraph(show: Boolean) {
        prefs.edit().putBoolean("show_6_month_graph", show).apply()
        _show6MonthGraph.value = show
    }

    fun setCurrencySymbol(symbol: String) {
        prefs.edit().putString("currency_symbol", symbol).apply()
        _currencySymbol.value = symbol
    }

    fun setDistanceUnit(unit: String) {
        prefs.edit().putString("distance_unit", unit).apply()
        _distanceUnit.value = unit
    }

    fun setVolumeUnit(unit: String) {
        prefs.edit().putString("volume_unit", unit).apply()
        _volumeUnit.value = unit
    }

    // All vehicles
    val vehicles: StateFlow<List<Vehicle>> = repository.allVehicles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // All fuel entries
    val allFuelEntries: StateFlow<List<FuelEntry>> = repository.allFuelEntries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Derived State: Calculations for mileage per vehicle and spend
    val dashboardState: StateFlow<DashboardState> = combine(vehicles, allFuelEntries) { vehicleList, entryList ->
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfCurrentMonthMs = calendar.timeInMillis

        // Spend current month across all vehicles
        val currentMonthSpend = entryList
            .filter { it.timestamp >= startOfCurrentMonthMs }
            .sumOf { it.totalCost }

        // Mileage calculation for each vehicle
        // Map of vehicleId -> Latest Calculated Mileage
        val latestMileageMap = mutableMapOf<Int, Double?>()
        val entriesCountMap = mutableMapOf<Int, Int>()

        for (vehicle in vehicleList) {
            // Get entries for this vehicle ordered by odometer ascending
            val vehicleEntries = entryList
                .filter { it.vehicleId == vehicle.id }
                .sortedBy { it.odometer }

            entriesCountMap[vehicle.id] = vehicleEntries.size

            if (vehicleEntries.isNotEmpty()) {
                // Latest entry is the last in the odometer-sorted list
                val latestEntry = vehicleEntries.last()
                val prevOdometer = if (vehicleEntries.size > 1) {
                    vehicleEntries[vehicleEntries.size - 2].odometer
                } else {
                    vehicle.initialOdometer
                }

                val diffOdo = latestEntry.odometer - prevOdometer
                val mileage = if (diffOdo > 0 && latestEntry.litersFilled > 0) {
                    diffOdo / latestEntry.litersFilled
                } else {
                    null
                }
                latestMileageMap[vehicle.id] = mileage
            } else {
                latestMileageMap[vehicle.id] = null
            }
        }

        DashboardState(
            totalSpendCurrentMonth = currentMonthSpend,
            vehicleLatestMileage = latestMileageMap,
            vehicleEntriesCount = entriesCountMap
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )

    // Helper functions to log records
    fun addVehicle(name: String, type: String, fuelType: String, initialOdometer: Double) {
        viewModelScope.launch {
            repository.insertVehicle(
                Vehicle(
                    name = name,
                    type = type,
                    fuelType = fuelType,
                    initialOdometer = initialOdometer
                )
            )
        }
    }

    fun updateVehicle(id: Int, name: String, type: String, fuelType: String, initialOdometer: Double) {
        viewModelScope.launch {
            repository.insertVehicle(
                Vehicle(
                    id = id,
                    name = name,
                    type = type,
                    fuelType = fuelType,
                    initialOdometer = initialOdometer
                )
            )
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicle)
        }
    }

    fun addFuelEntry(
        vehicleId: Int,
        pricePerLiter: Double,
        litersFilled: Double,
        totalCost: Double,
        odometer: Double,
        location: String?,
        timestamp: Long
    ) {
        viewModelScope.launch {
            repository.insertFuelEntry(
                FuelEntry(
                    vehicleId = vehicleId,
                    pricePerLiter = pricePerLiter,
                    litersFilled = litersFilled,
                    totalCost = totalCost,
                    odometer = odometer,
                    location = location,
                    timestamp = timestamp
                )
            )
        }
    }

    fun updateFuelEntry(
        id: Int,
        vehicleId: Int,
        pricePerLiter: Double,
        litersFilled: Double,
        totalCost: Double,
        odometer: Double,
        location: String?,
        timestamp: Long
    ) {
        viewModelScope.launch {
            repository.insertFuelEntry(
                FuelEntry(
                    id = id,
                    vehicleId = vehicleId,
                    pricePerLiter = pricePerLiter,
                    litersFilled = litersFilled,
                    totalCost = totalCost,
                    odometer = odometer,
                    location = location,
                    timestamp = timestamp
                )
            )
        }
    }

    fun deleteFuelEntry(entry: FuelEntry) {
        viewModelScope.launch {
            repository.deleteFuelEntry(entry)
        }
    }

    // Helper function to get full history of entries with calculated mileage for a vehicle
    fun getFullEntriesWithMileageForVehicle(
        vehicle: Vehicle,
        entries: List<FuelEntry>
    ): List<FuelEntryWithMileage> {
        val vehicleEntries = entries
            .filter { it.vehicleId == vehicle.id }
            .sortedBy { it.odometer }

        return vehicleEntries.mapIndexed { index, entry ->
            val prevOdo = if (index > 0) {
                vehicleEntries[index - 1].odometer
            } else {
                vehicle.initialOdometer
            }
            val diff = entry.odometer - prevOdo
            val mileage = if (diff > 0 && entry.litersFilled > 0) {
                diff / entry.litersFilled
            } else {
                null
            }
            FuelEntryWithMileage(entry, mileage)
        }.sortedByDescending { it.entry.odometer } // Sorted descending for history presentation
    }
}

data class DashboardState(
    val totalSpendCurrentMonth: Double = 0.0,
    val vehicleLatestMileage: Map<Int, Double?> = emptyMap(),
    val vehicleEntriesCount: Map<Int, Int> = emptyMap()
)

data class FuelEntryWithMileage(
    val entry: FuelEntry,
    val mileage: Double?
)

// ViewModel Manufacturing Factory
class FuelViewModelFactory(
    private val application: Application,
    private val repository: FuelRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FuelViewModel::class.java)) {
            return FuelViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
