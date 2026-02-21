package com.i2medier.financialpro.ui.search

import android.content.Context

object RecentCalculatorStore {
    private const val PREFS = "recent_calculators"
    private const val KEY = "items"
    private const val LIMIT = 8

    fun get(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "")
            .orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split("|").filter { it.isNotBlank() }
    }

    fun getAll(context: Context): List<String> = get(context)

    fun record(context: Context, id: String) = push(context, id)

    fun push(context: Context, id: String) {
        if (id.isBlank()) return
        val list = get(context).toMutableList()
        list.remove(id)
        list.add(0, id)
        while (list.size > LIMIT) list.removeAt(list.lastIndex)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, list.joinToString("|"))
            .apply()
    }
}
