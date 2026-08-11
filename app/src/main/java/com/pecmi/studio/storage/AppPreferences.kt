package com.pecmi.studio.storage

import android.content.Context
import android.content.SharedPreferences

enum class ThemeMode(val code: String, val displayNameAr: String, val displayNameEn: String) {
    SYSTEM("SYSTEM", "افتراضي النظام", "System Default"),
    LIGHT("LIGHT", "فاتح", "Light Mode"),
    DARK("DARK", "داكن", "Dark Mode")
}

enum class AppLanguage(val code: String, val nativeName: String, val isRtl: Boolean) {
    ARABIC("ar", "العربية", true),
    ENGLISH("en", "English", false),
    FRENCH("fr", "Français", false),
    SPANISH("es", "Español", false),
    TURKISH("tr", "Türkçe", false),
    GERMAN("de", "Deutsch", false),
    ITALIAN("it", "Italiano", false),
    RUSSIAN("ru", "Русский", false),
    PORTUGUESE("pt", "Português", false),
    INDONESIAN("id", "Bahasa Indonesia", false),
    CHINESE("zh", "简体中文", false);

    companion object {
        fun fromCode(code: String): AppLanguage {
            val normalizedCode = code.lowercase()
            return entries.find {
                it.code.equals(normalizedCode, ignoreCase = true) ||
                (it == CHINESE && normalizedCode.startsWith("zh")) ||
                (it == INDONESIAN && (normalizedCode == "in" || normalizedCode == "id"))
            } ?: ENGLISH
        }
    }
}

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pecmi_settings", Context.MODE_PRIVATE)

    var isFirstRun: Boolean
        get() = prefs.getBoolean("is_first_run", true)
        set(value) = prefs.edit().putBoolean("is_first_run", value).apply()

    var hasAcceptedConsent: Boolean
        get() = prefs.getBoolean("has_accepted_consent", false)
        set(value) = prefs.edit().putBoolean("has_accepted_consent", value).apply()

    var themeMode: ThemeMode
        get() {
            val saved = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
            return try {
                ThemeMode.valueOf(saved)
            } catch (e: Exception) {
                ThemeMode.SYSTEM
            }
        }
        set(value) {
            prefs.edit().putString("theme_mode", value.name).apply()
        }

    val hasUserSetLanguage: Boolean
        get() = prefs.contains("app_language")

    var language: AppLanguage
        get() {
            if (!prefs.contains("app_language")) {
                val sysLocale = java.util.Locale.getDefault()
                val sysLang = sysLocale.language.lowercase()
                val sysTag = sysLocale.toLanguageTag().lowercase()
                val isChinese = sysLang.startsWith("zh") || sysTag.startsWith("zh")
                return if (isChinese) {
                    AppLanguage.CHINESE
                } else {
                    AppLanguage.ENGLISH
                }
            }
            val code = prefs.getString("app_language", null) ?: AppLanguage.ENGLISH.code
            return AppLanguage.fromCode(code)
        }
        set(value) {
            prefs.edit().putString("app_language", value.code).apply()
        }

    var lastEditLayersJson: String?
        get() = prefs.getString("last_edit_layers_json", null)
        set(value) = prefs.edit().putString("last_edit_layers_json", value).apply()

    var lastEditSettingsJson: String?
        get() = prefs.getString("last_edit_settings_json", null)
        set(value) = prefs.edit().putString("last_edit_settings_json", value).apply()

    var lastEditTimestamp: Long
        get() = prefs.getLong("last_edit_timestamp", 0L)
        set(value) = prefs.edit().putLong("last_edit_timestamp", value).apply()
}
