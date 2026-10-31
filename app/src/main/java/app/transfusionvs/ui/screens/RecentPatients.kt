package app.transfusionvs.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.transfusionvs.schedule.PatientInput
import app.transfusionvs.ui.components.BedChip
import app.transfusionvs.ui.components.CapsLabel
import app.transfusionvs.ui.components.Panel
import app.transfusionvs.ui.theme.TvsSpace
import app.transfusionvs.ui.theme.Ward
import app.transfusionvs.ui.theme.WardType

/** Who this phone remembers, and a way to take one off it. They go by themselves a day after last use. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecentPatientsPanel(patients: List<String>, onRemove: (String) -> Unit) {
    val c = Ward.c
    Panel {
        CapsLabel("Recent patients")
        Column(verticalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
            Text(
                if (patients.isEmpty()) {
                    "Nobody yet. A patient's MRN is added when you start a plan for them, and goes a day after its last use."
                } else {
                    "Each MRN goes a day after its last use. Tap one to remove it now."
                },
                color = c.muted, style = WardType.Body,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(TvsSpace.s8), verticalArrangement = Arrangement.spacedBy(TvsSpace.s8)) {
                PatientInput.sorted(patients).forEach { mrn -> BedChip(mrn, { onRemove(mrn) }) }
            }
        }
    }
}
