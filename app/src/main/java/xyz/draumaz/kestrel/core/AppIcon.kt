package xyz.draumaz.kestrel.core

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import xyz.draumaz.kestrel.BuildConfig
import xyz.draumaz.kestrel.R

/**
 * The launcher icon's colour (Settings → App icon). Each option is an
 * <activity-alias> in the manifest; exactly one is enabled at a time. The
 * default keeps the `.MainActivity` component name the Flutter build used.
 */
enum class AppIcon(val key: String, val label: String, val alias: String, val background: Int, val foreground: Int) {
    VIOLET("violet", "Violet", ".MainActivity", R.drawable.ic_icon_bg_violet, R.drawable.ic_icon_fg_violet),
    OCEAN("ocean", "Ocean", ".IconOcean", R.drawable.ic_icon_bg_ocean, R.drawable.ic_icon_fg_ocean),
    FOREST("forest", "Forest", ".IconForest", R.drawable.ic_icon_bg_forest, R.drawable.ic_icon_fg_forest),
    SUNSET("sunset", "Sunset", ".IconSunset", R.drawable.ic_icon_bg_sunset, R.drawable.ic_icon_fg_sunset),
    MIDNIGHT("midnight", "Midnight", ".IconMidnight", R.drawable.ic_icon_bg_midnight, R.drawable.ic_icon_fg_midnight),
    /** The mark in colour on white, like Google's own app icons. */
    WHITE("white", "White", ".IconWhite", R.drawable.ic_icon_bg_white, R.drawable.ic_icon_fg_white);

    companion object {
        fun parse(key: String?): AppIcon = entries.firstOrNull { it.key == key } ?: VIOLET

        /** Enables [icon]'s launcher entry and disables the others (the app keeps running). */
        fun apply(context: Context, icon: AppIcon) {
            val pm = context.packageManager
            val pkg = context.packageName
            // The aliases are named after the namespace, which differs from
            // the package name in debug builds (applicationIdSuffix ".debug").
            val cls = BuildConfig::class.java.packageName
            // Enable the new one first, so there's never a moment with no launcher entry.
            pm.setComponentEnabledSetting(
                ComponentName(pkg, cls + icon.alias),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
            for (other in entries) if (other != icon) {
                pm.setComponentEnabledSetting(
                    ComponentName(pkg, cls + other.alias),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
    }
}
