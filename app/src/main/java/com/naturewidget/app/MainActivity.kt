package com.naturewidget.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.naturewidget.app.data.SettingsManager
import com.naturewidget.app.data.api.Observation
import com.naturewidget.app.data.repository.NatureRepository
import com.naturewidget.app.ui.theme.NatureWidgetTheme
import com.naturewidget.app.widget.NatureWidgetWorker
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val appContext = applicationContext
        val repository = NatureRepository(appContext)
        val settingsManager = SettingsManager.getInstance(appContext)
        
        setContent {
            NatureWidgetTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        repository = repository,
                        settingsManager = settingsManager,
                        onStartWidgetUpdates = {
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
    repository: NatureRepository,
    settingsManager: SettingsManager,
    onStartWidgetUpdates: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var currentObservation by remember { mutableStateOf<Observation?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Settings state
    var userLogin by remember { mutableStateOf("") }
    var savedUserLogin by remember { mutableStateOf("") }
    
    // Load saved settings
    LaunchedEffect(Unit) {
        try {
            settingsManager.userLoginFlow.collect { saved ->
                savedUserLogin = saved
                if (userLogin.isEmpty()) {
                    userLogin = saved
                }
            }
        } catch (e: Exception) {
            // Ignore errors, use defaults
        }
    }
    
    fun loadNewObservation() {
        scope.launch {
            isLoading = true
            error = null
            
            val result = repository.getRandomObservation(
                userLogin = savedUserLogin
            )
            
            result.fold(
                onSuccess = { observation ->
                    currentObservation = observation
                    // Also cache it for the widget
                    repository.downloadAndCacheImage(observation)
                },
                onFailure = { e ->
                    error = e.message ?: "Failed to load observation"
                }
            )
            
            isLoading = false
        }
    }
    
    fun saveSettings() {
        scope.launch {
            settingsManager.setUserLogin(userLogin)
            savedUserLogin = userLogin.trim()
            // Reload with new settings
            loadNewObservation()
        }
    }
    
    // Load initial observation
    LaunchedEffect(Unit) {
        onStartWidgetUpdates()
        loadNewObservation()
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
                    Spacer(modifier = Modifier.height(12.dp))
                    
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
                        text = "Leave empty for random observations from all users",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = { saveSettings() },
                        enabled = userLogin.trim() != savedUserLogin,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1a472a)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save & Refresh")
                    }
                    
                    if (savedUserLogin.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✓ Showing observations from: $savedUserLogin",
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
                        text = "1. Enter your iNaturalist username above\n" +
                               "2. Add the widget to your home screen\n" +
                               "3. Tap the widget to load a new photo\n" +
                               "4. Images update automatically every 4 hours",
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Preview section
            Text(
                text = "Preview",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Current observation preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1a472a)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(color = Color.White)
                    }
                    error != null -> {
                        Text(
                            text = error!!,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    currentObservation != null -> {
                        val photo = currentObservation!!.photos.firstOrNull()
                        if (photo != null) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    painter = rememberAsyncImagePainter(photo.getMediumUrl()),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                
                                // Info overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomStart)
                                        .background(Color(0x99000000))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = currentObservation!!.taxon?.preferredCommonName 
                                                ?: currentObservation!!.speciesGuess 
                                                ?: "Unknown",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        currentObservation!!.taxon?.name?.let { name ->
                                            Text(
                                                text = name,
                                                color = Color.LightGray,
                                                fontStyle = FontStyle.Italic,
                                                fontSize = 13.sp
                                            )
                                        }
                                        currentObservation!!.placeGuess?.let { place ->
                                            Text(
                                                text = "📍 $place",
                                                color = Color.Gray,
                                                fontSize = 12.sp
                                            )
                                        }
                                        currentObservation!!.user?.login?.let { user ->
                                            Text(
                                                text = "👤 $user",
                                                color = Color.Gray,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Refresh button
            Button(
                onClick = { loadNewObservation() },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1a472a)
                )
            ) {
                Text("Load New Observation")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Attribution
            Text(
                text = "Images from iNaturalist.org\nData licensed under CC",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}
