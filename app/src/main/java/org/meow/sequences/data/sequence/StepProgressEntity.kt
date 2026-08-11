package org.meow.sequences.data.sequence

import androidx.room.Entity
import java.time.Instant

/** Records a single step completion within a [SequenceRunEntity]. */
@Entity(tableName = "sequence_step_progress", primaryKeys = ["runId", "stepId"])
data class StepProgressEntity(
    val runId: Long,
    val stepId: Long,
    val completedAt: Instant,
)
