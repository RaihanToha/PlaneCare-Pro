package com.example.i18n

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Language {
    EN, BN
}

object I18nManager {
    private const val PREFS_NAME = "plantcare_prefs"
    private const val KEY_LANG = "selected_language"
    
    private val _currentLanguage = MutableStateFlow(Language.EN)
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()
    
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedLang = prefs.getString(KEY_LANG, Language.EN.name) ?: Language.EN.name
        _currentLanguage.value = try {
            Language.valueOf(savedLang)
        } catch (e: Exception) {
            Language.EN
        }
    }

    fun toggleLanguage() {
        val nextLang = if (_currentLanguage.value == Language.EN) Language.BN else Language.EN
        _currentLanguage.value = nextLang
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_LANG, nextLang.name).apply()
        }
    }

    fun setLanguage(lang: Language) {
        _currentLanguage.value = lang
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_LANG, lang.name).apply()
        }
    }

    // Translation mapping
    private val translations = mapOf(
        "welcome" to mapOf(Language.EN to "Welcome Back!", Language.BN to "স্বাগতম!"),
        "subtitle" to mapOf(Language.EN to "Your garden is thriving today.", Language.BN to "আপনার বাগান আজ ফুলে-ফেঁপে উঠছে।"),
        "categories" to mapOf(Language.EN to "Categories", Language.BN to "ক্যাটাগরি"),
        "cat_all" to mapOf(Language.EN to "All", Language.BN to "সব"),
        "cat_fruits" to mapOf(Language.EN to "Fruits", Language.BN to "ফল"),
        "cat_vegetables" to mapOf(Language.EN to "Vegetables", Language.BN to "শাকসবজি"),
        "cat_flowers" to mapOf(Language.EN to "Flowers", Language.BN to "ফুল"),
        "cat_herbs" to mapOf(Language.EN to "Herbs", Language.BN to "ভেষজ"),
        "search_dash" to mapOf(Language.EN to "Search plants...", Language.BN to "গাছ খুঁজুন..."),
        "add_plant" to mapOf(Language.EN to "Add Plant", Language.BN to "নতুন গাছ যুক্ত করুন"),
        "edit_plant" to mapOf(Language.EN to "Edit Plant", Language.BN to "গাছ এডিট করুন"),
        "all_plants" to mapOf(Language.EN to "All Plants", Language.BN to "সব গাছপালা"),
        "no_plants" to mapOf(Language.EN to "No plants found. Click '+' to add one!", Language.BN to "কোন গাছ পাওয়া যায়নি। নতুন গাছ যুক্ত করতে '+' চাপুন!"),
        "total_plants" to mapOf(Language.EN to "Total Plants", Language.BN to "মোট গাছপালা"),
        "recently_accessed" to mapOf(Language.EN to "Recently Accessed", Language.BN to "সম্প্রতি দেখা হয়েছে"),
        "no_recent" to mapOf(Language.EN to "No recently accessed plants.", Language.BN to "সম্প্রতি কোনো গাছ দেখা হয়নি।"),
        "category" to mapOf(Language.EN to "Category", Language.BN to "ক্যাটাগরি"),
        "plant_name" to mapOf(Language.EN to "Plant Name", Language.BN to "গাছের নাম"),
        "summary" to mapOf(Language.EN to "Short Summary", Language.BN to "সংক্ষিপ্ত তথ্য"),
        "about" to mapOf(Language.EN to "About", Language.BN to "গাছ সম্পর্কে"),
        "growing_instructions" to mapOf(Language.EN to "Growing Instructions", Language.BN to "রোপণ ও বৃদ্ধির নিয়ম"),
        "care_tips" to mapOf(Language.EN to "Care Tips", Language.BN to "যত্ন নেওয়ার টিপস"),
        "sickness" to mapOf(Language.EN to "Common Diseases", Language.BN to "সাধারণ রোগবালাই"),
        "remedies" to mapOf(Language.EN to "Remedies", Language.BN to "প্রতিকার"),
        "health_issues" to mapOf(Language.EN to "Custom Health Log", Language.BN to "স্বাস্থ্যের সমস্যা ও সমাধান"),
        "save" to mapOf(Language.EN to "Save Plant", Language.BN to "সেভ করুন"),
        "update" to mapOf(Language.EN to "Update Plant", Language.BN to "আপডেট করুন"),
        "delete" to mapOf(Language.EN to "Delete", Language.BN to "মুছে ফেলুন"),
        "add_issue" to mapOf(Language.EN to "Add Custom Issue", Language.BN to "নতুন সমস্যা যোগ করুন"),
        "issue_name" to mapOf(Language.EN to "Issue Name (e.g. Yellowing Leaves)", Language.BN to "সমস্যার নাম (যেমন: হলুদ পাতা)"),
        "remedy_desc" to mapOf(Language.EN to "Remedy (e.g. Reduce watering)", Language.BN to "সমাধান (যেমন: পানি দেওয়া কমিয়ে দিন)"),
        "tab_about" to mapOf(Language.EN to "About", Language.BN to "পরিচিতি"),
        "tab_growing" to mapOf(Language.EN to "Growing", Language.BN to "রোপন"),
        "tab_care" to mapOf(Language.EN to "Care", Language.BN to "যত্ন"),
        "tab_health" to mapOf(Language.EN to "Health", Language.BN to "স্বাস্থ্য"),
        "upload_photo" to mapOf(Language.EN to "Tap to attach plant photo", Language.BN to "গাছের ছবি সংযুক্ত করতে ট্যাপ করুন"),
        "photo_attached" to mapOf(Language.EN to "Plant Photo Attached", Language.BN to "ছবি সংযুক্ত করা হয়েছ"),
        "add_leaf_photo" to mapOf(Language.EN to "Attach Issue Photo (Optional)", Language.BN to "সমস্যার ছবি সংযুক্ত করুন (ঐচ্ছিক)"),
        "no_health_info" to mapOf(Language.EN to "No health issues recorded yet.", Language.BN to "কোন স্বাস্থ্যের সমস্যা রেকর্ড করা হয়নি।"),
        "validation_error" to mapOf(Language.EN to "Please enter a valid plant name and category!", Language.BN to "দয়া করে একটি সঠিক গাছের নাম এবং ক্যাটাগরি লিখুন!"),
        "confirm_delete" to mapOf(Language.EN to "Are you sure you want to delete this plant?", Language.BN to "আপনি কি নিশ্চিত যে এই গাছটি মুছে ফেলতে চান?"),
        "back" to mapOf(Language.EN to "Back", Language.BN to "পেছনে"),
        "custom_issues" to mapOf(Language.EN to "Custom Issues Added", Language.BN to "যুক্ত করা কাস্টম সমস্যাসমূহ"),
        "issue" to mapOf(Language.EN to "Issue", Language.BN to "সমস্যা"),
        "remedy" to mapOf(Language.EN to "Remedy", Language.BN to "সমাধান")
    )

    fun getString(key: String): String {
        return translations[key]?.get(_currentLanguage.value) ?: key
    }

    fun getStringFor(key: String, lang: Language): String {
        return translations[key]?.get(lang) ?: key
    }
}
