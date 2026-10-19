package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.HealthIssue
import com.example.data.Plant
import com.example.data.PlantRepository
import com.example.data.FirebaseSyncHelper
import com.example.data.PlantRequest
import com.example.data.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.Locale

data class DownloadProgressState(
    val isDownloading: Boolean = false,
    val plantName: String = "",
    val percentage: Int = 0,
    val estimatedSecondsRemaining: Double = 0.0
)

class PlantViewModel(private val repository: PlantRepository) : ViewModel() {

    var prefilledPlantName: String? = null

    val plants: StateFlow<List<Plant>> = repository.allPlants
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val latestAccessed: StateFlow<Plant?> = com.example.data.UserSettings.lastAccessedPlantId
        .flatMapLatest { id ->
            if (id != null) repository.getPlantById(id) else kotlinx.coroutines.flow.flowOf(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Firebase-related Live states
    private val _downloadProgress = MutableStateFlow(DownloadProgressState())
    val downloadProgress: StateFlow<DownloadProgressState> = _downloadProgress.asStateFlow()

    private val _publicPlants = MutableStateFlow<List<Plant>>(emptyList())
    val publicPlants: StateFlow<List<Plant>> = _publicPlants.asStateFlow()

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    private val _pendingRequests = MutableStateFlow<List<PlantRequest>>(emptyList())
    val pendingRequests: StateFlow<List<PlantRequest>> = _pendingRequests.asStateFlow()

    fun getPlant(id: Int) = repository.getPlantById(id)

    fun updateLastAccessed(id: Int) {
        viewModelScope.launch {
            repository.updateLastAccessed(id, System.currentTimeMillis())
            com.example.data.UserSettings.setLastAccessedPlantId(id)
        }
    }

    fun login(email: String, password: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = FirebaseSyncHelper.signIn(email, password)
            result.onSuccess { json ->
                val token = json.optString("idToken", "")
                val userId = json.optString("localId", "")
                val resEmail = json.optString("email", email)
                
                val isAdmin = FirebaseSyncHelper.checkIsAdmin(userId, token)
                
                UserSettings.setLoginSession(resEmail, token, userId, isAdmin)
                onResult(Result.success("Logged in successfully as $resEmail" + if (isAdmin) " (Admin)" else ""))
            }.onFailure { error ->
                onResult(Result.failure(error))
            }
        }
    }

    fun register(email: String, password: String, name: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = FirebaseSyncHelper.signUp(email, password)
            result.onSuccess { json ->
                val token = json.optString("idToken", "")
                val userId = json.optString("localId", "")
                val resEmail = json.optString("email", email)
                
                val isAdmin = FirebaseSyncHelper.checkIsAdmin(userId, token)
                
                UserSettings.setLoginSession(resEmail, token, userId, isAdmin)
                if (name.isNotBlank()) {
                    UserSettings.setAccountName(name)
                }
                onResult(Result.success("Registered successfully as $resEmail" + if (isAdmin) " (Admin)" else ""))
            }.onFailure { error ->
                onResult(Result.failure(error))
            }
        }
    }

    fun logout() {
        UserSettings.clearLoginSession()
    }

