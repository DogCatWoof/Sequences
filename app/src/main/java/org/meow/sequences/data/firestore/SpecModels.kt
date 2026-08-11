package org.meow.sequences.data.firestore

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class SpecSequence(
    val name: String,
    val description: String = "",
    val stepIds: List<String> = emptyList(),
    val isDeleted: Boolean = false,
    val createdAt: String = "",
    val lastModifiedAt: String = "",
    val pendingFirestoreSync: Boolean = false,
) {
    companion object {
        fun fromFirestoreMap(map: Map<String, Any?>): SpecSequence {
            val rawIds = (map["stepIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            return SpecSequence(
                name = map["name"] as? String ?: "",
                description = map["description"] as? String ?: "",
                stepIds = rawIds,
                isDeleted = map["isDeleted"] as? Boolean ?: false,
                createdAt = map["createdAt"]?.toString() ?: "",
                lastModifiedAt = map["lastModifiedAt"]?.toString() ?: "",
                pendingFirestoreSync = map["pendingFirestoreSync"] as? Boolean ?: false,
            )
        }
    }
}

data class SpecStep(
    val sequenceId: String = "",
    val instruction: String = "",
    val estimatedMinutes: Int? = null,
    val position: Int = 0,
    val specStepId: String = "",
    val stepType: String = "action",
    val isDeleted: Boolean = false,
    val lastModifiedAt: String = "",
) {
    companion object {
        fun fromFirestoreMap(map: Map<String, Any?>): SpecStep {
            return SpecStep(
                sequenceId = map["sequenceId"] as? String ?: "",
                instruction = map["instruction"] as? String ?: "",
                estimatedMinutes = (map["estimatedMinutes"] as? Long)?.toInt()
                    ?: map["estimatedMinutes"] as? Int,
                position = (map["position"] as? Long)?.toInt()
                    ?: map["position"] as? Int ?: 0,
                specStepId = map["specStepId"] as? String ?: "",
                stepType = map["stepType"] as? String ?: "action",
                isDeleted = map["isDeleted"] as? Boolean ?: false,
                lastModifiedAt = map["lastModifiedAt"]?.toString() ?: "",
            )
        }
    }
}

data class SpecSequenceRun(
    val sequenceId: String,
    val startedAt: String = "",
    val completedAt: String? = null,
    val stepRecords: List<Map<String, Any?>> = emptyList(),
) {
    companion object {
        private val gson = Gson()

        fun fromFirestoreMap(map: Map<String, Any?>): SpecSequenceRun {
            val records = (map["stepRecords"] as? List<*>)?.mapNotNull { it as? Map<String, Any?> } ?: emptyList()
            return SpecSequenceRun(
                sequenceId = map["sequenceId"] as? String ?: "",
                startedAt = map["startedAt"]?.toString() ?: "",
                completedAt = map["completedAt"]?.toString(),
                stepRecords = records,
            )
        }

        fun stepRecordsToJson(records: List<Map<String, Any?>>): String = gson.toJson(records)

        fun stepRecordsFromJson(json: String): List<Map<String, Any?>> {
            val type = object : TypeToken<List<Map<String, Any?>>>() {}.type
            return gson.fromJson(json, type) ?: emptyList()
        }
    }
}
