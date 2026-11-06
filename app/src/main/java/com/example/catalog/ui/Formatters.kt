package com.example.catalog.ui

import android.content.Context
import com.example.catalog.R
import com.example.catalog.core.SyncStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Formatowanie tekstów UI (ceny, godziny, status synchronizacji). */
object Formatters {

    fun price(value: Double): String = String.format(Locale.US, "$%.2f", value)

    fun time(millis: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

    /** „1 produkt”, „194 produkty”, „0 produktów”. */
    fun products(context: Context, count: Int): String =
        context.getString(polishPlural(count, R.string.products_one, R.string.products_few, R.string.products_many), count)

    /**
     * Reguły polskiej odmiany liczebników. Liczymy je sami, bo zasoby <plurals>
     * wybierają formę według języka urządzenia (np. na emulatorze po angielsku dałyby „194 produktu”).
     */
    fun <T> polishPlural(count: Int, one: T, few: T, many: T): T {
        val lastTwo = count % 100
        val last = count % 10
        return when {
            count == 1 -> one
            last in 2..4 && lastTwo !in 12..14 -> few
            else -> many
        }
    }

    fun syncStatus(context: Context, status: SyncStatus): String = when (status) {
        is SyncStatus.Idle -> status.lastSync?.let { last ->
            context.getString(
                R.string.sync_idle_last,
                time(last.at),
                products(context, last.count),
            )
        } ?: context.getString(R.string.sync_idle_never)
        SyncStatus.WaitingForNetwork -> context.getString(R.string.sync_waiting)
        SyncStatus.Running -> context.getString(R.string.sync_running)
        is SyncStatus.Retrying -> context.getString(R.string.sync_retrying, status.attempt)
        is SyncStatus.Succeeded -> context.getString(
            R.string.sync_succeeded,
            products(context, status.count),
            time(status.at),
        )
        is SyncStatus.Failed -> context.getString(R.string.sync_failed, status.reason)
    }
}