    fun cloudSync(context: Context, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            _isCloudSyncing.value = true
            val isAdm = UserSettings.isAdmin.value
            val plantList = plants.value
            
            val result = if (isAdm) {
                FirebaseSyncHelper.syncAdminPlants(context, plantList)
            } else {
                FirebaseSyncHelper.syncUserPlants(context, plantList)
            }
            
            _isCloudSyncing.value = false
            result.onSuccess {
                onResult(Result.success(if (isAdm) "Admin master catalog synced to cloud!" else "Personal plants synced to cloud!"))
            }.onFailure { error ->
                onResult(Result.failure(error))
            }
        }
    }

    fun fetchPublicPlants() {
        viewModelScope.launch {
            val token = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()?.token
            val result = FirebaseSyncHelper.fetchAdminPlants(token)
            result.onSuccess { list ->
                _publicPlants.value = list
            }.onFailure {
                _publicPlants.value = emptyList()
            }
        }
    }

    fun requestPlant(plantName: String, userEmail: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val token = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()?.token ?: return@launch onResult(Result.failure(Exception("Auth token missing.")))
            val result = FirebaseSyncHelper.requestPlantToAdmin(plantName, userEmail, token)
            result.onSuccess {
                onResult(Result.success("Successfully requested '$plantName' to Admin!"))
            }.onFailure { error ->
                onResult(Result.failure(error))
            }
        }
    }

    fun fetchPendingRequests() {
        viewModelScope.launch {
            if (UserSettings.isLoggedIn.value && UserSettings.isAdmin.value) {
                val result = FirebaseSyncHelper.fetchPendingRequests()
                result.onSuccess { list ->
                    _pendingRequests.value = list
                }
                .onFailure {
                    _pendingRequests.value = emptyList()
                }
            }
        }
    }

    fun deleteRequest(requestKey: String) {
        viewModelScope.launch {
            val result = FirebaseSyncHelper.deleteRequest(requestKey)
            result.onSuccess {
                fetchPendingRequests()
            }
        }
    }

    fun downloadPlant(context: Context, plant: Plant, onComplete: () -> Unit) {
        viewModelScope.launch {
            // Check if already downloaded
            val exists = plants.value.any { it.name.lowercase().trim() == plant.name.lowercase().trim() }
            if (exists) {
                onComplete()
                return@launch
            }

            _downloadProgress.value = DownloadProgressState(
                isDownloading = true,
                plantName = plant.name,
                percentage = 0,
                estimatedSecondsRemaining = 2.0
            )

            // Beautiful simulation of chunk downloads for smooth percentage and est. time
            for (step in 1..10) {
                kotlinx.coroutines.delay(200)
                val percentage = step * 10
                val estRemaining = (10 - step) * 0.2
                _downloadProgress.value = DownloadProgressState(
                    isDownloading = true,
                    plantName = plant.name,
                    percentage = percentage,
                    estimatedSecondsRemaining = if (estRemaining > 0) String.format(Locale.US, "%.1f", estRemaining).toDouble() else 0.0
                )
            }

            // Save plant to database (reset id to 0 so Room generates it locally)
            val plantToSave = plant.copy(id = 0, lastAccessed = System.currentTimeMillis())
            repository.insert(plantToSave)

            _downloadProgress.value = DownloadProgressState(isDownloading = false)
            onComplete()
        }
    }

    fun savePlant(
        id: Int = 0,
        name: String,
        category: String,
        summary: String,
        about: String,
        image: String,
        growingInstructions: String,
        careTips: String,
        sicknessInfo: String,
        remedies: String,
        healthIssues: List<HealthIssue>,
        pdfs: List<com.example.data.PlantPdf> = emptyList(),
        onComplete: (Int) -> Unit
    ) {
        viewModelScope.launch {
            val plant = Plant(
                id = id,
                name = name,
                category = category,
                summary = summary,
                about = about,
                image = image,
                growingInstructions = growingInstructions,
                careTips = careTips,
                sicknessInfo = sicknessInfo,
                remedies = remedies,
                healthIssues = healthIssues,
                pdfs = pdfs,
                lastAccessed = System.currentTimeMillis(),
                createdAt = if (id == 0) System.currentTimeMillis() else System.currentTimeMillis() // preserve or set
            )
            val newId = if (id == 0) {
                repository.insert(plant).toInt()
            } else {
                repository.update(plant)
                id
            }
            com.example.data.UserSettings.setLastAccessedPlantId(newId)
            onComplete(newId)
        }
    }

    fun deletePlant(plant: Plant, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.delete(plant)
            onComplete()
        }
    }

    fun savePlantsList(plantsList: List<Plant>, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            plantsList.forEach { plant ->
                if (plant.id == 0) {
                    repository.insert(plant)
                } else {
                    repository.update(plant)
                }
            }
            onComplete()
        }
    }

    fun importBackup(
        context: Context,
        uri: Uri,
        onCompleted: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.importBackup(context, uri)
            result.onSuccess { count ->
                onCompleted(count)
            }.onFailure { error ->
                onError(error.localizedMessage ?: "Unknown error")
            }
        }
    }

    // Helper to transcode Uri to Base64
    fun processImageUriAsync(context: Context, uri: Uri, onResult: (String?) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val result = try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap == null) {
                    null
                } else {
                    // Resize image to max 400x400 to keep Base64 footprint lightweight
                    val maxDimension = 400
                    val width = bitmap.width
                    val height = bitmap.height
                    val (newWidth, newHeight) = if (width > height) {
                        val ratio = width.toFloat() / maxDimension
                        Pair(maxDimension, (height / ratio).toInt())
                    } else {
                        val ratio = height.toFloat() / maxDimension
                        Pair((width / ratio).toInt(), maxDimension)
                    }
                    
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                    val outputStream = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                    val byteArray = outputStream.toByteArray()
                    "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    fun seedDefaultDataIfEmpty() {
        viewModelScope.launch {
            // Check if plants is empty
            repository.allPlants.collect { list ->
                if (list.isEmpty()) {
                    val defaultPlants = listOf(
                        Plant(
                            name = "Tomato Plant",
                            category = "Vegetables",
                            summary = "Juicy organic red garden tomatoes.",
                            about = "Tomatoes are warm-season vegetables that thrive in direct sunlight. They are packed with antioxidants like lycopene and are highly versatile in culinary arts.",
                            image = "", // Empty means it uses custom modern visual drawing/placeholder
                            growingInstructions = "Sow seeds 1/4 inch deep in fertile, organic-rich starter soil. Transplant outdoor in full sun after first frost, spaced 2 feet apart.",
                            careTips = "Water deeply and regularly at the plant's base (at least 1-2 inches per week). Avoid wetting the foliage to prevent fungal diseases.",
                            sicknessInfo = "Early Blight and Blossom End Rot (blackening bottom due to lack of calcium).",
                            remedies = "Ensure stable moisture, enrich soil with calcium, and prune lower leaves to improve air circulation.",
                            healthIssues = listOf(
                                HealthIssue(
                                    issue = "Yellow Leaves at Bottom",
                                    remedy = "This is often caused by overwatering or nitrate deficiency. Reduce watering and apply balanced nitrogen fertilizer."
                                ),
                                HealthIssue(
                                    issue = "Blossom End Rot",
                                    remedy = "Add lime or bone meal to the soil to supply calcium, and make sure watering is highly consistent."
                                )
                            ),
                            lastAccessed = System.currentTimeMillis() - 10000,
                            createdAt = System.currentTimeMillis() - 10000
                        ),
                        Plant(
                            name = "Red Shrub Rose",
                            category = "Flowers",
                            summary = "Classic deep crimson roses with beautiful fragrance.",
                            about = "Roses are fragrant, woody perennial flowering shrubs representing beauty and grace. They require precise pruning but reward gardeners with beautiful aromatic blossoms.",
                            image = "",
                            growingInstructions = "Plant bare-root or potted shrubs in spring in a hole twice as wide as the root ball. Ensure 6+ hours of direct sunshine.",
                            careTips = "Apply mulch to retain soil dampness. Prune during late winter or early spring just as buds open, removing dead wood.",
                            sicknessInfo = "Black Spot (dark spots on leaves) and Powdery Mildew (white dusty coating).",
                            remedies = "Spray leaves with organic neem oil or baking soda solution. Never water roses from overhead in the evening.",
                            healthIssues = listOf(
                                HealthIssue(
                                    issue = "Black Leaf Spots",
                                    remedy = "Surgically prune infected leaves and apply organic copper fungicide spray. Ensure excellent foliage air circulation."
                                )
                            ),
                            lastAccessed = System.currentTimeMillis() - 5000,
                            createdAt = System.currentTimeMillis() - 5000
                        ),
                        Plant(
                            name = "Sweet Basil",
                            category = "Herbs",
                            summary = "Highly aromatic culinary herb perfect for pestos.",
                            about = "Basil is an aromatic kitchen herb that produces refreshing, flavorful tender green leaves. It is highly sensitive to cold weather but grows vigorously indoors or out.",
                            image = "",
                            growingInstructions = "Sow seeds in warm indoor pots or plant outside in a warm sunny location. Keep sprouts 10 inches apart.",
                            careTips = "Water diligently when soil top feels dry. Regularly pinch the top leaf clusters to encourage bushy lateral growth and pinch flower buds immediately.",
                            sicknessInfo = "Downy Mildew (gray mold underneath leaf surfaces) and Fusarium Wilt.",
                            remedies = "Water from the bottom early in the morning so moisture dries off during the day. Discard heavily infected plants.",
                            healthIssues = listOf(
                                HealthIssue(
                                    issue = "Stem drooping / Wilt",
                                    remedy = "Usually indicates dry stress or root rot from compact soil. Give thoroughly aerated soil and water only when surface is dry."
                                )
                            ),
                            lastAccessed = System.currentTimeMillis() - 1000,
                            createdAt = System.currentTimeMillis() - 1000
                        )
                    )
                    for (plant in defaultPlants) {
                        repository.insert(plant)
                    }
                }
            }
        }
    }
}

class ViewModelFactory(private val repository: PlantRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlantViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlantViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
