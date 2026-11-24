package app.ht104.control.data

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    /** Wraps a base context with the user-selected language (also sets RTL layout direction for Arabic). */
    fun wrap(base: Context): Context {
        val lang = SettingsStore(base).language
        if (lang == "system") return base
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
