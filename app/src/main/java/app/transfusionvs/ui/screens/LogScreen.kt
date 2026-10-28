package app.transfusionvs.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.transfusionvs.data.LogRow
import app.transfusionvs.data.TransfusionStatus
import app.transfusionvs.schedule.Bucket
import app.transfusionvs.schedule.CsvFormat
import app.transfusionvs.schedule.Labels
import app.transfusionvs.schedule.Period
import app.transfusionvs.schedule.Stats
import app.transfusionvs.schedule.Summary
import app.transfusionvs.schedule.Timeliness
import app.transfusionvs.schedule.asCheckpoint
import app.transfusionvs.ui.CsvExport
import app.transfusionvs.ui.MainViewModel
import app.transfusionvs.ui.components.CapsLabel
import app.transfusionvs.ui.components.ChoiceChip
import app.transfusionvs.ui.components.Panel
import app.transfusionvs.ui.components.StatusPill
import app.transfusionvs.ui.components.TopBar
import app.transfusionvs.ui.theme.TimeFigures
import app.transfusionvs.ui.theme.Tvs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

/** Per day / week / month: % on time, average delay, missed count; the checkpoints; CSV; delete all. */
@Composable
fun LogScreen(vm: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val rows by vm.log.collectAsStateWithLifecycle()
    val home by vm.home.collectAsStateWithLifecycle()
    var period by rememberSaveable { mutableStateOf(Period.DAY) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }

    val tolerance = vm.toleranceMin
    val zone = ZoneId.systemDefault()
    val firstDay = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val buckets = remember(rows, period, tolerance) { Stats.buckets(rows.orEmpty(), period, tolerance, zone, firstDay) }
    val current = buckets.firstOrNull { it.start.toString() == selected } ?: buckets.firstOrNull()

    Column(Modifier.fillMaxSize().background(Tvs.Panel)) {
        TopBar(caps = "Log & stats", title = "Documentation on time", onBack = onBack)

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Period.DAY to "Day", Period.WEEK to "Week", Period.MONTH to "Month").forEach { (p, label) ->
                        ChoiceChip(label, selected = period == p, onClick = { period = p; selected = null }, modifier = Modifier.weight(1f))
                    }
                }
            }

            if (current == null) {
                item {
                    Panel {
                        Text(
                            if (rows == null) "Loading…" else "Nothing logged yet. Checkpoints appear here once they are acknowledged or missed.",
                            color = Tvs.Muted, fontSize = 16.sp,
                        )
                    }
                }
            } else {
                // Pick the day / week / month
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(buckets, key = { it.start.toString() }) { b ->
                            BucketChip(b, selected = b.start == current.start, onClick = { selected = b.start.toString() })
                        }
                    }
                }
                item { SummaryPanel(current.label, current.summary, tolerance) }
                item { CapsLabel("Checkpoints · ${current.label}", Tvs.Navy, Modifier.padding(top = 8.dp)) }
                items(current.rows, key = { it.id }) { r -> LogLine(r, tolerance, showDate = period != Period.DAY) }
            }

            item {
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            val all = rows.orEmpty()
                            if (all.isNotEmpty() && !exporting) {
                                exporting = true
                                scope.launch {
                                    val (uri, name) = withContext(Dispatchers.IO) {
                                        CsvExport.write(context, CsvFormat.build(all, tolerance, zone))
                                    }
                                    CsvExport.share(context, uri, name)
                                    exporting = false
                                }
                            }
                        },
                        enabled = !rows.isNullOrEmpty(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Tvs.Navy, contentColor = Tvs.White),
                    ) { Text("Export CSV (share)", fontSize = 17.sp, fontWeight = FontWeight.Black) }
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) { Text("Delete all data", color = Tvs.Red, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
                }
            }
        }
    }

    if (confirmDelete) {
        val running = home.orEmpty().count { it.transfusion.status == TransfusionStatus.RUNNING }
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete all data?") },
            text = {
                Text(
                    "Every transfusion and the whole log on this phone are erased. This cannot be undone." +
                        if (running > 0) "\n\n$running transfusion${if (running > 1) "s are" else " is"} still running — " +
                            "${if (running > 1) "their" else "its"} alarms will stop." else "",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    selected = null
                    vm.deleteAllData()
                }) { Text("Delete everything", color = Tvs.Red, fontWeight = FontWeight.Black) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun BucketChip(b: Bucket, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.heightIn(min = 56.dp)
            .background(if (selected) Tvs.Navy else Tvs.White, shape)
            .border(1.5.dp, if (selected) Tvs.Navy else Tvs.Outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        val fg = if (selected) Tvs.White else Tvs.Navy
        Text(b.label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
            b.summary.onTimePct?.let { "$it% on time" } ?: "—",
            color = if (selected) Tvs.White.copy(alpha = 0.85f) else Tvs.Muted, fontSize = 13.sp,
        )
    }
}

@Composable
private fun SummaryPanel(label: String, s: Summary, tolerance: Int) {
    Panel {
        CapsLabel(label, Tvs.Navy)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile("On time", s.onTimePct?.let { "$it%" } ?: "—", Tvs.Navy, Modifier.weight(1f))
            Tile("Avg delay", s.avgDelayMin?.let { "%.1f min".format(Locale.ENGLISH, it) } ?: "—", Tvs.Blue, Modifier.weight(1f))
            Tile("Missed", "${s.missed}", if (s.missed > 0) Tvs.Red else Tvs.Green, Modifier.weight(1f))
        }
        Text(
            "${s.counted} checks: ${s.onTime} on time · ${s.late} late · ${s.missed} missed. On time = within ±$tolerance min.",
            color = Tvs.Muted, fontSize = 14.sp,
        )
    }
}

@Composable
private fun Tile(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.background(Tvs.Panel, RoundedCornerShape(12.dp)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = TimeFigures, color = color, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text(title, color = Tvs.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

private val dateTime = DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.ENGLISH)

@Composable
private fun LogLine(r: LogRow, tolerance: Int, showDate: Boolean) {
    Row(
        Modifier.fillMaxWidth().background(Tvs.White, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (showDate) Instant.ofEpochMilli(r.scheduledAt).atZone(ZoneId.systemDefault()).format(dateTime) else Labels.clock(r.scheduledAt),
            style = TimeFigures, color = Tvs.Navy, fontSize = 15.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.width(if (showDate) 104.dp else 56.dp),
        )
        BedDot(r.colorIndex, 12.dp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(r.bedLabel, color = Tvs.Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(Labels.title(r.type, r.offsetMin), color = Tvs.Muted, fontSize = 13.sp)
        }
        StatusPill(Timeliness.status(r.asCheckpoint(), System.currentTimeMillis(), tolerance))
    }
}
