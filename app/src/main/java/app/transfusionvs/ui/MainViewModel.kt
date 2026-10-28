package app.transfusionvs.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.transfusionvs.App
import app.transfusionvs.data.CheckpointWithBed
import app.transfusionvs.data.TransfusionWithCheckpoints
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State for the main screens, straight from the database (single source of truth).
 * Actions run on the app scope so they finish even if the screen closes.
 */
class MainViewModel(private val app: App) : ViewModel() {

    private val dao = app.db.dao()

    /** null until the first load (so screens don't flash "nothing running"). */
    val home: StateFlow<List<TransfusionWithCheckpoints>?> =
        dao.observeHome().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val ringing: StateFlow<List<CheckpointWithBed>> =
        dao.observeRinging().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun transfusion(id: Long): Flow<TransfusionWithCheckpoints?> = dao.observeTransfusion(id)

    val toleranceMin: Int get() = app.settings.toleranceMin
    val limitMin: Int get() = app.settings.limitMin
    val warnBeforeMin: Int get() = app.settings.warnBeforeMin

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

    fun acknowledge(ids: List<Long>) {
        app.scope.launch { app.engine.acknowledge(ids) }
    }
}
