package app.transfusionvs.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Writes the CSV to the app's private cache and hands it to the Android share sheet. */
object CsvExport {

    /** Call off the main thread. Keeps only the latest export on the phone. */
    fun write(context: Context, csv: String): Pair<Uri, String> {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val name = "transfusion-vs-log-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".csv"
        val file = File(dir, name)
        file.writeText("﻿" + csv) // BOM: Excel then reads Arabic bed labels correctly
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file) to name
    }

    fun share(context: Context, uri: Uri, name: String) {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/csv")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, "Transfusion VS log")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // explicit grant (required from Android 18)
        send.clipData = ClipData.newRawUri(name, uri)
        try {
            context.startActivity(Intent.createChooser(send, "Export log (CSV)"))
        } catch (e: ActivityNotFoundException) {
            // No app can receive a file — nothing to do.
        }
    }
}
