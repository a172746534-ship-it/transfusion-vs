package app.transfusionvs.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.transfusionvs.App
import app.transfusionvs.R
import app.transfusionvs.data.CheckpointWithBed
import app.transfusionvs.data.TransfusionStatus
import app.transfusionvs.data.TransfusionWithCheckpoints
import app.transfusionvs.data.TurnPlanStatus
import app.transfusionvs.data.TurnPlanWithSlots
import app.transfusionvs.data.UndoAction
import app.transfusionvs.ui.screens.TurnPlanDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Marks a screen's primary action for the emulator UI checks (bottom third, exactly one). */
const val PRIMARY_TAG = "primary"

/**
 * State for the main screens, straight from the database (single source of truth).
 * Actions run on the app scope so they finish even if the screen closes.
 */
class MainViewModel(private val app: App) : ViewModel() {

    private val dao = app.db.dao()
    private val wardDao = app.db.wardDao()

    /** null until the first load (so screens don't flash "nothing running"). */
    val home: StateFlow<List<TransfusionWithCheckpoints>?> =
        dao.observeHome().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val ringing: StateFlow<List<CheckpointWithBed>> =
        dao.observeRinging().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val anyTransfusion: StateFlow<Boolean> =
        dao.observeAnyTransfusion().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun transfusion(id: Long): Flow<TransfusionWithCheckpoints?> = dao.observeTransfusion(id)

    // ---- Repositioning ----

    private val turnDao = app.db.turnDao()

    /** null until the first load. */
    val turns: StateFlow<List<TurnPlanWithSlots>?> =
        turnDao.observeHome().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun turnPlan(id: Long): Flow<TurnPlanWithSlots?> = turnDao.observePlan(id)

    val turnIntervalMin: Int get() = app.settings.turnIntervalMin
    val turnWindowMin: Int get() = app.settings.turnWindowMin

    fun startTurnPlan(draft: TurnPlanDraft) {
        app.scope.launch { app.engine.startTurnPlan(draft) }
    }

    fun editTurnPlan(id: Long, draft: TurnPlanDraft) {
        app.scope.launch { app.engine.editTurnPlan(id, draft) }
    }

    fun stopTurnPlan(id: Long) {
        app.scope.launch { app.engine.stopTurnPlan(id) }
    }

    fun restartTurnPlan(id: Long) {
        app.scope.launch { app.engine.restartTurnPlan(id) }
    }

    // ---- Recent patients ----

    /** null until the first load: an empty list means nobody yet, and opens the keyboard for the first MRN. */
    val wardBeds: StateFlow<List<String>?> =
        wardDao.observePatients().map { list -> list.map { it.mrn } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Patients with a running timer → the icon to show on the grid. */
    val busyBeds: StateFlow<Map<String, Int>> = combine(dao.observeHome(), turnDao.observeHome()) { tx, plans ->
        plans.filter { it.plan.status == TurnPlanStatus.RUNNING }.associate { it.plan.bedLabel to R.drawable.ic_turn } +
            tx.filter { it.transfusion.status == TransfusionStatus.RUNNING }.associate { it.transfusion.bedLabel to R.drawable.ic_drop }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // ---- Undo ----

    val undo: StateFlow<List<UndoAction>> =
        wardDao.observeUndo().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun undo(id: Long) {
        app.scope.launch { app.engine.undo(id) }
    }

    val toleranceMin: Int get() = app.settings.toleranceMin
    val limitMin: Int get() = app.settings.limitMin
    val warnBeforeMin: Int get() = app.settings.warnBeforeMin
    val missedTimeoutMin: Int get() = app.settings.missedTimeoutMin

    var lastDurationMin: Int
        get() = app.settings.lastDurationMin
        set(v) {
            app.settings.lastDurationMin = v
        }

    fun start(bedLabel: String, startAt: Long, plannedDurationMin: Int) {
        app.scope.launch { app.engine.startTransfusion(bedLabel, startAt, plannedDurationMin) }
    }

    fun edit(id: Long, bedLabel: String, startAt: Long, plannedDurationMin: Int) {
        app.scope.launch { app.engine.editTransfusion(id, bedLabel, startAt, plannedDurationMin) }
    }

    fun end(id: Long) {
        app.scope.launch { app.engine.endTransfusion(id) }
    }

    fun stopForReaction(id: Long) {
        app.scope.launch { app.engine.stopForReaction(id) }
    }

    fun forgetStopped(id: Long) {
        app.scope.launch { app.engine.forgetStopped(id) }
    }

    fun acknowledge(ids: List<Long>) {
        app.scope.launch { app.engine.acknowledge(ids) }
    }
}
