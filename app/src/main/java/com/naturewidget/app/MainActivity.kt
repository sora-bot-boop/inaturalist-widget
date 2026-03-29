package com.naturewidget.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.naturewidget.app.data.SettingsManager
import com.naturewidget.app.data.SettingsManager.Companion.WidgetMode
import com.naturewidget.app.widget.NatureWidgetWorker
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val appContext = applicationContext
        val settings = SettingsManager.getInstance(appContext)
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        settings = settings,
                        onScheduleUpdates = {
                            NatureWidgetWorker.enqueuePeriodic(appContext)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    settings: SettingsManager,
    onScheduleUpdates: () -> Unit
) {
    val context = LocalContext.current
    
    // Settings state
    var userLogin by remember { mutableStateOf(settings.getUserLogin()) }
    var savedUserLogin by remember { mutableStateOf(settings.getUserLogin()) }
    var refreshInterval by remember { mutableStateOf(settings.getRefreshInterval().toFloat()) }
    var savedRefreshInterval by remember { mutableStateOf(settings.getRefreshInterval()) }
    var selectedLocale by remember { mutableStateOf(settings.getLocaleSetting()) }
    var savedLocale by remember { mutableStateOf(settings.getLocaleSetting()) }
    var selectedMode by remember { mutableStateOf(settings.getWidgetMode()) }
    var savedMode by remember { mutableStateOf(settings.getWidgetMode()) }
    var settingsSaved by remember { mutableStateOf(false) }
    var localeDropdownExpanded by remember { mutableStateOf(false) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                               permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }
    
    fun hasUnsavedChanges(): Boolean {
        return userLogin.trim() != savedUserLogin || 
               refreshInterval.roundToInt() != savedRefreshInterval ||
               selectedLocale != savedLocale ||
               selectedMode != savedMode
    }
    
    fun saveSettings() {
        settings.setUserLogin(userLogin)
        settings.setRefreshInterval(refreshInterval.roundToInt())
        settings.setLocale(selectedLocale)
        settings.setWidgetMode(selectedMode)
        savedUserLogin = userLogin.trim()
        savedRefreshInterval = refreshInterval.roundToInt()
        savedLocale = selectedLocale
        savedMode = selectedMode
        onScheduleUpdates() // Re-schedule with new interval
        settingsSaved = true
    }
    
    // Start widget updates on launch
    LaunchedEffect(Unit) {
        onScheduleUpdates()
    }
    
    // Reset saved message after delay
    LaunchedEffect(settingsSaved) {
        if (settingsSaved) {
            kotlinx.coroutines.delay(2000)
            settingsSaved = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nature Widget") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1a472a),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⚙️ Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Mode selector
                    Text(
                        text = "Widget Mode",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column(modifier = Modifier.selectableGroup()) {
                        WidgetMode.entries.forEach { mode ->
                            val isSelected = selectedMode == mode
                            val needsSetup = when (mode) {
                                WidgetMode.PERSONAL -> userLogin.isBlank()
                                WidgetMode.DISCOVER -> !hasLocationPermission
                                else -> false
                            }
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .selectable(
                                        selected = isSelected,
                                        onClick = {
                                            selectedMode = mode
                                            // Request location permission if Discover mode selected
                                            if (mode == WidgetMode.DISCOVER && !hasLocationPermission) {
                                                locationPermissionLauncher.launch(
                                                    arrayOf(
                                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                                        Manifest.permission.ACCESS_FINE_LOCATION
                                                    )
                                                )
                                            }
                                        },
                                        role = Role.RadioButton
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF1a472a).copy(alpha = 0.1f) 
                                                    else MaterialTheme.colorScheme.surface
                                ),
                                border = if (isSelected) BorderStroke(2.dp, Color(0xFF1a472a)) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = Color(0xFF1a472a)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = mode.displayName,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = mode.description,
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                        if (needsSetup && isSelected) {
                                            Text(
                                                text = when (mode) {
                                                    WidgetMode.PERSONAL -> "⚠️ Enter username below"
                                                    WidgetMode.DISCOVER -> "⚠️ Location permission required"
                                                    else -> ""
                                                },
                                                fontSize = 12.sp,
                                                color = Color(0xFFE65100)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Username field (only show when Personal mode is selected)
                    if (selectedMode == WidgetMode.PERSONAL) {
                        OutlinedTextField(
                            value = userLogin,
                            onValueChange = { userLogin = it },
                            label = { Text("iNaturalist Username") },
                            placeholder = { Text("e.g., kueda") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "Your iNaturalist username to show your observations",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Refresh interval slider
                    Text(
                        text = "Refresh Interval",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = refreshInterval,
                            onValueChange = { refreshInterval = it },
                            valueRange = 1f..60f,
                            steps = 58, // 1, 2, 3, ... 60
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF1a472a),
                                activeTrackColor = Color(0xFF1a472a)
                            )
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = "${refreshInterval.roundToInt()} min",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.width(60.dp)
                        )
                    }
                    
                    Text(
                        text = "How often the widget loads a new photo (min 15 min due to Android limits)",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Language selector
                    Text(
                        text = "Language",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ExposedDropdownMenuBox(
                        expanded = localeDropdownExpanded,
                        onExpandedChange = { localeDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = SettingsManager.SUPPORTED_LOCALES.find { it.first == selectedLocale }?.second ?: "Auto",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = localeDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = localeDropdownExpanded,
                            onDismissRequest = { localeDropdownExpanded = false }
                        ) {
                            SettingsManager.SUPPORTED_LOCALES.forEach { (code, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        selectedLocale = code
                                        localeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Language for species common names",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Save button
                    Button(
                        onClick = { saveSettings() },
                        enabled = hasUnsavedChanges(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1a472a)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Settings")
                    }
                    
                    // Status messages
                    if (settingsSaved) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✓ Settings saved!",
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                    
                    if (savedUserLogin.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Showing observations from: $savedUserLogin",
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🌿 How to use",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Enter your iNaturalist username above (optional)\n" +
                               "2. Set how often you want new photos\n" +
                               "3. Add the widget to your home screen\n" +
                               "4. Tap the widget anytime to load a new photo",
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Images from iNaturalist.org\nData licensed under CC",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}
