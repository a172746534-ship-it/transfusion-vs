package app.transfusionvs.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import app.transfusionvs.R
import app.transfusionvs.alarm.Notifications
import app.transfusionvs.app
import app.transfusionvs.data.Settings
import app.transfusionvs.data.TurnDao
import app.transfusionvs.data.TvsDao
import app.transfusionvs.schedule.BedMask
import app.transfusionvs.schedule.BedType
import app.transfusionvs.schedule.BedView
import app.transfusionvs.schedule.BedViews
import app.transfusionvs.schedule.Labels
import app.transfusionvs.schedule.Urgency
import kotlinx.coroutines.launch

/**
 * Home-screen widget: each bed's next check as a CLOCK TIME (widgets can't refresh every second, so
 * no live countdowns). Refreshed by the engine whenever anything changes, so an acknowledge from
 * anywhere shows at once. A due check can be acknowledged from here.
 */
class BedsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val app = context.app
        val pending = goAsync()
        app.scope.launch {
            try {
                BedsWidget.refresh(context, app.db.dao(), app.db.turnDao(), app.settings)
            } finally {
                pending.finish()
            }
        }
    }
}

object BedsWidget {
    private const val MAX_ROWS = 6

    suspend fun refresh(context: Context, dao: TvsDao, turnDao: TurnDao, settings: Settings) {
        runCatching {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, BedsWidgetProvider::class.java))
            if (ids.isEmpty()) return
            manager.updateAppWidget(ids, build(context, beds(dao, turnDao, settings), System.currentTimeMillis()))
        }
    }

    private suspend fun beds(dao: TvsDao, turnDao: TurnDao, settings: Settings): List<BedView> {
        val now = System.currentTimeMillis()
        return masked(
            BedViews.sorted(
                dao.homeOnce().map { BedViews.of(it, now) } + turnDao.homeOnce().map { BedViews.of(it, now, settings.missedTimeoutMin) },
            ).filter { !it.idle },
            dao.bedLabels() + turnDao.bedLabels(),
        )
    }

    /** The widget is outside the app and shows on the home screen: bed labels go through the mask. */
    fun masked(beds: List<BedView>, active: Collection<String>): List<BedView> {
        val mask = BedMask.masker(active)
        return beds.map { it.copy(bed = mask(it.bed)) }
    }

    /** Also used by the emulator test that renders the widget to an image. */
    fun build(context: Context, beds: List<BedView>, now: Long): RemoteViews {
        val root = RemoteViews(context.packageName, R.layout.widget_beds)
        root.setTextViewText(R.id.widget_title, BedViews.summary(beds))
        root.setTextViewText(R.id.widget_updated, "as of ${Labels.clock(now)}")
        root.setOnClickPendingIntent(R.id.widget_root, Notifications.openApp(context))
        root.removeAllViews(R.id.widget_rows)
        beds.take(MAX_ROWS).forEach { root.addView(R.id.widget_rows, row(context, it)) }
        val more = beds.size - MAX_ROWS
        root.setViewVisibility(R.id.widget_more, if (more > 0) View.VISIBLE else View.GONE)
        if (more > 0) root.setTextViewText(R.id.widget_more, "+$more more")
        root.setViewVisibility(R.id.widget_empty, if (beds.isEmpty()) View.VISIBLE else View.GONE)
        return root
    }

    private fun row(context: Context, bed: BedView): RemoteViews {
        val r = RemoteViews(context.packageName, R.layout.widget_row)
        val mark = when {
            bed.stoppedForReaction -> "◆ "
            bed.urgency == Urgency.OVERDUE -> "◆ "
            bed.urgency == Urgency.NOW -> "● "
            else -> ""
        }
        r.setTextViewText(R.id.row_time, mark + (bed.time?.let { Labels.clock(it) } ?: "—"))
        r.setImageViewResource(R.id.row_icon, if (bed.type == BedType.TRANSFUSION) R.drawable.ic_drop else R.drawable.ic_turn)
        r.setTextViewText(R.id.row_bed, bed.bed)
        r.setTextViewText(R.id.row_what, stateWord(bed) + bed.what)
        val alert = bed.stoppedForReaction || bed.urgency == Urgency.OVERDUE
        r.setTextColor(R.id.row_time, context.getColor(if (alert) R.color.widget_alert else R.color.widget_ink))
        val canAck = bed.ackIds.isNotEmpty() && !bed.stoppedForReaction
        r.setViewVisibility(R.id.row_ack, if (canAck) View.VISIBLE else View.GONE)
        if (canAck) r.setOnClickPendingIntent(R.id.row_ack, Notifications.ackIntent(context, bed.ackIds))
        r.setOnClickPendingIntent(R.id.row_root, if (bed.roundOpen) Notifications.alarmScreen(context) else Notifications.openApp(context))
        return r
    }

    private fun stateWord(bed: BedView) = when {
        bed.stoppedForReaction -> "STOPPED · "
        bed.urgency == Urgency.OVERDUE -> "OVERDUE · "
        bed.urgency == Urgency.NOW -> "DUE NOW · "
        else -> ""
    }
}
