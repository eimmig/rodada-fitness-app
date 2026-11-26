package br.edu.utfpr.rodada.data

import android.content.Context

/** Small on-device preferences (focused group, permission rationale already shown). */
class Preferences(context: Context) {
    private val prefs = context.getSharedPreferences("rodada", Context.MODE_PRIVATE)

    var focusedGroupId: String?
        get() = prefs.getString("focused_group", null)
        set(value) = prefs.edit().putString("focused_group", value).apply()

    var permissionsExplained: Boolean
        get() = prefs.getBoolean("permissions_explained", false)
        set(value) = prefs.edit().putBoolean("permissions_explained", value).apply()
}
