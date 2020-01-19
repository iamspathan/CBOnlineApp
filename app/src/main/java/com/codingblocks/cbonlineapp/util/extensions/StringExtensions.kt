package com.codingblocks.cbonlineapp.util.extensions

import android.text.SpannableStringBuilder
import androidx.core.text.bold
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.NoSuchElementException

fun folderSize(directory: File): Long {
    var length: Long = 0
    for (file in directory.listFiles()) {
        length += if (file.isFile)
            file.length()
        else
            folderSize(file)
    }
    return length
}

fun Long.readableFileSize(): String {
    if (this <= 0) return "0 MB"
    val units = arrayOf("B", "kB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(this.toDouble()) / Math.log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(
        this / Math.pow(
            1024.0,
            digitGroups.toDouble()
        )
    ) + " " + units[digitGroups]
}

fun String.greater(): Boolean {
    return this.toLong() >= (System.currentTimeMillis() / 1000)
}

fun Long.getDurationBreakdown(): String {
    if (this <= 0) {
        return "---"
    }
    var millis = this
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    millis -= TimeUnit.HOURS.toMillis(hours)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)

    val sb = StringBuilder(64)
    sb.append(hours)
    sb.append(" Hours ")
    sb.append(minutes)
    sb.append(" Mins ")
    return (sb.toString())
}

fun formatDate(date: String): String {
    var format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    if (date.isEmpty()) {
        throw NoSuchElementException("Invalid Date")
    }
    val newDate = format.parse(date)
    val calender = Calendar.getInstance()
    calender.time = newDate
    calender.add(Calendar.HOUR, 5)
    calender.add(Calendar.MINUTE, 30)
    format = SimpleDateFormat("MMM dd yyyy hh:mm a", Locale.US)
    return format.format(calender.time)
}

fun String.isotomillisecond(): Long {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    format.timeZone = TimeZone.getTimeZone("UTC")
    val newDate = format.parse(this)
    return newDate.time
}

fun secToTime(time: Double): String {
    val sec = time.toInt()
    val seconds = sec % 60
    var minutes = sec / 60
    if (minutes >= 60) {
        val hours = minutes / 60
        minutes %= 60
        if (hours >= 24) {
            val days = hours / 24
            return String.format("%d days %02d:%02d:%02d", days, hours % 24, minutes, seconds)
        }
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
    return String.format("00:%02d:%02d", minutes, seconds)
}

fun getDateForTime(time: String): String {
    val dateFormat = SimpleDateFormat("dd-MMM-yy", Locale.US)
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = time.toLong() * 1000

    return dateFormat.format(calendar.time)
}

fun getDate(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = System.currentTimeMillis()

    return dateFormat.format(calendar.time)
}

fun getSpannableSring(normalText: String, boldText: String): SpannableStringBuilder =
    SpannableStringBuilder()
        .append(normalText)
        .bold { append(boldText) }

fun timeAgo(time: Long): String {
    val diff = Math.floor(((System.currentTimeMillis() - time) / 1000).toDouble())
    var interval = Math.floor(diff / 31536000).toInt()
    if (interval >= 1) {
        return "$interval Years Ago"
    }
    interval = Math.floor(diff / 2592000).toInt()
    if (interval >= 1) {
        return "$interval Months Ago"
    }
    interval = Math.floor(diff / 604800).toInt()
    if (interval >= 1) {
        return "$interval Weeks Ago"
    }
    interval = Math.floor(diff / 86400).toInt()
    if (interval >= 1) {
        return "$interval Days Ago"
    }
    interval = Math.floor(diff / 3600).toInt()
    if (interval >= 1) {
        return "$interval Hours Ago"
    }
    interval = Math.floor(diff / 60).toInt()
    if (interval >= 1) {
        return "$interval Minutes Ago"
    }
    return "Just Now"
}
