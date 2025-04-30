package com.example.budgetbuddy.util

import android.text.format.DateUtils as AndroidDateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    /**
     * Checks if two Calendar instances represent the same day (ignoring time).
     */
    fun isSameDay(cal1: Calendar, cal2: Calendar?): Boolean {
        if (cal2 == null) return false
        return cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
               cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Returns a relative date string like "Today", "Yesterday", or a formatted date.
     */
    fun getRelativeDateString(calendar: Calendar): String {
        val now = Calendar.getInstance()
        return when {
            isSameDay(calendar, now) -> "Today"
            isSameDay(calendar, now.apply { add(Calendar.DAY_OF_YEAR, -1) }) -> "Yesterday"
            else -> {
                // Format as "MMM dd" (e.g., "Oct 10") or include year if not current year
                val format = if (calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
                    SimpleDateFormat("MMM dd", Locale.getDefault())
                } else {
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                }
                format.format(calendar.time)
            }
        }
    }
} 