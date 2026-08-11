package org.meow.sequences.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.meow.sequences.data.sequence.SequenceEntity

@Composable
fun SequenceListScreen(modifier: Modifier = Modifier) {
    val vm: SequenceViewModel = koinViewModel()
    val runVm: SequenceRunViewModel = koinViewModel()
    val sequences by vm.sequences.collectAsState()
    val runState by runVm.state.collectAsState()

    var expandedSequenceId by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        if (runState is SequenceRunUiState.Active) {
            item { ActiveRunCard(state = runState as SequenceRunUiState.Active, runVm = runVm) }
            item { HorizontalDivider() }
        }
        item { SettingsSectionLabel("Sequences") }
        items(sequences, key = { it.id }) { sequence ->
            SequenceItem(
                sequence = sequence,
                expanded = expandedSequenceId == sequence.id,
                hasActiveRun = runState is SequenceRunUiState.Active,
                onExpand = {
                    expandedSequenceId = if (expandedSequenceId == sequence.id) null else sequence.id
                },
                onStartRun = { runVm.startRun(sequence.id) },
                vm = vm,
            )
        }
    }
}

@Composable
private fun ActiveRunCard(state: SequenceRunUiState.Active, runVm: SequenceRunViewModel) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Active Run: ${state.sequence.name}")
        LinearProgressIndicator(
            progress = { state.progressFraction },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        state.currentStep?.let { step ->
            Text("Step ${state.steps.indexOf(step) + 1} of ${state.steps.size}: ${step.instruction}")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            state.currentStep?.let { step ->
                Button(onClick = { runVm.completeStep(state.run.id, step.id) }) { Text("Done") }
            }
            OutlinedButton(onClick = { runVm.endRun(state.run.id) }) { Text("End Run") }
        }
    }
}

@Composable
private fun SequenceItem(
    sequence: SequenceEntity,
    expanded: Boolean,
    hasActiveRun: Boolean,
    onExpand: () -> Unit,
    onStartRun: () -> Unit,
    vm: SequenceViewModel,
) {
    val steps by vm.getSteps(sequence.id).collectAsState(initial = emptyList())
    Column {
        ListItem(
            headlineContent = { Text(sequence.name) },
            supportingContent = { Text("${steps.size} step${if (steps.size == 1) "" else "s"}") },
            trailingContent = {
                Row {
                    if (!hasActiveRun) {
                        TextButton(onClick = onStartRun) { Text("Start") }
                    }
                    IconButton(onClick = onExpand) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                        )
                    }
                }
            },
        )
        if (expanded) {
            steps.forEach { step ->
                ListItem(
                    headlineContent = { Text("${step.position + 1}. ${step.instruction}") },
                    supportingContent = step.estimatedMinutes?.let { { Text("~$it min") } },
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
        HorizontalDivider()
    }
}
