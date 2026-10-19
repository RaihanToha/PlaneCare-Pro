package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.example.ui.theme.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.i18n.I18nManager
import com.example.ui.PlantViewModel
import com.example.ui.DownloadProgressState
import com.example.data.UserSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllPlantsScreen(
    viewModel: PlantViewModel,
    initialSearchQuery: String?,
    initialCategory: String?,
    onBack: () -> Unit,
    onViewPlant: (Int) -> Unit,
    onNavigateToLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val plants by viewModel.plants.collectAsStateWithLifecycle()
    val publicPlants by viewModel.publicPlants.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val isLoggedIn by UserSettings.isLoggedIn.collectAsStateWithLifecycle()
    val userEmail by UserSettings.userEmail.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf(initialSearchQuery ?: "") }
    var selectedCategory by remember { mutableStateOf(initialCategory ?: "All") }

    val categories = listOf("All", "Fruits", "Vegetables", "Flowers", "Herbs")

    // Trigger cloud fetch of admin plants when search query changes if logged in
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            viewModel.fetchPublicPlants()
        }
    }

    // Filter plants locally based on search query and category
    val filteredPlants = remember(plants, searchQuery, selectedCategory) {
        plants.filter { p ->
            val matchesCategory = selectedCategory == "All" || p.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isEmpty() || 
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.summary.contains(searchQuery, ignoreCase = true) ||
                    p.category.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    // Find matching public plants that are NOT saved locally
    val cloudPlants = remember(publicPlants, plants, searchQuery, selectedCategory) {
        publicPlants.filter { pub ->
            val alreadyLocal = plants.any { it.name.lowercase().trim() == pub.name.lowercase().trim() }
            val matchesCategory = selectedCategory == "All" || pub.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isEmpty() || pub.name.contains(searchQuery, ignoreCase = true) ||
                    pub.summary.contains(searchQuery, ignoreCase = true)
            !alreadyLocal && matchesCategory && matchesSearch
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.trim().isNotEmpty() && cloudPlants.isNotEmpty()) {
            kotlinx.coroutines.delay(800)
            Toast.makeText(context, "Found ${cloudPlants.size} plant(s) available for download!", Toast.LENGTH_SHORT).show()
        }
    }

    var requestStatus by remember { mutableStateOf("") }
    var isRequesting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = I18nManager.getString("all_plants"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("all_plants_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = I18nManager.getString("back")
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (downloadProgress.isDownloading) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp)),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Downloading ${downloadProgress.plantName}... 🌱",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${downloadProgress.percentage}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { downloadProgress.percentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                        Text(
                            text = "Est. ${downloadProgress.estimatedSecondsRemaining}s remaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            // Live Search Bar
            val isDark = !MaterialTheme.colorScheme.primary.equals(ForestGreenPrimary)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it 
                    requestStatus = ""
                },
                placeholder = { 
                    Text(
                        text = I18nManager.getString("search_dash"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) SlateSecondaryDark else Color(0xFF94A3B8)
                    ) 
                },
                leadingIcon = { 
                    Icon(
                        imageVector = Icons.Default.Search, 
                        contentDescription = "Search",
                        tint = if (isDark) SlateSecondaryDark else Color(0xFF94A3B8)
                    ) 
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { 
                            searchQuery = "" 
                            requestStatus = ""
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear, 
                                contentDescription = "Clear",
                                tint = if (isDark) SlateSecondaryDark else Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("all_plants_search_input"),
                shape = RoundedCornerShape(24.dp), // Polished extra-rounded corners
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Categories Filter horizontal list
            LazyRow(
                modifier = Modifier.fillMaxWidth().testTag("category_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                    val catTranslationKey = when (cat) {
                        "All" -> "cat_all"
                        "Fruits" -> "cat_fruits"
                        "Vegetables" -> "cat_vegetables"
                        "Flowers" -> "cat_flowers"
                        "Herbs" -> "cat_herbs"
                        else -> cat
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp)) // Extra rounded pills
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.Transparent else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = I18nManager.getString(catTranslationKey),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dynamic plants listing
            if (filteredPlants.isNotEmpty() || cloudPlants.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // Local saved plants section
                    if (filteredPlants.isNotEmpty()) {
                        item {
                            Text(
                                text = "Local Database",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(filteredPlants, key = { "local_${it.id}" }) { plant ->
                            PlantListItemCard(
                                plant = plant,
                                onClick = { onViewPlant(plant.id) }
                            )
                        }
                    }

                    // Cloud matching plants section
                    if (cloudPlants.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Available in Admin's Cloud ☁️",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(cloudPlants, key = { "cloud_${it.name}" }) { plant ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(2.dp, RoundedCornerShape(24.dp)),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PlantIconPlaceholder(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = plant.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = plant.summary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.downloadPlant(context, plant) {
                                                Toast.makeText(context, "${plant.name} downloaded offline!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Get 📥", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Empty matching search list state -> Show Request to Admin Option if searchQuery is not empty!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        if (searchQuery.trim().isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .shadow(4.dp, RoundedCornerShape(20.dp)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "🔍 Not found in Local Database or Cloud",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Would you like to request the admin to add '${searchQuery}' to the catalog?",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    if (requestStatus.isNotEmpty()) {
                                        Text(
                                            text = requestStatus,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (requestStatus.contains("Success")) ForestGreenPrimary else MaterialTheme.colorScheme.error,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (!isLoggedIn) {
                                                onNavigateToLogin()
                                                return@Button
                                            }
                                            isRequesting = true
                                            viewModel.requestPlant(searchQuery, userEmail ?: "anonymous@plantcare.com") { res ->
                                                isRequesting = false
                                                res.onSuccess { msg ->
                                                    requestStatus = "Success: $msg"
                                                }.onFailure { err ->
                                                    requestStatus = "Error: ${err.localizedMessage}"
                                                }
                                            }
                                        },
                                        enabled = !isRequesting,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isRequesting) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                                        } else {
                                            Text(if (isLoggedIn) "Request to Admin 🙋‍♂️" else "Log In to Request")
                                        }
                                    }
                                }
                            }
                        } else {
                            PlantIconPlaceholder(
                                modifier = Modifier
                                    .size(88.dp)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f))
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = I18nManager.getString("no_plants"),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlantListItemCard(
    plant: com.example.data.Plant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imgBitmap = rememberBase64Image(plant.image)
    val isDark = !MaterialTheme.colorScheme.primary.equals(ForestGreenPrimary)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("plant_card_${plant.id}")
            .shadow(
                elevation = if (isDark) 0.dp else 1.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.04f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Plant image or icon placeholder
            if (imgBitmap != null) {
                Image(
                    bitmap = imgBitmap,
                    contentDescription = plant.name,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                PlantIconPlaceholder(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Category small badge
                val categoryTranslationKey = when (plant.category) {
                    "Fruits" -> "cat_fruits"
                    "Vegetables" -> "cat_vegetables"
                    "Flowers" -> "cat_flowers"
                    "Herbs" -> "cat_herbs"
                    else -> plant.category
                }
                
                // Let's color-code each list badge exquisitely!
                val (badgeBg, badgeFg) = when (plant.category) {
                    "Fruits" -> Pair(Color(0xFFFFEDD5), Color(0xFFEA580C))
                    "Vegetables" -> Pair(Color(0xFFFEF3C7), Color(0xFFD97706))
                    "Flowers" -> Pair(Color(0xFFFCE7F3), Color(0xFFDB2777))
                    "Herbs" -> Pair(Color(0xFFD1FAE5), Color(0xFF059669))
                    else -> Pair(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), MaterialTheme.colorScheme.primary)
                }
                
                val finalBg = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else badgeBg
                val finalFg = if (isDark) MaterialTheme.colorScheme.primary else badgeFg

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(finalBg)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = I18nManager.getString(categoryTranslationKey).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = finalFg,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = plant.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = plant.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
