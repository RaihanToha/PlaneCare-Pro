package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserSettings {
    private const val PREFS_NAME = "usersettings_prefs"
    private const val KEY_DARK_MODE = "dark_mode_enabled"
    private const val KEY_NICKNAME = "user_nickname"
    private const val KEY_ACCOUNT_NAME = "user_account_name"
    private const val KEY_PROFILE_PHOTO = "user_profile_photo"
    
    // Firebase credentials and session keys
    private const val KEY_FIREBASE_API_KEY = "firebase_api_key"
    private const val KEY_FIREBASE_PROJECT_ID = "firebase_project_id"
    private const val KEY_USER_EMAIL = "firebase_user_email"
    private const val KEY_USER_TOKEN = "firebase_user_token"
    private const val KEY_USER_ID = "firebase_user_id"
    private const val KEY_IS_LOGGED_IN = "firebase_is_logged_in"
    private const val KEY_IS_ADMIN = "is_admin"
    
    private lateinit var prefs: SharedPreferences
    
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()
    
    private val _nickname = MutableStateFlow<String?>(null)
    val nickname: StateFlow<String?> = _nickname.asStateFlow()

    private val _accountName = MutableStateFlow<String?>(null)
    val accountName: StateFlow<String?> = _accountName.asStateFlow()

    private val _profilePhoto = MutableStateFlow<String?>(null)
    val profilePhoto: StateFlow<String?> = _profilePhoto.asStateFlow()
    
    // Firebase StateFlows
    private val _firebaseApiKey = MutableStateFlow("")
    val firebaseApiKey: StateFlow<String> = _firebaseApiKey.asStateFlow()

    private val _firebaseProjectId = MutableStateFlow("")
    val firebaseProjectId: StateFlow<String> = _firebaseProjectId.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _userToken = MutableStateFlow<String?>(null)
    val userToken: StateFlow<String?> = _userToken.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isDarkMode.value = prefs.getBoolean(KEY_DARK_MODE, false)
        _nickname.value = prefs.getString(KEY_NICKNAME, null)
        _accountName.value = prefs.getString(KEY_ACCOUNT_NAME, null)
        _profilePhoto.value = prefs.getString(KEY_PROFILE_PHOTO, null)
        
        // Load firebase credentials securely without hardcoded secrets
        val savedApiKey = prefs.getString(KEY_FIREBASE_API_KEY, "") ?: ""
        val buildConfigApiKey = try {
            val field = com.example.BuildConfig::class.java.getField("FIREBASE_API_KEY")
            field.get(null) as? String ?: ""
        } catch (_: Exception) {
            ""
        }
        val resKeyId = context.resources.getIdentifier("google_api_key", "string", context.packageName)
        val resApiKey = if (resKeyId != 0) {
            try { context.getString(resKeyId) } catch (_: Exception) { "" }
        } else {
            ""
        }
        _firebaseApiKey.value = when {
            savedApiKey.isNotEmpty() -> savedApiKey
            buildConfigApiKey.isNotEmpty() && buildConfigApiKey != "DEFAULT_FIREBASE_API_KEY" -> buildConfigApiKey
            resApiKey.isNotEmpty() -> resApiKey
            else -> ""
        }

        val savedProjId = prefs.getString(KEY_FIREBASE_PROJECT_ID, "") ?: ""
        val buildConfigProjId = try {
            val field = com.example.BuildConfig::class.java.getField("FIREBASE_PROJECT_ID")
            field.get(null) as? String ?: ""
        } catch (_: Exception) {
            ""
        }
        val resProjId = context.resources.getIdentifier("project_id", "string", context.packageName)
        val resProjectId = if (resProjId != 0) {
            try { context.getString(resProjId) } catch (_: Exception) { "" }
        } else {
            ""
        }
        _firebaseProjectId.value = when {
            savedProjId.isNotEmpty() -> savedProjId
            buildConfigProjId.isNotEmpty() -> buildConfigProjId
            resProjectId.isNotEmpty() -> resProjectId
            else -> "plantcare-pro-2701c"
        }
        _userEmail.value = prefs.getString(KEY_USER_EMAIL, null)
        _userToken.value = prefs.getString(KEY_USER_TOKEN, null)
        _userId.value = prefs.getString(KEY_USER_ID, null)
        _isLoggedIn.value = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        _isAdmin.value = prefs.getBoolean(KEY_IS_ADMIN, false)
        loadLastAccessedForCurrentUser()
    }
    
    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        if (::prefs.isInitialized) {
            prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        }
    }
    
    fun toggleDarkMode() {
        setDarkMode(!_isDarkMode.value)
    }
    
    fun setNickname(name: String) {
        val trimmed = name.trim()
        _nickname.value = if (trimmed.isEmpty()) null else trimmed
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_NICKNAME, if (trimmed.isEmpty()) null else trimmed).apply()
        }
    }

    fun setAccountName(name: String) {
        val trimmed = name.trim()
        _accountName.value = if (trimmed.isEmpty()) null else trimmed
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_ACCOUNT_NAME, if (trimmed.isEmpty()) null else trimmed).apply()
        }
    }

    fun setProfilePhoto(base64: String?) {
        _profilePhoto.value = base64
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_PROFILE_PHOTO, base64).apply()
        }
    }
    
    fun setFirebaseConfig(apiKey: String, projectId: String) {
        _firebaseApiKey.value = apiKey.trim()
        _firebaseProjectId.value = projectId.trim()
        if (::prefs.isInitialized) {
            prefs.edit()
                .putString(KEY_FIREBASE_API_KEY, apiKey.trim())
                .putString(KEY_FIREBASE_PROJECT_ID, projectId.trim())
                .apply()
        }
    }
    
    fun setLoginSession(email: String, token: String, id: String, isAdmin: Boolean = false) {
        _userEmail.value = email.trim()
        _userToken.value = token
        _userId.value = id
        _isLoggedIn.value = true
        _isAdmin.value = isAdmin
        if (::prefs.isInitialized) {
            prefs.edit()
                .putString(KEY_USER_EMAIL, email.trim())
                .putString(KEY_USER_TOKEN, token)
                .putString(KEY_USER_ID, id)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putBoolean(KEY_IS_ADMIN, isAdmin)
                .apply()
            loadLastAccessedForCurrentUser()
        }
    }
    
    fun setAdmin(isAdmin: Boolean) {
        _isAdmin.value = isAdmin
        if (::prefs.isInitialized) {
            prefs.edit()
                .putBoolean(KEY_IS_ADMIN, isAdmin)
                .apply()
        }
    }
    
    private val _lastAccessedPlantId = MutableStateFlow<Int?>(null)
    val lastAccessedPlantId: StateFlow<Int?> = _lastAccessedPlantId.asStateFlow()

    fun setLastAccessedPlantId(plantId: Int) {
        val currentUser = _userId.value ?: "guest"
        _lastAccessedPlantId.value = plantId
        if (::prefs.isInitialized) {
            prefs.edit().putInt("last_accessed_$currentUser", plantId).apply()
        }
    }
    
    private fun loadLastAccessedForCurrentUser() {
        if (::prefs.isInitialized) {
            val currentUser = _userId.value ?: "guest"
            val id = prefs.getInt("last_accessed_$currentUser", -1)
            _lastAccessedPlantId.value = if (id != -1) id else null
        }
    }
    
    fun clearLoginSession() {
        _userEmail.value = null
        _userToken.value = null
        _userId.value = null
        _isLoggedIn.value = false
        _isAdmin.value = false
        if (::prefs.isInitialized) {
            prefs.edit()
                .remove(KEY_USER_EMAIL)
                .remove(KEY_USER_TOKEN)
                .remove(KEY_USER_ID)
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .putBoolean(KEY_IS_ADMIN, false)
                .apply()
            loadLastAccessedForCurrentUser()
        }
    }
    
}
