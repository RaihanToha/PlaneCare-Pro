package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupHelper {

    fun exportBackup(context: Context, plants: List<Plant>) {
        // Left intact for general sharing if needed
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                val rootObj = JSONObject()
                rootObj.put("appName", "PlantCare")
                rootObj.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                
                val plantsArray = JSONArray()
                val converters = Converters()
                
                for (plant in plants) {
                    val plantObj = JSONObject()
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
                    
                    plantsArray.put(plantObj)
                }
                
                rootObj.put("plants", plantsArray)
                
                // Write to a temporary backup file in filesDir
                val file = File(context.filesDir, "plantcare_backup.json")
                if (file.exists()) {
                    file.delete()
                }
                
                val fos = FileOutputStream(file)
                fos.write(rootObj.toString(2).toByteArray())
                fos.close()
                
                val authority = "${context.packageName}.fileprovider"
                val uri = FileProvider.getUriForFile(context, authority, file)
                
                withContext(Dispatchers.Main) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "PlantCare Backup Data")
                        putExtra(Intent.EXTRA_TEXT, "Here is the PlantCare data backup file (.json) to import your plants into any other device running the app.")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    
                    val chooserIntent = Intent.createChooser(shareIntent, "Share Backup via")
                    chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.startActivity(chooserIntent)
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun exportBackupToUri(context: Context, plants: List<Plant>, uri: Uri): Boolean {
        try {
            val rootObj = JSONObject()
            rootObj.put("appName", "PlantCare")
            rootObj.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            
            val plantsArray = JSONArray()
            val converters = Converters()
            
            for (plant in plants) {
                val plantObj = JSONObject()
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
                
                plantsArray.put(plantObj)
            }
            
            rootObj.put("plants", plantsArray)
            
            val outputStream = context.contentResolver.openOutputStream(uri) ?: return false
            outputStream.write(rootObj.toString(2).toByteArray(Charsets.UTF_8))
            outputStream.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun readPlantsFromBackup(context: Context, uri: Uri): Result<List<Plant>> {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("Cannot open file stream"))
            val bytes = inputStream.readBytes()
            inputStream.close()
            
            val jsonStr = String(bytes, Charsets.UTF_8)
            val rootObj = JSONObject(jsonStr)
            
            if (!rootObj.has("plants")) {
                return Result.failure(Exception("Invalid backup file: 'plants' key not found."))
            }
            
            val plantsArray = rootObj.getJSONArray("plants")
            val converters = Converters()
            val list = mutableListOf<Plant>()
            
            for (i in 0 until plantsArray.length()) {
                val plantObj = plantsArray.getJSONObject(i)
                val name = plantObj.optString("name", "Unnamed Plant")
                val category = plantObj.optString("category", "General")
                val summary = plantObj.optString("summary", "")
                val about = plantObj.optString("about", "")
                val image = plantObj.optString("image", "")
                val growingInstructions = plantObj.optString("growingInstructions", "")
                val careTips = plantObj.optString("careTips", "")
                val sicknessInfo = plantObj.optString("sicknessInfo", "")
                val remedies = plantObj.optString("remedies", "")
                
                val issuesArray = plantObj.optJSONArray("healthIssues")
                val healthIssues = if (issuesArray != null) {
                    converters.stringToIssues(issuesArray.toString())
                } else {
                    emptyList()
                }
                
                val pdfsArray = plantObj.optJSONArray("pdfs")
                val pdfs = mutableListOf<PlantPdf>()
                if (pdfsArray != null) {
                    for (j in 0 until pdfsArray.length()) {
                        try {
                            val pdfObj = pdfsArray.getJSONObject(j)
                            val pName = pdfObj.optString("name", "")
                            val pFilename = pdfObj.optString("filename", "")
                            if (pName.isNotEmpty() && pFilename.isNotEmpty()) {
                                pdfs.add(PlantPdf(name = pName, filename = pFilename))
                                
                                val contentBase64 = pdfObj.optString("contentBase64", "")
                                if (contentBase64.isNotEmpty()) {
                                    val destFile = File(context.filesDir, pFilename)
                                    val decodedBytes = android.util.Base64.decode(contentBase64, android.util.Base64.DEFAULT)
                                    destFile.writeBytes(decodedBytes)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                
                val lastAccessed = plantObj.optLong("lastAccessed", System.currentTimeMillis())
                val createdAt = plantObj.optLong("createdAt", System.currentTimeMillis())
                
                val plant = Plant(
                    id = 0,
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
                    lastAccessed = lastAccessed,
                    createdAt = createdAt
                )
                list.add(plant)
            }
            return Result.success(list)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        }
    }

    suspend fun importBackup(
        context: Context,
        uri: Uri,
        plantDao: PlantDao
    ): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    return@withContext Result.failure(Exception("Cannot open file stream"))
                }
                
                val bytes = inputStream.readBytes()
                inputStream.close()
                
                val jsonStr = String(bytes, Charsets.UTF_8)
                val rootObj = JSONObject(jsonStr)
                
                if (!rootObj.has("plants")) {
                    return@withContext Result.failure(Exception("Invalid backup file: 'plants' key not found."))
                }
                
                val plantsArray = rootObj.getJSONArray("plants")
                val converters = Converters()
                var importedCount = 0
                
                for (i in 0 until plantsArray.length()) {
                    val plantObj = plantsArray.getJSONObject(i)
                    
                    val name = plantObj.optString("name", "Unnamed Plant")
                    val category = plantObj.optString("category", "General")
                    val summary = plantObj.optString("summary", "")
                    val about = plantObj.optString("about", "")
                    val image = plantObj.optString("image", "")
                    val growingInstructions = plantObj.optString("growingInstructions", "")
                    val careTips = plantObj.optString("careTips", "")
                    val sicknessInfo = plantObj.optString("sicknessInfo", "")
                    val remedies = plantObj.optString("remedies", "")
                    
                    val issuesArray = plantObj.optJSONArray("healthIssues")
                    val healthIssues = if (issuesArray != null) {
                        converters.stringToIssues(issuesArray.toString())
                    } else {
                        emptyList()
                    }
                    
                    val pdfsArray = plantObj.optJSONArray("pdfs")
                    val pdfs = mutableListOf<PlantPdf>()
                    if (pdfsArray != null) {
                        for (j in 0 until pdfsArray.length()) {
                            try {
                                val pdfObj = pdfsArray.getJSONObject(j)
                                val pName = pdfObj.optString("name", "")
                                val pFilename = pdfObj.optString("filename", "")
                                if (pName.isNotEmpty() && pFilename.isNotEmpty()) {
                                    pdfs.add(PlantPdf(name = pName, filename = pFilename))
                                    
                                    val contentBase64 = pdfObj.optString("contentBase64", "")
                                    if (contentBase64.isNotEmpty()) {
                                        val destFile = File(context.filesDir, pFilename)
                                        val decodedBytes = android.util.Base64.decode(contentBase64, android.util.Base64.DEFAULT)
                                        destFile.writeBytes(decodedBytes)
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    
                    val lastAccessed = plantObj.optLong("lastAccessed", System.currentTimeMillis())
                    val createdAt = plantObj.optLong("createdAt", System.currentTimeMillis())
                    
                    val plant = Plant(
                        id = 0,
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
                        lastAccessed = lastAccessed,
                        createdAt = createdAt
                    )
                    
                    plantDao.insertPlant(plant)
                    importedCount++
                }
                
                Result.success(importedCount)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}
