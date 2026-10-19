package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.json.JSONArray
import org.json.JSONObject

data class HealthIssue(
    val issue: String,
    val remedy: String,
    val image: String? = null // Holds Base64 string for issue leaf photo
)

data class PlantPdf(
    val name: String, // Friendly display filename, e.g., "Tomato growing guide"
    val filename: String // Actual filename stored on app disk filesDir, e.g., "pdf_171584...pdf"
)

@Entity(tableName = "plants")
@TypeConverters(Converters::class)
data class Plant(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // Fruits, Vegetables, Flowers, or Herbs
    val summary: String,
    val about: String,
    val image: String, // Base64 data-url from user upload
    val growingInstructions: String,
    val careTips: String,
    val sicknessInfo: String,
    val remedies: String,
    val healthIssues: List<HealthIssue> = emptyList(),
    val pdfs: List<PlantPdf> = emptyList(),
    val lastAccessed: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

class Converters {
    @TypeConverter
    fun stringToIssues(value: String?): List<HealthIssue> {
        if (value.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<HealthIssue>()
        try {
            val array = JSONArray(value)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val issue = obj.optString("issue", "")
                val remedy = obj.optString("remedy", "")
                val img = if (obj.has("image") && !obj.isNull("image")) obj.getString("image") else null
                list.add(HealthIssue(issue = issue, remedy = remedy, image = img))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    @TypeConverter
    fun issuesToString(list: List<HealthIssue>?): String {
        if (list == null) return "[]"
        val array = JSONArray()
        for (item in list) {
            try {
                val obj = JSONObject()
                obj.put("issue", item.issue)
                obj.put("remedy", item.remedy)
                if (item.image != null) {
                    obj.put("image", item.image)
                } else {
                    obj.put("image", JSONObject.NULL)
                }
                array.put(obj)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return array.toString()
    }

    @TypeConverter
    fun stringToPdfs(value: String?): List<PlantPdf> {
        if (value.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<PlantPdf>()
        try {
            val array = JSONArray(value)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.optString("name", "")
                val filename = obj.optString("filename", "")
                list.add(PlantPdf(name = name, filename = filename))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    @TypeConverter
    fun pdfsToString(list: List<PlantPdf>?): String {
        if (list == null) return "[]"
        val array = JSONArray()
        for (item in list) {
            try {
                val obj = JSONObject()
                obj.put("name", item.name)
                obj.put("filename", item.filename)
                array.put(obj)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return array.toString()
    }
}
