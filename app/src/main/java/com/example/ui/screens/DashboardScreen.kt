package com.example.ui.screens

import android.graphics.BitmapFactory
import android.widget.Toast
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.i18n.I18nManager
import com.example.i18n.Language
import com.example.ui.PlantViewModel
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: PlantViewModel,
    onAddPlant: () -> Unit,
    onViewPlant: (Int) -> Unit,
    onNavigateToAllPlants: (String?, String?) -> Unit, // (initialSearchQuery, initialCategory)
    onNavigateToAuth: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentLang by I18nManager.currentLanguage.collectAsStateWithLifecycle()
    val plants by viewModel.plants.collectAsStateWithLifecycle()
    val latestPlant by viewModel.latestAccessed.collectAsStateWithLifecycle()

    val nicknameState = com.example.data.UserSettings.nickname.collectAsState()
    val nickname = nicknameState.value
    val accountNameState = com.example.data.UserSettings.accountName.collectAsState()
    val accountName = accountNameState.value
    val isDarkModeState = com.example.data.UserSettings.isDarkMode.collectAsState()
    val isDarkMode = isDarkModeState.value
    val profilePhotoState = com.example.data.UserSettings.profilePhoto.collectAsState()
    val profilePhoto = profilePhotoState.value
    val profilePhotoBitmap = rememberBase64Image(profilePhoto)
    
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    
    // Core state variables for multi-plant export selection and merge overlap resolution during import
    var showExportSelectionDialog by remember { mutableStateOf(false) }
    var showImportConflictDialog by remember { mutableStateOf(false) }
    var pendingImportPlants by remember { mutableStateOf<List<com.example.data.Plant>>(emptyList()) }
    var conflictingPlantNames by remember { mutableStateOf<List<String>>(emptyList()) }
    
    val profileImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.processImageUriAsync(context, uri) { base64 ->
                com.example.data.UserSettings.setProfilePhoto(base64)
            }
        }
    }

    val importFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            val res = com.example.data.BackupHelper.readPlantsFromBackup(context, uri)
            res.onSuccess { importedList ->
                if (importedList.isEmpty()) {
                    Toast.makeText(context, "No plant records found in the selected file.", Toast.LENGTH_SHORT).show()
                    return@onSuccess
                }
                val existingList = plants ?: emptyList()
                val existingNamesSet = existingList.map { it.name.trim().lowercase() }.toSet()
                
                val conflicts = importedList.filter { imp -> 
                    existingNamesSet.contains(imp.name.trim().lowercase())
                }
                
                if (conflicts.isNotEmpty()) {
                    pendingImportPlants = importedList
                    conflictingPlantNames = conflicts.map { it.name }
                    showImportConflictDialog = true
                } else {
                    viewModel.savePlantsList(importedList) {
                        Toast.makeText(context, "${importedList.size} plant records successfully imported!", Toast.LENGTH_LONG).show()
                    }
                    showProfileDialog = false
                }
            }.onFailure { err ->
                Toast.makeText(context, "Restore failed: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    var plantsToExport by remember { mutableStateOf<List<com.example.data.Plant>>(emptyList()) }
    val exportFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            val success = com.example.data.BackupHelper.exportBackupToUri(context, plantsToExport, uri)
            if (success) {
                Toast.makeText(context, "Backup file saved successfully to your device!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Failed to save backup file.", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    LaunchedEffect(nickname) {
        showNicknameDialog = nickname.isNullOrEmpty()
    }

    var searchQuery by remember { mutableStateOf("") }

    // Seed data on first launch
    LaunchedEffect(Unit) {
        viewModel.seedDefaultDataIfEmpty()
    }

    if (showNicknameDialog) {
        var enteredName by remember { mutableStateOf("") }
        var isNameError by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { /* Prevent dismiss by clicking outside */ },
            title = {
                Text(
                    text = "Welcome to PlantCare! 🌱",
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Let's personalize your plant tracker experience. Please select a nickname so we can welcome you properly!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = enteredName,
                        onValueChange = {
                            enteredName = it
                            if (isNameError) isNameError = false
                        },
                        label = { Text("Your Nickname") },
                        isError = isNameError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("nickname_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (isNameError) {
                        Text(
                            text = "Nickname cannot be empty",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (enteredName.trim().isNotEmpty()) {
                            com.example.data.UserSettings.setNickname(enteredName)
                            showNicknameDialog = false
                        } else {
                            isNameError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("nickname_save_button")
                ) {
                    Text("Get Started")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showExportSelectionDialog) {
        var selectedPlantIds by remember(plants) { mutableStateOf(plants.map { it.id }.toSet()) }
        
        AlertDialog(
            onDismissRequest = { showExportSelectionDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📤 Export Plants",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Select which plants to export:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { selectedPlantIds = plants.map { it.id }.toSet() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Select All")
                        }
                        TextButton(
                            onClick = { selectedPlantIds = emptySet() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Deselect All")
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    
                    if (plants.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No plants saved in the database.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                        ) {
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(scrollState),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                plants.forEach { plant ->
                                    val isChecked = selectedPlantIds.contains(plant.id)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                selectedPlantIds = if (isChecked) {
                                                    selectedPlantIds - plant.id
                                                } else {
                                                    selectedPlantIds + plant.id
                                                }
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                selectedPlantIds = if (checked == true) {
                                                    selectedPlantIds + plant.id
                                                } else {
                                                    selectedPlantIds - plant.id
                                                }
                                            }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = plant.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = plant.category,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            val toExport = plants.filter { selectedPlantIds.contains(it.id) }
                            if (toExport.isNotEmpty()) {
                                plantsToExport = toExport
                                exportFilePickerLauncher.launch("plantcare_backup.json")
                                showExportSelectionDialog = false
                            } else {
                                Toast.makeText(context, "Please select at least 1 plant.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedPlantIds.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Locally")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            val toExport = plants.filter { selectedPlantIds.contains(it.id) }
                            if (toExport.isNotEmpty()) {
                                com.example.data.BackupHelper.exportBackup(context, toExport)
                                showExportSelectionDialog = false
                            } else {
                                Toast.makeText(context, "Please select at least 1 plant.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedPlantIds.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Share App")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExportSelectionDialog = false }
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showImportConflictDialog) {
        AlertDialog(
            onDismissRequest = { showImportConflictDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚠️ Duplicate Plant Check",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "The following plants already exist in your app:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 120.dp)
                    ) {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            conflictingPlantNames.forEach { name ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text("🌿", fontSize = 14.sp)
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Would you like to overwrite existing data or save them as new copies under different names?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Button(
                        onClick = {
                            val existingList = plants ?: emptyList()
                            val reconciledList = pendingImportPlants.map { importedPlant ->
                                val match = existingList.find { it.name.trim().equals(importedPlant.name.trim(), ignoreCase = true) }
                                if (match != null) {
                                    importedPlant.copy(id = match.id)
                                } else {
                                    importedPlant
                                }
                            }
                            viewModel.savePlantsList(reconciledList) {
                                Toast.makeText(context, "${reconciledList.size} plants successfully imported & updated!", Toast.LENGTH_LONG).show()
                            }
                            showImportConflictDialog = false
                            showProfileDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Overwrite Existing Data")
                    }
                    
                    Button(
                        onClick = {
                            val existingList = plants ?: emptyList()
                            val existingNamesMutable = existingList.map { it.name.trim().lowercase() }.toMutableSet()
                            val reconciledList = pendingImportPlants.map { importedPlant ->
                                val exists = existingNamesMutable.contains(importedPlant.name.trim().lowercase())
                                if (exists) {
                                    val newName = getUniquePlantName(importedPlant.name, existingNamesMutable)
                                    existingNamesMutable.add(newName.trim().lowercase())
                                    importedPlant.copy(name = newName)
                                } else {
                                    existingNamesMutable.add(importedPlant.name.trim().lowercase())
                                    importedPlant
                                }
                            }
                            viewModel.savePlantsList(reconciledList) {
                                Toast.makeText(context, "${reconciledList.size} plants successfully imported under new names!", Toast.LENGTH_LONG).show()
                            }
                            showImportConflictDialog = false
                            showProfileDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save as Different Name (Keep Both)")
                    }
                    
                    TextButton(
                        onClick = { showImportConflictDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel Import", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    val isLoggedIn by com.example.data.UserSettings.isLoggedIn.collectAsStateWithLifecycle()
    val userEmail by com.example.data.UserSettings.userEmail.collectAsStateWithLifecycle()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()

    var showRequestsDialog by remember { mutableStateOf(false) }

    if (showRequestsDialog) {
        val pendingRequests by viewModel.pendingRequests.collectAsStateWithLifecycle()
        
        LaunchedEffect(Unit) {
            viewModel.fetchPendingRequests()
        }
        
        AlertDialog(
            onDismissRequest = { showRequestsDialog = false },
            title = {
                Text(
                    text = "Admin Dashboard 🛡️",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Requested plant varieties from users. Clicking a request pre-fills its name and takes you to the add screen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                        
                        if (pendingRequests.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No pending requests! 🎉",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        } else {
                            Box(modifier = Modifier.heightIn(max = 240.dp)) {
                                val scrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(scrollState),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pendingRequests.forEach { req ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.prefilledPlantName = req.plantName
                                                    viewModel.deleteRequest(req.key)
                                                    Toast.makeText(context, "Handled request for ${req.plantName}", Toast.LENGTH_SHORT).show()
                                                    showRequestsDialog = false
                                                    showProfileDialog = false
                                                    onAddPlant()
                                                },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = req.plantName,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "By: ${req.userEmail}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        viewModel.deleteRequest(req.key)
                                                        Toast.makeText(context, "Request deleted", Toast.LENGTH_SHORT).show()
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add, 
                                                        contentDescription = "Dismiss Request",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.rotate(45f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRequestsDialog = false }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showProfileDialog) {
        var editNickname by remember { mutableStateOf(nickname ?: "") }
        var profileError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🌱 Profile Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                val dialogScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .verticalScroll(dialogScroll),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile image display and edit
                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        if (profilePhotoBitmap != null) {
                            Image(
                                bitmap = profilePhotoBitmap,
                                contentDescription = "User Profile Photo",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(
                                        androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(ForestGreenPrimary, PolishedEmerald800)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (!nickname.isNullOrEmpty()) nickname.take(1).uppercase() else "P",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Edit camera/photo icon badge on bottom right
                        IconButton(
                            onClick = { profileImagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Change Profile Photo",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    if (isLoggedIn && !accountName.isNullOrEmpty()) {
                        Text(
                            text = "Account Name: $accountName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    // Nickname input
                    OutlinedTextField(
                        value = editNickname,
                        onValueChange = {
                            editNickname = it
                            if (profileError) profileError = false
                        },
                        label = { Text("Display Name") },
                        isError = profileError,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (profileError) {
                        Text(
                            text = "Name cannot be empty",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }

                    // Logout Button directly after Display Name Box if Logged In
                    if (isLoggedIn) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Logged in as:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = accountName ?: userEmail ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    if (com.example.data.UserSettings.isAdmin.value) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "ADMINISTRATOR 🛡️",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ForestGreenPrimary,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        viewModel.logout()
                                        Toast.makeText(context, "Logged out successfully!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Logout 🚪", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Small light/dark theme switch inside the profile
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Theme Preference",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        TextButton(
                            onClick = { com.example.data.UserSettings.toggleDarkMode() },
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = if (isDarkMode) "🌙 Theme: Dark" else "☀️ Theme: Light",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Import / Export backup section
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Backup & Data Sync",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Backup plants to transfer them to another device safely.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        // ADMIN REQUESTS BUTTON BEFORE IMPORT/EXPORT
                        if (isLoggedIn && com.example.data.UserSettings.isAdmin.value) {
                            Button(
                                onClick = {
                                    showRequestsDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Text("🛡️ Admin Dashboard")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Export Button
                            Button(
                                onClick = {
                                    showExportSelectionDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Text("📤 Export")
                            }

                            // Import Button
                            Button(
                                onClick = {
                                    importFilePickerLauncher.launch("application/json")
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            ) {
                                Text("📥 Import")
                            }
                        }

                        // CLOUD SYNC BUTTON BELOW IMPORT/EXPORT
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                if (isLoggedIn) {
                                    viewModel.cloudSync(context) { res ->
                                        res.onSuccess { msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, "Sync error: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    showProfileDialog = false
                                    onNavigateToAuth()
                                }
                            },
                            enabled = !isCloudSyncing,
                            modifier = Modifier.fillMaxWidth().testTag("profile_cloud_sync_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForestGreenPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            if (isCloudSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Syncing with Cloud...")
                            } else {
                                Text(if (isLoggedIn) "Cloud Sync ☁️" else "Login / Cloud Sync ☁️")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editNickname.trim().isNotEmpty()) {
                            com.example.data.UserSettings.setNickname(editNickname)
                            showProfileDialog = false
                        } else {
                            profileError = true
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showProfileDialog = false }
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPlant,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .shadow(8.dp, CircleShape)
                    .testTag("add_plant_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = I18nManager.getString("add_plant")
                )
            }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Header Section
            item {
                val isDark = !MaterialTheme.colorScheme.primary.equals(ForestGreenPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = if (!nickname.isNullOrEmpty()) "Welcome, $nickname!" else "Welcome!",
                            style = MaterialTheme.typography.headlineLarge,
                            color = if (isDark) MaterialTheme.colorScheme.onBackground else Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = I18nManager.getString("subtitle"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) SlateSecondaryDark else Color(0xFF64748B)
                        )
                    }

                    // Clickable profile picture / dashboard logo
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("dashboard_profile_button")
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable { showProfileDialog = true }
                    ) {
                        if (profilePhotoBitmap != null) {
                            Image(
                                bitmap = profilePhotoBitmap,
                                contentDescription = "Open Profile Dashboard Options",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(ForestGreenPrimary, PolishedEmerald800)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (!nickname.isNullOrEmpty()) nickname.take(1).uppercase() else "P",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Search Bar (navigates to AllPlants list on search action)
            item {
                val isDark = !MaterialTheme.colorScheme.primary.equals(ForestGreenPrimary)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { 
                        Text(
                            text = "Search from total ${plants.size} saved plants",
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
                                onNavigateToAllPlants(searchQuery, null)
                                searchQuery = ""
                            }) {
                                Text(
                                    text = "Go",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_bar_dash"),
                    shape = RoundedCornerShape(24.dp), // Polished extra-rounded corners
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (searchQuery.isNotEmpty()) {
                            onNavigateToAllPlants(searchQuery, null)
                            searchQuery = ""
                        }
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            if (searchQuery.isNotEmpty()) {
                val searchResults = plants.filter { 
                    it.name.contains(searchQuery, ignoreCase = true) || it.summary.contains(searchQuery, ignoreCase = true) 
                }
                
                if (searchResults.isEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f))
                                .padding(vertical = 32.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Not found locally. Click 'Go' to search the cloud.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "Search Results",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(searchResults.size) { index ->
                        val p = searchResults[index]
                        val imageBitmap = rememberBase64Image(p.image)
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onViewPlant(p.id) }
                                .padding(bottom = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (imageBitmap != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = imageBitmap,
                                        contentDescription = p.name,
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(14.dp)),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    PlantIconPlaceholder(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = p.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = p.summary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // "Recently Accessed" Section
                item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = I18nManager.getString("recently_accessed"),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (plants.isNotEmpty()) {
                        Text(
                            text = I18nManager.getString("cat_all"),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { onNavigateToAllPlants(null, null) }
                                .padding(4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (latestPlant != null) {
                    val p = latestPlant!!
                    val imageBitmap = rememberBase64Image(p.image)
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewPlant(p.id) }
                            .testTag("recent_plant_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (imageBitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = imageBitmap,
                                    contentDescription = p.name,
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                PlantIconPlaceholder(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                val catKey = when (p.category) {
                                    "Fruits" -> "cat_fruits"
                                    "Vegetables" -> "cat_vegetables"
                                    "Flowers" -> "cat_flowers"
                                    "Herbs" -> "cat_herbs"
                                    else -> p.category
                                }
                                
                                Text(
                                    text = I18nManager.getString(catKey).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = p.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = p.summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    // Friendly Empty Recent state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f))
                            .padding(vertical = 32.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = I18nManager.getString("no_recent"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Beautiful Static Categories Section
            item {
                Text(
                    text = I18nManager.getString("categories"),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                val categories = listOf(
                    CategoryItem(
                        key = "Fruits",
                        translationKey = "cat_fruits",
                        iconBackgroundColor = Color(0xFFFFEDD5), // orange-100
                        iconForegroundColor = Color(0xFFEA580C)  // orange-600
                    ) { drawFruitIcon(it) },
                    CategoryItem(
                        key = "Vegetables",
                        translationKey = "cat_vegetables",
                        iconBackgroundColor = Color(0xFFFEF3C7), // amber-100
                        iconForegroundColor = Color(0xFFD97706)  // amber-600
                    ) { drawVegetableIcon(it) },
                    CategoryItem(
                        key = "Flowers",
                        translationKey = "cat_flowers",
                        iconBackgroundColor = Color(0xFFFCE7F3), // pink-100
                        iconForegroundColor = Color(0xFFDB2777)  // pink-600
                    ) { drawFlowerIcon(it) },
                    CategoryItem(
                        key = "Herbs",
                        translationKey = "cat_herbs",
                        iconBackgroundColor = Color(0xFFD1FAE5), // emerald-100
                        iconForegroundColor = Color(0xFF059669)  // emerald-600
                    ) { drawHerbIcon(it) }
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (i in categories.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // First category card
                            val cat1 = categories[i]
                            CategoryCard(
                                item = cat1,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToAllPlants(null, cat1.key) }
                            )
                            
                            // Second category card
                            if (i + 1 < categories.size) {
                                val cat2 = categories[i + 1]
                                CategoryCard(
                                    item = cat2,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateToAllPlants(null, cat2.key) }
                                )
                            } else {
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Watermark Footer
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .height(1.dp)
                            .fillMaxWidth(0.25f)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "This App Was Created by Raihan Toha",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("raihan_toha_watermark")
                    )
                }
            }
            } // Close the else block for searchQuery.isNotEmpty()
        }
    }
}

/** Helper models & drawers for Dashboard category cards */
data class CategoryItem(
    val key: String,
    val translationKey: String,
    val iconBackgroundColor: Color,
    val iconForegroundColor: Color,
    val iconDrawer: @Composable (Color) -> Unit
)

@Composable
fun CategoryCard(
    item: CategoryItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = !MaterialTheme.colorScheme.primary.equals(ForestGreenPrimary)
    
    // In dark mode, keep subtle translucent colors, in light mode use the premium theme color codes!
    val finalBgBox = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else item.iconBackgroundColor
    val finalFgColor = if (isDark) MaterialTheme.colorScheme.primary else item.iconForegroundColor
    
    Card(
        modifier = modifier
            .height(110.dp)
            .clickable(onClick = onClick)
            .shadow(
                elevation = if (isDark) 0.dp else 1.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.05f)
            ),
        shape = RoundedCornerShape(24.dp), // Extra rounded corners to match professional design HTML "rounded-3xl"
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(finalBgBox),
                contentAlignment = Alignment.Center
            ) {
                item.iconDrawer(finalFgColor)
            }
            Text(
                text = I18nManager.getString(item.translationKey),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

// Custom organic mini canvas drawers to present standard categories exquisitely:
@Composable
fun drawFruitIcon(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize(0.5f)) {
        val w = size.width
        val h = size.height
        // Draw apple/peach circle
        drawCircle(color = color, radius = w * 0.42f, center = Offset(w/2f, h * 0.55f))
        // Tiny leaf stem
        val stemPath = Path().apply {
            moveTo(w/2f, h * 0.25f)
            quadraticTo(w * 0.7f, 0f, w * 0.85f, h * 0.1f)
            quadraticTo(w * 0.6f, h * 0.2f, w/2f, h * 0.25f)
        }
        drawPath(path = stemPath, color = color)
    }
}

@Composable
fun drawVegetableIcon(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize(0.52f)) {
        val w = size.width
        val h = size.height
        // Draw a carrot triangle profile
        val path = Path().apply {
            moveTo(w / 2f, h) // tip
            lineTo(w * 0.2f, h * 0.25f)
            quadraticTo(w / 2f, 0f, w * 0.8f, h * 0.25f)
            close()
        }
        drawPath(path = path, color = color)
        // Little sprout tuft at the top
        drawCircle(color = color, radius = w * 0.12f, center = Offset(w / 2f, h * 0.1f))
    }
}

@Composable
fun drawFlowerIcon(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize(0.55f)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        // Draw distinct layered petals
        val numPetals = 6
        val radius = w * 0.25f
        for (i in 0 until numPetals) {
            val angle = i * (2 * Math.PI / numPetals)
            val px = cx + (radius * cos(angle)).toFloat()
            val py = cy + (radius * sin(angle)).toFloat()
            drawCircle(color = color.copy(alpha = 0.85f), radius = w * 0.18f, center = Offset(px, py))
        }
        // Center disk
        drawCircle(color = color, radius = w * 0.15f, center = Offset(cx, cy))
    }
}

@Composable
fun drawHerbIcon(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize(0.5f)) {
        val w = size.width
        val h = size.height
        // Draw dual sprout structure
        drawLine(
            color = color,
            start = Offset(w/2f, h),
            end = Offset(w/2f, h * 0.2f),
            strokeWidth = 2.5.dp.toPx()
        )
        // Left mini leaf
        val leftLeaf = Path().apply {
            moveTo(w/2f, h * 0.6f)
            cubicTo(0f, h * 0.4f, 0f, h * 0.1f, w/2f, h * 0.4f)
            close()
        }
        drawPath(path = leftLeaf, color = color)
        // Right mini leaf
        val rightLeaf = Path().apply {
            moveTo(w/2f, h * 0.4f)
            cubicTo(w, h * 0.2f, w, 0f, w/2f, h * 0.2f)
            close()
        }
        drawPath(path = rightLeaf, color = color)
    }
}

private fun convertToBanglaNumerals(number: Int): String {
    val enNumerals = listOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    val bnNumerals = listOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    val numStr = number.toString()
    return numStr.map { char ->
        val index = enNumerals.indexOf(char)
        if (index != -1) bnNumerals[index] else char
    }.joinToString("")
}

private fun getUniquePlantName(baseName: String, existingNames: Set<String>): String {
    var candidate = "$baseName (Copy)"
    var counter = 1
    while (existingNames.contains(candidate.trim().lowercase())) {
        counter++
        candidate = "$baseName (Copy $counter)"
    }
    return candidate
}
