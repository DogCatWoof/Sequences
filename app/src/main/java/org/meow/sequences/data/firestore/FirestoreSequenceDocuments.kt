package org.meow.sequences.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import org.meow.sequences.data.sequence.SequenceEntity
import org.meow.sequences.data.sequence.SequenceRunEntity
import org.meow.sequences.data.sequence.StepEntity
import java.time.Instant
import java.time.format.DateTimeFormatter

// ── Shared helpers ─────────────────────────────────────────────────────────────

fun parseDocumentInstant(s: DocumentSnapshot, field: String): Instant? {
  when (val value = s.get(field)) {
    is Timestamp -> return value.toInstant()
    is String -> {
      try { return Instant.parse(value) } catch (_: Exception) {}
      try { return DateTimeFormatter.ISO_DATE_TIME.parse(value, Instant::from) } catch (_: Exception) {}
    }
  }
  return null
}

private fun parseCreatedAt(value: String): Instant {
  if (value.isBlank()) return Instant.now()
  return try {
    Instant.parse(value)
  } catch (_: Exception) {
    try {
      DateTimeFormatter.ISO_DATE_TIME.parse(value, Instant::from)
    } catch (_: Exception) {
      Instant.now()
    }
  }
}

// ── Sequence ──────────────────────────────────────────────────────────────────

data class SequenceDocument(
  val name: String,
  val description: String,
  val stepIds: List<String>,
  val isDeleted: Boolean,
  val createdAt: String,
  val lastModifiedAt: Instant,
  val pendingFirestoreSync: Boolean,
) {
  fun toMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "description" to description.ifEmpty { null },
    "stepIds" to stepIds,
    "isDeleted" to isDeleted,
    "createdAt" to createdAt,
    "lastModifiedAt" to lastModifiedAt.toFirestoreTimestamp(),
    "pendingFirestoreSync" to pendingFirestoreSync,
  )

  companion object {
    fun fromSnapshot(s: DocumentSnapshot): SequenceDocument {
      val spec = SpecSequence.fromFirestoreMap(s.data ?: emptyMap())
      return SequenceDocument(
        name = spec.name,
        description = spec.description,
        stepIds = spec.stepIds,
        isDeleted = spec.isDeleted,
        createdAt = spec.createdAt,
        lastModifiedAt = parseDocumentInstant(s, "lastModifiedAt") ?: Instant.now(),
        pendingFirestoreSync = spec.pendingFirestoreSync,
      )
    }
  }
}

fun SequenceEntity.toDocument() = SequenceDocument(
  name = name,
  description = description,
  stepIds = emptyList(),
  isDeleted = isDeleted,
  createdAt = createdAt.toString(),
  lastModifiedAt = lastModifiedAt,
  pendingFirestoreSync = pendingFirestoreSync,
)

fun SequenceDocument.toEntity(firestoreId: String, localId: Long = 0) = SequenceEntity(
  id = localId,
  firestoreId = firestoreId,
  name = name,
  description = description,
  createdAt = parseCreatedAt(createdAt),
  isDeleted = isDeleted,
  lastModifiedAt = lastModifiedAt,
  pendingFirestoreSync = false,
)

// ── Step (normalized, separate collection) ─────────────────────────────────────

data class StepDocument(
  val sequenceFirestoreId: String,
  val instruction: String,
  val estimatedMinutes: Int?,
  val position: Int,
  val specStepId: String,
  val stepType: String,
  val isDeleted: Boolean,
  val lastModifiedAt: Instant,
) {
  fun toMap(): Map<String, Any?> = mapOf(
    "sequenceId" to sequenceFirestoreId,
    "instruction" to instruction,
    "estimatedMinutes" to estimatedMinutes,
    "position" to position,
    "specStepId" to specStepId,
    "stepType" to stepType,
    "isDeleted" to isDeleted,
    "lastModifiedAt" to lastModifiedAt.toFirestoreTimestamp(),
  )

  companion object {
    fun fromSnapshot(s: DocumentSnapshot): StepDocument {
      val spec = SpecStep.fromFirestoreMap(s.data ?: emptyMap())
      return StepDocument(
        sequenceFirestoreId = spec.sequenceId,
        instruction = spec.instruction,
        estimatedMinutes = spec.estimatedMinutes,
        position = spec.position,
        specStepId = spec.specStepId,
        stepType = spec.stepType,
        isDeleted = spec.isDeleted,
        lastModifiedAt = parseDocumentInstant(s, "lastModifiedAt") ?: Instant.now(),
      )
    }
  }
}

fun StepEntity.toDocument(sequenceFirestoreId: String) = StepDocument(
  sequenceFirestoreId = sequenceFirestoreId,
  instruction = instruction,
  estimatedMinutes = estimatedMinutes,
  position = position,
  specStepId = specStepId,
  stepType = stepType,
  isDeleted = isDeleted,
  lastModifiedAt = lastModifiedAt,
)

fun StepDocument.toEntity(
  firestoreId: String,
  sequenceLocalId: Long,
  localId: Long = 0,
) = StepEntity(
  id = localId,
  firestoreId = firestoreId,
  sequenceId = sequenceLocalId,
  specStepId = specStepId,
  instruction = instruction,
  estimatedMinutes = estimatedMinutes,
  position = position,
  stepType = stepType,
  isDeleted = isDeleted,
  lastModifiedAt = lastModifiedAt,
  pendingFirestoreSync = false,
)

// ── SequenceRun ───────────────────────────────────────────────────────────────

data class SequenceRunDocument(
  val sequenceFirestoreId: String,
  val startedAt: Instant,
  val completedAt: Instant?,
  val lastModifiedAt: Instant,
  val stepRecordsJson: String,
) {
  fun toMap(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>(
      "sequenceId" to sequenceFirestoreId,
      "startedAt" to startedAt.toFirestoreTimestamp(),
      "completedAt" to completedAt?.toFirestoreTimestamp(),
      "lastModifiedAt" to lastModifiedAt.toFirestoreTimestamp(),
    )
    val records = SpecSequenceRun.stepRecordsFromJson(stepRecordsJson)
    if (records.isNotEmpty()) map["stepRecords"] = records
    return map
  }

  companion object {
    fun fromSnapshot(s: DocumentSnapshot): SequenceRunDocument {
      val spec = SpecSequenceRun.fromFirestoreMap(s.data ?: emptyMap())
      val recordsJson = SpecSequenceRun.stepRecordsToJson(spec.stepRecords)
      return SequenceRunDocument(
        sequenceFirestoreId = spec.sequenceId,
        startedAt = parseDocumentInstant(s, "startedAt") ?: Instant.now(),
        completedAt = parseDocumentInstant(s, "completedAt"),
        lastModifiedAt = parseDocumentInstant(s, "lastModifiedAt") ?: Instant.now(),
        stepRecordsJson = recordsJson,
      )
    }
  }
}

fun SequenceRunEntity.toDocument(sequenceFirestoreId: String) = SequenceRunDocument(
  sequenceFirestoreId = sequenceFirestoreId,
  startedAt = startedAt,
  completedAt = completedAt,
  lastModifiedAt = lastModifiedAt,
  stepRecordsJson = stepRecordsJson,
)

fun SequenceRunDocument.toEntity(
  firestoreId: String,
  sequenceLocalId: Long,
  localId: Long = 0,
) = SequenceRunEntity(
  id = localId,
  firestoreId = firestoreId,
  sequenceId = sequenceLocalId,
  startedAt = startedAt,
  completedAt = completedAt,
  stepRecordsJson = stepRecordsJson,
  lastModifiedAt = lastModifiedAt,
  pendingFirestoreSync = false,
)
