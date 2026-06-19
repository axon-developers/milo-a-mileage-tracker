package com.axon.milo.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.axon.milo.data.FuelEntry
import com.axon.milo.data.Vehicle
import com.axon.milo.ui.viewmodel.FuelEntryWithMileage
import com.axon.milo.ui.viewmodel.FuelViewModel
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    viewModel: FuelViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vehicles by viewModel.vehicles.collectAsState()
    val allEntries by viewModel.allFuelEntries.collectAsState()
    val dashboardState by viewModel.dashboardState.collectAsState()

    val showMonthlySpend by viewModel.showMonthlySpend.collectAsState()
    val show6MonthGraph by viewModel.show6MonthGraph.collectAsState()

    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val distanceUnit by viewModel.distanceUnit.collectAsState()
    val volumeUnit by viewModel.volumeUnit.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var showEditVehicleDialog by remember { mutableStateOf<Vehicle?>(null) }
    var showEditFuelEntryDialog by remember { mutableStateOf<FuelEntry?>(null) }
    var showAddFuelEntryDialog by remember { mutableStateOf(false) }
    var selectedVehicleForHistory by remember { mutableStateOf<Vehicle?>(null) }

    val tabs = listOf(
        TabItem("Dashboard", Icons.Default.Dashboard),
        TabItem("All Logs", Icons.Default.LocalGasStation),
        TabItem("Analytics", Icons.Default.BarChart),
        TabItem("Settings", Icons.Default.Settings)
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Milo",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 1.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    if (selectedTabIndex == 0 && selectedVehicleForHistory == null) {
                        IconButton(
                            onClick = { showAddVehicleDialog = true },
                            modifier = Modifier.testTag("add_vehicle_toolbar_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Vehicle"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.navigationBarsPadding()
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTabIndex == index && selectedVehicleForHistory == null,
                        onClick = {
                            selectedVehicleForHistory = null
                            selectedTabIndex = index
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        alwaysShowLabel = true,
                        modifier = Modifier.testTag("nav_tab_${tab.title.lowercase().replace(" ", "_").replace("-", "_")}")
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTabIndex in listOf(0, 1) && selectedVehicleForHistory == null) {
                FloatingActionButton(
                    onClick = { showAddFuelEntryDialog = true },
                    modifier = Modifier.testTag("dashboard_add_log_fab"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Log Fuel Fill-Up"
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = if (selectedVehicleForHistory != null) -1 else selectedTabIndex,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ScreenTransitions"
            ) { state ->
                when (state) {
                    -1 -> {
                        selectedVehicleForHistory?.let { vehicle ->
                            VehicleHistoryScreen(
                                vehicle = vehicle,
                                entries = allEntries,
                                viewModel = viewModel,
                                currencySymbol = currencySymbol,
                                distanceUnit = distanceUnit,
                                volumeUnit = volumeUnit,
                                onBack = { selectedVehicleForHistory = null },
                                onEditVehicle = { showEditVehicleDialog = it },
                                onDeleteVehicle = {
                                    viewModel.deleteVehicle(it)
                                    selectedVehicleForHistory = null
                                },
                                onEditFuelEntry = { showEditFuelEntryDialog = it }
                            )
                        }
                    }
                    0 -> {
                        DashboardScreen(
                            vehicles = vehicles,
                            entries = allEntries,
                            dashboardState = dashboardState,
                            showMonthlySpend = showMonthlySpend,
                            show6MonthGraph = show6MonthGraph,
                            currencySymbol = currencySymbol,
                            distanceUnit = distanceUnit,
                            volumeUnit = volumeUnit,
                            onAddVehicleClick = { showAddVehicleDialog = true },
                            onVehicleClick = { selectedVehicleForHistory = it },
                            onDeleteVehicle = { viewModel.deleteVehicle(it) },
                            onAddFuelEntryClick = { showAddFuelEntryDialog = true }
                        )
                    }
                    1 -> {
                        LogFuelScreen(
                            vehicles = vehicles,
                            entries = allEntries,
                            viewModel = viewModel,
                            currencySymbol = currencySymbol,
                            distanceUnit = distanceUnit,
                            volumeUnit = volumeUnit,
                            onAddFuelEntryClick = { showAddFuelEntryDialog = true },
                            onEditFuelEntry = { showEditFuelEntryDialog = it },
                            onAddVehicleClick = { showAddVehicleDialog = true }
                        )
                    }
                    2 -> {
                        AnalyticsScreen(
                            vehicles = vehicles,
                            entries = allEntries,
                            dashboardState = dashboardState
                        )
                    }
                    3 -> {
                        SettingsScreen(
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

        if (showAddVehicleDialog) {
            AddVehicleDialog(
                onDismiss = { showAddVehicleDialog = false },
                onConfirm = { name, type, fuelType, initialOdo ->
                    viewModel.addVehicle(name, type, fuelType, initialOdo)
                    showAddVehicleDialog = false
                }
            )
        }

        if (showEditVehicleDialog != null) {
            AddVehicleDialog(
                vehicleToEdit = showEditVehicleDialog,
                onDismiss = { showEditVehicleDialog = null },
                onConfirm = { name, type, fuelType, initialOdo ->
                    showEditVehicleDialog?.let { originalVehicle ->
                        viewModel.updateVehicle(originalVehicle.id, name, type, fuelType, initialOdo)
                    }
                    showEditVehicleDialog = null
                }
            )
        }

        if (showEditFuelEntryDialog != null) {
            EditFuelEntryDialog(
                entry = showEditFuelEntryDialog!!,
                currencySymbol = currencySymbol,
                distanceUnit = distanceUnit,
                volumeUnit = volumeUnit,
                onDismiss = { showEditFuelEntryDialog = null },
                onConfirm = { price, liters, cost, odo, loc ->
                    showEditFuelEntryDialog?.let { original ->
                        viewModel.updateFuelEntry(
                            id = original.id,
                            vehicleId = original.vehicleId,
                            pricePerLiter = price,
                            litersFilled = liters,
                            totalCost = cost,
                            odometer = odo,
                            location = loc,
                            timestamp = original.timestamp
                        )
                    }
                    showEditFuelEntryDialog = null
                }
            )
        }

        if (showAddFuelEntryDialog) {
            AddFuelEntryDialog(
                vehicles = vehicles,
                currencySymbol = currencySymbol,
                distanceUnit = distanceUnit,
                volumeUnit = volumeUnit,
                onDismiss = { showAddFuelEntryDialog = false },
                onConfirm = { vehicleId, price, liters, cost, odo, loc ->
                    viewModel.addFuelEntry(
                        vehicleId = vehicleId,
                        pricePerLiter = price,
                        litersFilled = liters,
                        totalCost = cost,
                        odometer = odo,
                        location = loc,
                        timestamp = java.lang.System.currentTimeMillis()
                    )
                    showAddFuelEntryDialog = false
                }
            )
        }
    }
}

data class TabItem(val title: String, val icon: ImageVector)

// ================= TAB 0: ALL LOGS HISTORY SCREEN =================

data class DetailedAllLogEntry(
    val entry: FuelEntry,
    val mileage: Double?,
    val vehicle: Vehicle?
)

@Composable
fun LogFuelScreen(
    vehicles: List<Vehicle>,
    entries: List<FuelEntry>,
    viewModel: FuelViewModel,
    currencySymbol: String,
    distanceUnit: String,
    volumeUnit: String,
    onAddFuelEntryClick: () -> Unit,
    onEditFuelEntry: (FuelEntry) -> Unit,
    onAddVehicleClick: () -> Unit
) {
    val listState = rememberLazyListState()

    val detailedAllEntries = remember(entries, vehicles) {
        vehicles.flatMap { vehicle ->
            viewModel.getFullEntriesWithMileageForVehicle(vehicle, entries).map { hw ->
                DetailedAllLogEntry(hw.entry, hw.mileage, vehicle)
            }
        }.sortedByDescending { it.entry.timestamp }
    }

    var entriesLimit by remember { mutableIntStateOf(10) }
    val visibleEntries = remember(detailedAllEntries, entriesLimit) {
        detailedAllEntries.take(entriesLimit)
    }

    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            if (lastVisibleItem == null) {
                false
            } else {
                lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 2
            }
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && entriesLimit < detailedAllEntries.size) {
            entriesLimit += 10
        }
    }

    val efficiencyLabel = remember(distanceUnit, volumeUnit) {
        if (distanceUnit == "mi" && volumeUnit == "gal") "MPG" else "$distanceUnit/$volumeUnit"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (vehicles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Vehicles Registered",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Register a vehicle profile to view and log fuel fill-ups.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onAddVehicleClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("empty_logs_add_vehicle_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Vehicle Profile", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else if (detailedAllEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalGasStation,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Fuel Logs Yet",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your collective fuel log history is empty. Press the floating '+' button to log your first gas fill-up!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onAddFuelEntryClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("empty_logs_add_log_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Fuel Fill-Up", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "All Fuel Logs (${detailedAllEntries.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(visibleEntries, key = { it.entry.id }) { detailed ->
                    val entry = detailed.entry
                    val mileage = detailed.mileage
                    val vehicle = detailed.vehicle
                    val dateFormatted = remember(entry.timestamp) {
                        SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(java.util.Date(entry.timestamp))
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("all_logs_entry_item_${entry.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Top Headline Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getVehicleIcon(vehicle?.type),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = vehicle?.name ?: "Unknown Vehicle",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = dateFormatted,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = { onEditFuelEntry(entry) },
                                        modifier = Modifier.testTag("all_logs_edit_entry_${entry.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit entry",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteFuelEntry(entry) },
                                        modifier = Modifier.testTag("all_logs_delete_entry_${entry.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HighlightOff,
                                            contentDescription = "Delete entry",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            if (entry.location != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Location",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = entry.location,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            // Numerical Details Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Odometer",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${entry.odometer.toInt()} $distanceUnit",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Volume",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.2f", entry.litersFilled)} $volumeUnit",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Cost",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${currencySymbol}${String.format(Locale.US, "%.2f", entry.totalCost)}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            // Fuel Efficiency Badge/Highlights
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(vertical = 8.dp, horizontal = 12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Speed,
                                            contentDescription = "Mileage",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Fuel Efficiency",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Text(
                                        text = if (mileage != null) {
                                            "${String.format(Locale.US, "%.2f", mileage)} $efficiencyLabel"
                                        } else {
                                            "N/A (First log of vehicle)"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (mileage != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (detailedAllEntries.size > entriesLimit) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { entriesLimit += 10 },
                                modifier = Modifier.testTag("all_logs_load_more_button")
                            ) {
                                Text("Load More Logs")
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun triggerGPSLocationFetch(
    context: Context,
    onResult: (String) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    val coords = String.format(Locale.US, "%.5f, %.5f", loc.latitude, loc.longitude)
                    onResult(coords)
                } else {
                    onError("No cached GPS location. Turn on Location Services.")
                }
            }
            .addOnFailureListener {
                onError(it.localizedMessage ?: "Failed to acquire GPS location")
            }
    } catch (e: Exception) {
        onError("Location error: ${e.message}")
    }
}

fun getVehicleIcon(type: String?): ImageVector {
    return when (type?.lowercase(Locale.ROOT)) {
        "sedan", "car" -> Icons.Default.DirectionsCar
        "suv" -> Icons.Default.AirportShuttle
        "motorcycle", "bike" -> Icons.Default.TwoWheeler
        "truck" -> Icons.Default.LocalShipping
        "van" -> Icons.Default.DirectionsBus
        else -> Icons.Default.DirectionsCar
    }
}

// ================= TAB 1: INTEGRATED DASHBOARD & FLEET SCREEN =================

@Composable
fun DashboardScreen(
    vehicles: List<Vehicle>,
    entries: List<FuelEntry>,
    dashboardState: com.axon.milo.ui.viewmodel.DashboardState,
    showMonthlySpend: Boolean,
    show6MonthGraph: Boolean,
    currencySymbol: String,
    distanceUnit: String,
    volumeUnit: String,
    onAddVehicleClick: () -> Unit,
    onVehicleClick: (Vehicle) -> Unit,
    onDeleteVehicle: (Vehicle) -> Unit,
    onAddFuelEntryClick: () -> Unit
) {
    val recentMonthsData = remember(entries) {
        getRecentMonthsSpendData(entries)
    }

    // Previous month comparison
    val calendar = Calendar.getInstance()
    val startOfCurrentMonthMs = calendar.apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val previousMonthCalStart = Calendar.getInstance().apply {
         add(Calendar.MONTH, -1)
         set(Calendar.DAY_OF_MONTH, 1)
         set(Calendar.HOUR_OF_DAY, 0)
         set(Calendar.MINUTE, 0)
         set(Calendar.SECOND, 0)
         set(Calendar.MILLISECOND, 0)
    }
    val previousMonthCalEnd = Calendar.getInstance().apply {
         add(Calendar.MONTH, -1)
         set(Calendar.DAY_OF_MONTH, previousMonthCalStart.getActualMaximum(Calendar.DAY_OF_MONTH))
         set(Calendar.HOUR_OF_DAY, 23)
         set(Calendar.MINUTE, 59)
         set(Calendar.SECOND, 59)
         set(Calendar.MILLISECOND, 999)
    }
    val startOfPrevMonthMs = previousMonthCalStart.timeInMillis
    val endOfPrevMonthMs = previousMonthCalEnd.timeInMillis

    val currentMonthSpend = dashboardState.totalSpendCurrentMonth
    val prevMonthSpend = entries
         .filter { it.timestamp in startOfPrevMonthMs..endOfPrevMonthMs }
         .sumOf { it.totalCost }

    val comparisonText = remember(currentMonthSpend, prevMonthSpend, currencySymbol) {
        if (prevMonthSpend > 0) {
            val ratio = (currentMonthSpend - prevMonthSpend) / prevMonthSpend
            val percent = String.format(Locale.US, "%.1f", abs(ratio) * 100.0)
            if (ratio > 0) {
                "📈 $percent% higher than previous month (${currencySymbol}${String.format(Locale.US, "%.2f", prevMonthSpend)})"
            } else {
                "📉 $percent% lower than previous month (${currencySymbol}${String.format(Locale.US, "%.2f", prevMonthSpend)})"
            }
        } else if (currentMonthSpend > 0) {
            "⚡ First month of fuel spend logs"
        } else {
            "No logs registered yet"
        }
    }

    val efficiencyLabel = remember(distanceUnit, volumeUnit) {
        if (distanceUnit == "mi" && volumeUnit == "gal") "MPG" else "$distanceUnit/$volumeUnit"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Budget Highlights Card (Conditional)
        if (showMonthlySpend) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "MONTHLY SPEND OVERVIEW",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${currencySymbol}${String.format(Locale.US, "%.2f", currentMonthSpend)}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = comparisonText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // 6 Month Fuel Spend Bar Graph (Conditional)
        if (show6MonthGraph) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "6-Month Fuel Consumption (${currencySymbol})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Aggregated monthly spend across all active vehicles",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (recentMonthsData.all { it.amount == 0.0 }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No transaction spent records logged yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            MonthlySpendBarGraph(data = recentMonthsData)
                        }
                    }
                }
            }
        }

        // Vehicles List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Vehicle Profiles",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Registered fleet & mileage performance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = onAddVehicleClick,
                    modifier = Modifier.testTag("add_vehicle_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Vehicle")
                }
            }
        }

        if (vehicles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "You do not have any registered vehicles yet. Touch 'Add Vehicle' above to get started.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(vehicles, key = { it.id }) { vehicle ->
                val mileage = dashboardState.vehicleLatestMileage[vehicle.id]
                val entriesCount = dashboardState.vehicleEntriesCount[vehicle.id] ?: 0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onVehicleClick(vehicle) }
                        .testTag("vehicle_item_${vehicle.id}"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getVehicleIcon(vehicle.type),
                                    contentDescription = "Vehicle",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = vehicle.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(vehicle.type) },
                                        modifier = Modifier.height(24.dp)
                                    )
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(vehicle.fuelType) },
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }
                        }

                        // Mileage stats
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "LATEST MILEAGE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            if (mileage != null) {
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", mileage)} $efficiencyLabel",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = if (entriesCount > 0) "Calculating..." else "No log yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$entriesCount entries",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
}
 // ================= COGNITIVE HISTORY DETAILS LIST SCREEN =================

@Composable
fun VehicleHistoryScreen(
    vehicle: Vehicle,
    entries: List<FuelEntry>,
    viewModel: FuelViewModel,
    currencySymbol: String,
    distanceUnit: String,
    volumeUnit: String,
    onBack: () -> Unit,
    onEditVehicle: (Vehicle) -> Unit,
    onDeleteVehicle: (Vehicle) -> Unit,
    onEditFuelEntry: (FuelEntry) -> Unit
) {
    val detailedEntries = remember(entries) {
        viewModel.getFullEntriesWithMileageForVehicle(vehicle, entries)
    }

    var entriesLimit by remember { mutableIntStateOf(10) }
    val visibleEntries = remember(detailedEntries, entriesLimit) {
        detailedEntries.take(entriesLimit)
    }

    // Graph is for the last 10 log entries.
    // detailedEntries is sorted descending (newest first). Let's take the first 10,
    // then reverse them so odometer/chronology rises from left-to-right (ascending).
    val graphEntries = remember(detailedEntries) {
        detailedEntries.take(10).reversed()
    }
    val graphMileages = remember(graphEntries) {
        graphEntries.mapNotNull { it.mileage }
    }

    val efficiencyLabel = remember(distanceUnit, volumeUnit) {
        if (distanceUnit == "mi" && volumeUnit == "gal") "MPG" else "$distanceUnit/$volumeUnit"
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    .statusBarsPadding()
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vehicle.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Fuel Logs & Efficiency History",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { onEditVehicle(vehicle) },
                    modifier = Modifier.testTag("edit_vehicle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Vehicle",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = { onDeleteVehicle(vehicle) },
                    modifier = Modifier.testTag("delete_vehicle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Vehicle",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    ) { inPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // General Details Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getVehicleIcon(vehicle.type),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${vehicle.type} • ${vehicle.fuelType}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Started odometer: ${vehicle.initialOdometer.toInt()} $distanceUnit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Mileage Trend Graph - Only plotted with the last 10 entries
            if (graphMileages.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Mileage Efficiency Trend (Last 10 Logs)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Calculated $efficiencyLabel for newest logs (chronological)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            MileageTrendLineGraph(history = graphMileages)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Transaction History (${detailedEntries.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (detailedEntries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.LocalGasStation,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No fuel logs logged yet for this vehicle",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(visibleEntries, key = { it.entry.id }) { detailed ->
                    val entry = detailed.entry
                    val mileage = detailed.mileage
                    val dateFormatted = remember(entry.timestamp) {
                        SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(Date(entry.timestamp))
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("entry_item_${entry.id}"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Top Headline Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = dateFormatted,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (entry.location != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = "Location",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = entry.location,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = { onEditFuelEntry(entry) },
                                        modifier = Modifier.testTag("edit_entry_${entry.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit entry",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteFuelEntry(entry) },
                                        modifier = Modifier.testTag("delete_entry_${entry.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HighlightOff,
                                            contentDescription = "Delete entry",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

                            // Numerical Details Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Odometer",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${entry.odometer.toInt()} $distanceUnit",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Volume",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.2f", entry.litersFilled)} $volumeUnit",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Cost",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${currencySymbol}${String.format(Locale.US, "%.2f", entry.totalCost)}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            // Efficiency Card Highlight
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Speed,
                                            contentDescription = "Calculated Mileage",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Calculated Efficiency",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Text(
                                        text = if (mileage != null) "${String.format(Locale.US, "%.2f", mileage)} $efficiencyLabel" else "N/A (First log / values)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (mileage != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Load More button when required
                if (detailedEntries.size > entriesLimit) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { entriesLimit += 10 },
                                modifier = Modifier.testTag("load_more_entries_button")
                            ) {
                                Text("Load More Entries")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================= DIALOGS =================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleDialog(
    vehicleToEdit: Vehicle? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf(vehicleToEdit?.name ?: "") }
    var selectedType by remember { mutableStateOf(vehicleToEdit?.type ?: "Sedan") }
    var selectedFuelType by remember { mutableStateOf(vehicleToEdit?.fuelType ?: "Petrol") }
    var initialOdo by remember { mutableStateOf(vehicleToEdit?.initialOdometer?.toInt()?.toString() ?: "") }

    val vehicleTypes = listOf("Sedan", "SUV", "Motorcycle", "Truck", "Van")
    val fuelTypes = listOf("Petrol", "Diesel", "Electric", "Hybrid", "LPG")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (vehicleToEdit != null) "Edit Vehicle Details" else "Register New Vehicle",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Vehicle Name") },
                    placeholder = { Text("e.g., My Ford Fusion") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vehicle_name_input")
                )

                // Vehicle Type Dropdowns / Options
                Column {
                    Text(
                        "Vehicle Type",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    var expandedType by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { expandedType = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedType)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = expandedType,
                            onDismissRequest = { expandedType = false },
                            modifier = Modifier.fillMaxWidth(0.65f)
                        ) {
                            vehicleTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        selectedType = type
                                        expandedType = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Fuel Type Dropdowns / Options
                Column {
                    Text(
                        "Fuel Type",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    var expandedFuelType by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { expandedFuelType = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedFuelType)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = expandedFuelType,
                            onDismissRequest = { expandedFuelType = false },
                            modifier = Modifier.fillMaxWidth(0.65f)
                        ) {
                            fuelTypes.forEach { fuelType ->
                                DropdownMenuItem(
                                    text = { Text(fuelType) },
                                    onClick = {
                                        selectedFuelType = fuelType
                                        expandedFuelType = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = initialOdo,
                    onValueChange = { initialOdo = it },
                    label = { Text("Initial Odometer (km)") },
                    placeholder = { Text("e.g., 25000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("initial_odo_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val odo = initialOdo.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && odo >= 0) {
                        onConfirm(name, selectedType, selectedFuelType, odo)
                    }
                },
                enabled = name.isNotBlank() && initialOdo.isNotBlank(),
                modifier = Modifier.testTag("save_vehicle_button")
            ) {
                Text(if (vehicleToEdit != null) "Save" else "Register")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ================= TAB 2: ANALYTICS & CUSTOM GRAPHS SCREEN =================

@Composable
fun AnalyticsScreen(
    vehicles: List<Vehicle>,
    entries: List<FuelEntry>,
    dashboardState: com.axon.milo.ui.viewmodel.DashboardState
) {
    var selectedVehicleForChart by remember { mutableStateOf<Vehicle?>(null) }
    var isChartDropdownExpanded by remember { mutableStateOf(false) }

    val recentMonthsData = remember(entries) {
        getRecentMonthsSpendData(entries)
    }

    LaunchedEffect(vehicles) {
        if (selectedVehicleForChart == null && vehicles.isNotEmpty()) {
            selectedVehicleForChart = vehicles.first()
        }
    }

    val vehicleMileageHistory = remember(selectedVehicleForChart, entries) {
        selectedVehicleForChart?.let { vehicle ->
            val vehicleEntries = entries
                .filter { it.vehicleId == vehicle.id }
                .sortedBy { it.odometer }

            vehicleEntries.mapIndexed { index, entry ->
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
                mileage ?: 0.0
            }.filter { it > 0.0 }
        } ?: emptyList()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Fuel Analytics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Performance dashboard of fuel expenses and efficiency graphs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Spend Statistics Chart Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Monthly Fuel Consumption ($)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Last 6 calendar months aggregate spend",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (recentMonthsData.all { it.amount == 0.0 }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No transaction spent records logged in the past 6 months to visualize.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        MonthlySpendBarGraph(data = recentMonthsData)
                    }
                }
            }
        }

        // Mileage Efficiency Trend Line Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Vehicle Mileage Efficiency",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Efficiency trends (km/L) across logged entries",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (vehicles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No vehicles registered to analyze metrics.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        // Dropdown to select vehicle
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                onClick = { isChartDropdownExpanded = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = getVehicleIcon(selectedVehicleForChart?.type),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = selectedVehicleForChart?.name ?: "Select Vehicle",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = isChartDropdownExpanded,
                                onDismissRequest = { isChartDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                vehicles.forEach { vehicle ->
                                    DropdownMenuItem(
                                        text = { Text(vehicle.name) },
                                        onClick = {
                                            selectedVehicleForChart = vehicle
                                            isChartDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (vehicleMileageHistory.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Log at least one entry above vehicle's initial mileage to see efficiency statistics.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            MileageTrendLineGraph(history = vehicleMileageHistory)
                        }
                    }
                }
            }
        }
    }
}

// ---------------- CUSTOM BAR GRAPH DRAWING ----------------

@Composable
fun MonthlySpendBarGraph(data: List<MonthSpendData>) {
    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.surfaceVariant
    val maxAmt = remember(data) { data.maxOfOrNull { it.amount }?.coerceAtLeast(10.0) ?: 10.0 }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val totalWidth = size.width
        val totalHeight = size.height

        val labelHeight = 24.dp.toPx()
        val graphHeight = totalHeight - labelHeight

        val numBars = data.size
        val barSpacing = totalWidth / (numBars * 3)
        val barWidth = (totalWidth - (barSpacing * (numBars + 1))) / numBars

        data.forEachIndexed { index, monthData ->
            val amount = monthData.amount
            val normalizedHeight = (amount / maxAmt) * graphHeight

            val startX = barSpacing + index * (barWidth + barSpacing)
            val startY = graphHeight - normalizedHeight

            val isCurrentMonth = index == data.size - 1
            val currentBarColor = if (isCurrentMonth) activeColor else inactiveColor

            // Draw rounded bar
            drawRoundRect(
                color = currentBarColor,
                topLeft = Offset(startX.toFloat(), startY.toFloat()),
                size = androidx.compose.ui.geometry.Size(barWidth.toFloat(), normalizedHeight.toFloat().coerceAtLeast(4f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
            )

            // Draw label
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = labelColor.toArgb()
                    textSize = 11.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }

                // Amount label above bar
                if (amount > 0) {
                    canvas.nativeCanvas.drawText(
                        "$${amount.toInt()}",
                        (startX + barWidth / 2).toFloat(),
                        (startY - 8f).toFloat(),
                        paint
                    )
                }

                // Month text
                canvas.nativeCanvas.drawText(
                    monthData.label,
                    (startX + barWidth / 2).toFloat(),
                    (totalHeight - 4f).toFloat(),
                    paint
                )
            }
        }
    }
}

// ---------------- CUSTOM LINE GRAPH DRAWING ----------------

@Composable
fun MileageTrendLineGraph(history: List<Double>) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val maxVal = remember(history) { history.maxOrNull()?.coerceAtLeast(5.0) ?: 15.0 }
    val minVal = remember(history) { (history.minOrNull() ?: 0.0).coerceAtMost(maxVal - 1.0) }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val width = size.width
        val height = size.height

        val paddingBottom = 20.dp.toPx()
        val paddingLeft = 32.dp.toPx()
        val graphWidth = width - paddingLeft
        val graphHeight = height - paddingBottom

        // Draw gridlines (Horizontal)
        val horizontalLinesCount = 3
        for (i in 0..horizontalLinesCount) {
            val y = i * (graphHeight / horizontalLinesCount)
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y.toFloat()),
                end = Offset(width, y.toFloat()),
                strokeWidth = 1f
            )

            // Draw Y scale values
            drawIntoCanvas { canvas ->
                val scaleVal = maxVal - i * ((maxVal - minVal) / horizontalLinesCount)
                val paint = android.graphics.Paint().apply {
                    color = labelColor.toArgb()
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
                canvas.nativeCanvas.drawText(
                    String.format(Locale.US, "%.1f", scaleVal),
                    (paddingLeft - 8f).toFloat(),
                    (y + 3.dp.toPx()).toFloat(),
                    paint
                )
            }
        }

        // Draw Line path
        if (history.isNotEmpty()) {
            val points = history.indices.map { i ->
                val x = paddingLeft + i * (graphWidth / (history.size - 1).coerceAtLeast(1))
                val ratio = (history[i] - minVal) / (maxVal - minVal).coerceAtLeast(0.1)
                val y = graphHeight - (ratio * graphHeight)
                Offset(x.toFloat(), y.toFloat())
            }

            // Drawing path
            val path = Path().apply {
                if (points.isNotEmpty()) {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Drawing data points circles
            points.forEachIndexed { idx, point ->
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = lineColor,
                    radius = 2.dp.toPx(),
                    center = point
                )

                // Label values
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    canvas.nativeCanvas.drawText(
                        String.format(Locale.US, "%.1f", history[idx]),
                        point.x,
                        (point.y - 10f).coerceAtLeast(12f).toFloat(),
                        paint
                    )
                }
            }
        }
    }
}

// ---------------- CALCULATE MONTHLY GRAPH METRICS ----------------

data class MonthSpendData(
    val label: String,
    val amount: Double
)

fun getRecentMonthsSpendData(entries: List<FuelEntry>): List<MonthSpendData> {
    val result = mutableListOf<MonthSpendData>()
    val cal = Calendar.getInstance()

    // 6 Months including current month
    for (i in 5 downTo 0) {
        val targetMonthCal = Calendar.getInstance()
        targetMonthCal.add(Calendar.MONTH, -i)
        val year = targetMonthCal.get(Calendar.YEAR)
        val month = targetMonthCal.get(Calendar.MONTH) // 0-11

        // Find range of target month
        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, startCal.getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val spend = entries
            .filter { it.timestamp in startCal.timeInMillis..endCal.timeInMillis }
            .sumOf { it.totalCost }

        val monthsAbbr = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        result.add(MonthSpendData(monthsAbbr[month], spend))
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FuelViewModel
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val showMonthlySpend by viewModel.showMonthlySpend.collectAsState()
    val show6MonthGraph by viewModel.show6MonthGraph.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val distanceUnit by viewModel.distanceUnit.collectAsState()
    val volumeUnit by viewModel.volumeUnit.collectAsState()

    var isThemeDropdownExpanded by remember { mutableStateOf(false) }
    var isCurrencyDropdownExpanded by remember { mutableStateOf(false) }
    var isDistanceDropdownExpanded by remember { mutableStateOf(false) }
    var isVolumeDropdownExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Preferences & Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Personalize app appearance, units, currency and dashboard layout details.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Appearance Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "APPEARANCE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "App Theme",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Select active color scheme preference",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box {
                            TextButton(
                                onClick = { isThemeDropdownExpanded = true },
                                modifier = Modifier.testTag("theme_selector_button")
                            ) {
                                Text(text = themeMode, fontWeight = FontWeight.Bold)
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }

                            DropdownMenu(
                                expanded = isThemeDropdownExpanded,
                                onDismissRequest = { isThemeDropdownExpanded = false }
                            ) {
                                listOf("Light", "Dark", "System").forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(mode) },
                                        onClick = {
                                            viewModel.setThemeMode(mode)
                                            isThemeDropdownExpanded = false
                                        },
                                        modifier = Modifier.testTag("theme_option_${mode.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Metrics & Currency Customization Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "METRICS & CURRENCY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Currency row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Preferred Currency",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Select currency symbol for costs and stats",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box {
                            TextButton(
                                onClick = { isCurrencyDropdownExpanded = true },
                                modifier = Modifier.testTag("currency_selector_button")
                            ) {
                                Text(text = currencySymbol, fontWeight = FontWeight.Bold)
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }

                            DropdownMenu(
                                expanded = isCurrencyDropdownExpanded,
                                onDismissRequest = { isCurrencyDropdownExpanded = false }
                            ) {
                                listOf("$", "€", "£", "₹", "¥").forEach { symbol ->
                                    DropdownMenuItem(
                                        text = { Text(symbol) },
                                        onClick = {
                                            viewModel.setCurrencySymbol(symbol)
                                            isCurrencyDropdownExpanded = false
                                        },
                                        modifier = Modifier.testTag("currency_option_${symbol}")
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Distance unit row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Distance Unit",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Choose odometer preference unit",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box {
                            TextButton(
                                onClick = { isDistanceDropdownExpanded = true },
                                modifier = Modifier.testTag("distance_unit_selector_button")
                            ) {
                                Text(text = if (distanceUnit == "km") "Kilometers (km)" else "Miles (mi)", fontWeight = FontWeight.Bold)
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }

                            DropdownMenu(
                                expanded = isDistanceDropdownExpanded,
                                onDismissRequest = { isDistanceDropdownExpanded = false }
                            ) {
                                listOf("km" to "Kilometers (km)", "mi" to "Miles (mi)").forEach { (unit, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            viewModel.setDistanceUnit(unit)
                                            isDistanceDropdownExpanded = false
                                        },
                                        modifier = Modifier.testTag("distance_option_${unit}")
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Volume unit row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Volume Unit",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Choose fuel capacity measurement unit",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box {
                            TextButton(
                                onClick = { isVolumeDropdownExpanded = true },
                                modifier = Modifier.testTag("volume_unit_selector_button")
                            ) {
                                Text(text = if (volumeUnit == "L") "Liters (L)" else "Gallons (gal)", fontWeight = FontWeight.Bold)
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }

                            DropdownMenu(
                                expanded = isVolumeDropdownExpanded,
                                onDismissRequest = { isVolumeDropdownExpanded = false }
                            ) {
                                listOf("L" to "Liters (L)", "gal" to "Gallons (gal)").forEach { (unit, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            viewModel.setVolumeUnit(unit)
                                            isVolumeDropdownExpanded = false
                                        },
                                        modifier = Modifier.testTag("volume_option_${unit}")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dashboard customization Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "DASHBOARD CUSTOMIZATION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 1: Vehicle Profiles (Always enabled label)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Vehicle Profiles",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Required to see registered fleets",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = true,
                            onCheckedChange = null,
                            enabled = false,
                            modifier = Modifier.testTag("dashboard_vehicles_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 2: Monthly Spend Overview toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Monthly Spend Card",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Show overall spend in current calendar month",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showMonthlySpend,
                            onCheckedChange = { viewModel.setShowMonthlySpend(it) },
                            modifier = Modifier.testTag("dashboard_monthly_spend_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 3: 6-Month Graph toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "6-Month Consumption Graph",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Display 6-month aggregate fuel comparison",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = show6MonthGraph,
                            onCheckedChange = { viewModel.setShow6MonthGraph(it) },
                            modifier = Modifier.testTag("dashboard_6_month_graph_switch")
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFuelEntryDialog(
    entry: FuelEntry,
    currencySymbol: String,
    distanceUnit: String,
    volumeUnit: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double, Double, Double, String?) -> Unit
) {
    var priceInput by remember { mutableStateOf(entry.pricePerLiter.toString()) }
    var litersInput by remember { mutableStateOf(entry.litersFilled.toString()) }
    var totalCostInput by remember { mutableStateOf(entry.totalCost.toString()) }
    var odometerInput by remember { mutableStateOf(entry.odometer.toInt().toString()) }
    var locationInput by remember { mutableStateOf(entry.location ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Edit Fuel Fill-Up",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { priceInput = it },
                    label = { Text("Price per $volumeUnit ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("edit_entry_price")
                )
                OutlinedTextField(
                    value = litersInput,
                    onValueChange = { litersInput = it },
                    label = { Text("Volume Filled ($volumeUnit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("edit_entry_liters")
                )
                OutlinedTextField(
                    value = totalCostInput,
                    onValueChange = { totalCostInput = it },
                    label = { Text("Total Cost ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("edit_entry_cost")
                )
                OutlinedTextField(
                    value = odometerInput,
                    onValueChange = { odometerInput = it },
                    label = { Text("Odometer Reading ($distanceUnit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("edit_entry_odometer")
                )
                OutlinedTextField(
                    value = locationInput,
                    onValueChange = { locationInput = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_entry_location")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceInput.toDoubleOrNull() ?: entry.pricePerLiter
                    val liters = litersInput.toDoubleOrNull() ?: entry.litersFilled
                    val cost = totalCostInput.toDoubleOrNull() ?: entry.totalCost
                    val odo = odometerInput.toDoubleOrNull() ?: entry.odometer
                    val loc = locationInput.takeIf { it.isNotBlank() }
                    onConfirm(price, liters, cost, odo, loc)
                },
                modifier = Modifier.testTag("save_edit_entry_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_edit_entry_button")) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFuelEntryDialog(
    vehicles: List<Vehicle>,
    currencySymbol: String,
    distanceUnit: String,
    volumeUnit: String,
    onDismiss: () -> Unit,
    onConfirm: (Int, Double, Double, Double, Double, String?) -> Unit
) {
    if (vehicles.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("No Registered Vehicles") },
            text = { Text("Please add a vehicle profile to your fleet registry before logging fuel fill-ups.") },
            confirmButton = {
                Button(onClick = onDismiss) { Text("OK") }
            }
        )
        return
    }

    var selectedVehicle by remember { mutableStateOf(vehicles.first()) }
    var isVehicleDropdownExpanded by remember { mutableStateOf(false) }

    var priceInput by remember { mutableStateOf("") }
    var litersInput by remember { mutableStateOf("") }
    var totalCostInput by remember { mutableStateOf("") }
    var odometerInput by remember { mutableStateOf("") }
    var locationInput by remember { mutableStateOf("") }

    var isTotalCostOverridden by remember { mutableStateOf(false) }

    LaunchedEffect(priceInput, litersInput) {
        if (!isTotalCostOverridden) {
            val price = priceInput.toDoubleOrNull() ?: 0.0
            val liters = litersInput.toDoubleOrNull() ?: 0.0
            if (price > 0 && liters > 0) {
                totalCostInput = String.format(Locale.US, "%.2f", price * liters)
            } else if (price == 0.0 || liters == 0.0) {
                totalCostInput = ""
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalGasStation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Log Fuel Fill-Up",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select Vehicle",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { isVehicleDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth().testTag("add_entry_dialog_vehicle_selector")
                    ) {
                        Icon(
                            imageVector = getVehicleIcon(selectedVehicle.type),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(selectedVehicle.name, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = isVehicleDropdownExpanded,
                        onDismissRequest = { isVehicleDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        vehicles.forEach { vehicle ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = getVehicleIcon(vehicle.type),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(vehicle.name)
                                    }
                                },
                                onClick = {
                                    selectedVehicle = vehicle
                                    isVehicleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { priceInput = it },
                    label = { Text("Price per $volumeUnit ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("add_entry_dialog_price")
                )
                OutlinedTextField(
                    value = litersInput,
                    onValueChange = { litersInput = it },
                    label = { Text("Volume Filled ($volumeUnit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("add_entry_dialog_liters")
                )
                OutlinedTextField(
                    value = totalCostInput,
                    onValueChange = {
                        totalCostInput = it
                        isTotalCostOverridden = true
                    },
                    label = { Text("Total Cost ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("add_entry_dialog_cost")
                )
                OutlinedTextField(
                    value = odometerInput,
                    onValueChange = { odometerInput = it },
                    label = { Text("Odometer Reading ($distanceUnit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = odometerInput.isNotEmpty() && (odometerInput.toDoubleOrNull() ?: 0.0) <= 0.0,
                    supportingText = if (odometerInput.isNotEmpty() && (odometerInput.toDoubleOrNull() ?: 0.0) <= 0.0) {
                        { Text("Odometer must be greater than 0", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth().testTag("add_entry_dialog_odometer")
                )
                OutlinedTextField(
                    value = locationInput,
                    onValueChange = { locationInput = it },
                    label = { Text("Location (Optional)") },
                    modifier = Modifier.fillMaxWidth().testTag("add_entry_dialog_location")
                )
            }
        },
        confirmButton = {
            val isOdometerValid = (odometerInput.toDoubleOrNull() ?: 0.0) > 0.0
            val isLitersValid = (litersInput.toDoubleOrNull() ?: 0.0) > 0.0
            Button(
                onClick = {
                    val price = priceInput.toDoubleOrNull() ?: 0.0
                    val liters = litersInput.toDoubleOrNull() ?: 0.0
                    val cost = totalCostInput.toDoubleOrNull() ?: (price * liters)
                    val odo = odometerInput.toDoubleOrNull() ?: 0.0
                    val loc = locationInput.takeIf { it.isNotBlank() }
                    onConfirm(selectedVehicle.id, price, liters, cost, odo, loc)
                },
                enabled = isOdometerValid && isLitersValid,
                modifier = Modifier.testTag("submit_add_entry_dialog_button")
            ) {
                Text("Log Fill-Up")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_add_entry_dialog_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

