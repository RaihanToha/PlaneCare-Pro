package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object FirebaseSyncHelper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // Helper to get custom Firebase Database URL
    private fun getDatabaseUrl(): String {
        val projId = UserSettings.firebaseProjectId.value.trim()
        if (projId.isEmpty()) return ""
        if (projId.startsWith("http://") || projId.startsWith("https://")) {
            return if (projId.endsWith("/")) projId else "$projId/"
        }
        return "https://$projId-default-rtdb.firebaseio.com/"
    }

    private fun getApiKey(): String {
        return UserSettings.firebaseApiKey.value.trim()
    }

    suspend fun checkIsAdmin(userId: String, token: String): Boolean = withContext(Dispatchers.IO) {
        val dbUrl = getDatabaseUrl()
        if (dbUrl.isEmpty()) return@withContext false
        
        val url = "${dbUrl}admins/$userId.json?auth=$token"
        val request = Request.Builder().url(url).get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    return@withContext bodyStr.trim() != "null"
                }
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun registerAdmin(userId: String, email: String, token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val dbUrl = getDatabaseUrl()
        if (dbUrl.isEmpty()) return@withContext Result.failure(Exception("Firebase DB not configured"))
        
        val url = "${dbUrl}admins/$userId.json?auth=$token"
        val requestBody = JSONObject().apply {
            put("email", email)
            put("createdAt", System.currentTimeMillis())
        }.toString().toRequestBody(JSON_MEDIA_TYPE)
        
        val request = Request.Builder().url(url).put(requestBody).build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Failed to register as admin."))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val authResult = com.google.firebase.auth.FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).await()
            val tokenResult = authResult.user?.getIdToken(true)?.await()
            val token = tokenResult?.token ?: ""
            val json = JSONObject().apply {
                put("idToken", token)
                put("localId", authResult.user?.uid ?: "")
                put("email", authResult.user?.email ?: "")
            }
            Result.success(json)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val authResult = com.google.firebase.auth.FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).await()
            val tokenResult = authResult.user?.getIdToken(true)?.await()
            val token = tokenResult?.token ?: ""
            val json = JSONObject().apply {
                put("idToken", token)
                put("localId", authResult.user?.uid ?: "")
                put("email", authResult.user?.email ?: "")
            }
            Result.success(json)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncUserPlants(context: Context, plants: List<Plant>): Result<Boolean> = withContext(Dispatchers.IO) {
        val dbUrl = getDatabaseUrl()
        if (dbUrl.isEmpty()) {
            return@withContext Result.failure(Exception("Firebase Database/Project ID not configured."))
        }
        val userId = UserSettings.userId.value ?: return@withContext Result.failure(Exception("User not logged in."))
        val token = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()?.token ?: return@withContext Result.failure(Exception("Auth token missing."))

        val url = "${dbUrl}users/$userId/plants.json?auth=$token"
        
        val rootObj = JSONObject()
        val converters = Converters()
        
        for (plant in plants) {
            val plantObj = JSONObject()
            plantObj.put("id", plant.id)
            plantObj.put("name", plant.name)
            plantObj.put("category", plant.category)
            plantObj.put("summary", plant.summary)
            plantObj.put("about", plant.about)
            plantObj.put("image", plant.image)
            plantObj.put("growingInstructions", plant.growingInstructions)
            plantObj.put("careTips", plant.careTips)
            plantObj.put("sicknessInfo", plant.sicknessInfo)
            plantObj.put("remedies", plant.remedies)
            
            val issuesStr = converters.issuesToString(plant.healthIssues)
            plantObj.put("healthIssues", JSONArray(issuesStr))
            
            val pdfsArray = JSONArray()
            for (pdf in plant.pdfs) {
                try {
                    val pdfObj = JSONObject()
                    pdfObj.put("name", pdf.name)
                    pdfObj.put("filename", pdf.filename)
                    
                    val pdfFile = File(context.filesDir, pdf.filename)
                    if (pdfFile.exists()) {
                        val bytes = pdfFile.readBytes()
                        val base64Str = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        pdfObj.put("contentBase64", base64Str)
                    }
                    pdfsArray.put(pdfObj)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            plantObj.put("pdfs", pdfsArray)
            plantObj.put("lastAccessed", plant.lastAccessed)
            plantObj.put("createdAt", plant.createdAt)
            
            // Key by plant name or sanitised string to make it clean
            val safeKey = plant.name.replace(".", "_").replace("$", "_").replace("#", "_").replace("[", "_").replace("]", "_")
            rootObj.put(safeKey, plantObj)
        }

        val requestBody = rootObj.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Sync user plants failed: code ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncAdminPlants(context: Context, plants: List<Plant>): Result<Boolean> = withContext(Dispatchers.IO) {
        val dbUrl = getDatabaseUrl()
        if (dbUrl.isEmpty()) {
            return@withContext Result.failure(Exception("Firebase Database/Project ID not configured."))
        }
        val token = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()?.token ?: return@withContext Result.failure(Exception("Auth token missing."))

        val url = "${dbUrl}public_plants.json?auth=$token"
        
        val rootObj = JSONObject()
        val converters = Converters()
        
        for (plant in plants) {
            val plantObj = JSONObject()
            plantObj.put("id", plant.id)
            plantObj.put("name", plant.name)
            plantObj.put("category", plant.category)
            plantObj.put("summary", plant.summary)
            plantObj.put("about", plant.about)
            plantObj.put("image", plant.image)
            plantObj.put("growingInstructions", plant.growingInstructions)
            plantObj.put("careTips", plant.careTips)
            plantObj.put("sicknessInfo", plant.sicknessInfo)
            plantObj.put("remedies", plant.remedies)
            
            val issuesStr = converters.issuesToString(plant.healthIssues)
            plantObj.put("healthIssues", JSONArray(issuesStr))
            
            val pdfsArray = JSONArray()
            for (pdf in plant.pdfs) {
                try {
                    val pdfObj = JSONObject()
                    pdfObj.put("name", pdf.name)
                    pdfObj.put("filename", pdf.filename)
                    
                    val pdfFile = File(context.filesDir, pdf.filename)
                    if (pdfFile.exists()) {
                        val bytes = pdfFile.readBytes()
                        val base64Str = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        pdfObj.put("contentBase64", base64Str)
                    }
                    pdfsArray.put(pdfObj)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            plantObj.put("pdfs", pdfsArray)
            plantObj.put("lastAccessed", plant.lastAccessed)
            plantObj.put("createdAt", plant.createdAt)
            
            val safeKey = plant.name.replace(".", "_").replace("$", "_").replace("#", "_").replace("[", "_").replace("]", "_")
            rootObj.put(safeKey, plantObj)
        }

        val requestBody = rootObj.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Sync admin plants failed: code ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAdminPlants(token: String? = null): Result<List<Plant>> = withContext(Dispatchers.IO) {
        val dbUrl = getDatabaseUrl()
        if (dbUrl.isEmpty()) {
            return@withContext Result.failure(Exception("Firebase Database/Project ID not configured."))
        }

        val url = if (token != null) "${dbUrl}public_plants.json?auth=$token" else "${dbUrl}public_plants.json"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    if (bodyStr.trim() == "null" || bodyStr.isEmpty()) {
                        return@withContext Result.success(emptyList())
                    }
                    val plantsList = mutableListOf<Plant>()
                    val rootObj = JSONObject(bodyStr)
                    val keys = rootObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val plantObj = rootObj.getJSONObject(key)
                        
                        val pId = plantObj.optLong("id", System.currentTimeMillis())
                        val pName = plantObj.optString("name", "")
                        val pCategory = plantObj.optString("category", "")
                        val pSummary = plantObj.optString("summary", "")
                        val pAbout = plantObj.optString("about", "")
                        val pImage = plantObj.optString("image", "")
                        val pGrowing = plantObj.optString("growingInstructions", "")
                        val pCare = plantObj.optString("careTips", "")
                        val pSickness = plantObj.optString("sicknessInfo", "")
                        val pRemedies = plantObj.optString("remedies", "")
                        
                        val healthIssues = mutableListOf<HealthIssue>()
                        val issuesArray = plantObj.optJSONArray("healthIssues")
                        if (issuesArray != null) {
                            for (i in 0 until issuesArray.length()) {
                                try {
                                    val issueObj = issuesArray.getJSONObject(i)
                                    healthIssues.add(
                                        HealthIssue(
                                            issue = issueObj.optString("issue", ""),
                                            remedy = issueObj.optString("remedy", ""),
                                            image = if (issueObj.has("image") && !issueObj.isNull("image")) issueObj.getString("image") else null
                                        )
                                    )
                                } catch (e: Exception) {
                                    // Maybe it was string encoded array
                                }
                            }
                        }
                        
                        val pdfs = mutableListOf<PlantPdf>()
                        val pdfsArray = plantObj.optJSONArray("pdfs")
                        if (pdfsArray != null) {
                            for (i in 0 until pdfsArray.length()) {
                                try {
                                    val pdfObj = pdfsArray.getJSONObject(i)
                                    pdfs.add(
                                        PlantPdf(
                                            name = pdfObj.optString("name", ""),
                                            filename = pdfObj.optString("filename", "")
                                        )
                                    )
                                } catch (e: Exception) {}
                            }
                        }
                        
                        val lastAccessed = plantObj.optLong("lastAccessed", System.currentTimeMillis())
                        val createdAt = plantObj.optLong("createdAt", System.currentTimeMillis())
                        
                        plantsList.add(
                            Plant(
                                id = pId.toInt(),
                                name = pName,
                                category = pCategory,
                                summary = pSummary,
                                about = pAbout,
                                image = pImage,
                                growingInstructions = pGrowing,
                                careTips = pCare,
                                sicknessInfo = pSickness,
                                remedies = pRemedies,
                                healthIssues = healthIssues,
                                pdfs = pdfs,
                                lastAccessed = lastAccessed,
                                createdAt = createdAt
                            )
                        )
                    }
                    Result.success(plantsList)
                } else {
                    Result.failure(Exception("Fetch admin plants failed: code ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestPlantToAdmin(plantName: String, userEmail: String, token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val dbUrl = getDatabaseUrl()
        if (dbUrl.isEmpty()) {
            return@withContext Result.failure(Exception("Firebase Database/Project ID not configured."))
        }

        val url = "${dbUrl}requests.json?auth=$token"
        val requestObj = JSONObject().apply {
            put("plantName", plantName)
            put("userEmail", userEmail)
            put("requestedAt", System.currentTimeMillis())
        }

        val requestBody = requestObj.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Request to admin failed: code ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchPendingRequests(): Result<List<PlantRequest>> = withContext(Dispatchers.IO) {
        val dbUrl = getDatabaseUrl()
        if (dbUrl.isEmpty()) {
            return@withContext Result.failure(Exception("Firebase Database/Project ID not configured."))
        }
        val token = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()?.token ?: return@withContext Result.failure(Exception("Auth token missing."))

        val url = "${dbUrl}requests.json?auth=$token"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    if (bodyStr.trim() == "null" || bodyStr.isEmpty()) {
                        return@withContext Result.success(emptyList())
                    }
                    val reqList = mutableListOf<PlantRequest>()
                    val rootObj = JSONObject(bodyStr)
                    val keys = rootObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val reqObj = rootObj.getJSONObject(key)
                        reqList.add(
                            PlantRequest(
                                key = key,
                                plantName = reqObj.optString("plantName", ""),
                                userEmail = reqObj.optString("userEmail", ""),
                                requestedAt = reqObj.optLong("requestedAt", System.currentTimeMillis())
                            )
                        )
                    }
                    Result.success(reqList)
                } else {
                    Result.failure(Exception("Fetch requests failed: code ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRequest(requestKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val dbUrl = getDatabaseUrl()
        if (dbUrl.isEmpty()) {
            return@withContext Result.failure(Exception("Firebase Database/Project ID not configured."))
        }
        val token = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()?.token ?: return@withContext Result.failure(Exception("Auth token missing."))

        val url = "${dbUrl}requests/$requestKey.json?auth=$token"
        val request = Request.Builder()
            .url(url)
            .delete()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Delete request failed: code ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class PlantRequest(
    val key: String,
    val plantName: String,
    val userEmail: String,
    val requestedAt: Long
)
