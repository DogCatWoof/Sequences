package org.meow.sequences.data.firestore

import org.meow.sequences.data.sequence.SequenceDao
import java.time.Instant
import java.util.UUID

class FirestoreSyncService(
    private val source: FirestoreSource,
    private val sequenceDao: SequenceDao,
) {
    suspend fun pushPending(uid: String) {
        pushSequences(uid)
        pushSequenceSteps(uid)
        pushSequenceRuns(uid)
    }

    suspend fun pullAndMerge(uid: String, since: Instant?) {
        pullSequences(uid, since)
        pullSequenceSteps(uid, since)
        pullSequenceRuns(uid, since)
    }

    private suspend fun pushSequences(uid: String) {
        for (s in sequenceDao.getPendingFirestoreSync() + sequenceDao.getPendingFirestoreDelete()) {
            val id = s.firestoreId ?: newId()
            source.upsert(uid, "sequences", id, s.toDocument().toMap())
            sequenceDao.markSequenceFirestoreSynced(s.id, id)
        }
    }

    private suspend fun pushSequenceSteps(uid: String) {
        for (step in sequenceDao.getPendingFirestoreStepSync() + sequenceDao.getPendingFirestoreStepDelete()) {
            val seqFsId = sequenceDao.getById(step.sequenceId)?.firestoreId ?: continue
            val id = step.firestoreId ?: newId()
            source.upsert(uid, "sequenceSteps", id, step.toDocument(seqFsId).toMap())
            sequenceDao.markStepFirestoreSynced(step.id, id)
        }
    }

    private suspend fun pushSequenceRuns(uid: String) {
        for (run in sequenceDao.getPendingFirestoreRunSync()) {
            val seqFsId = sequenceDao.getById(run.sequenceId)?.firestoreId ?: continue
            val id = run.firestoreId ?: newId()
            source.upsert(uid, "sequenceRuns", id, run.toDocument(seqFsId).toMap())
            sequenceDao.markRunFirestoreSynced(run.id, id)
        }
    }

    private suspend fun pullSequences(uid: String, since: Instant?) {
        for (doc in fetchDocs(uid, "sequences", since)) {
            val remote = SequenceDocument.fromSnapshot(doc)
            val local = sequenceDao.getByFirestoreId(doc.id)
            if (local == null || remote.lastModifiedAt.toInstant() > local.lastModifiedAt) {
                sequenceDao.upsertSequence(remote.toEntity(firestoreId = doc.id, localId = local?.id ?: 0L))
            }
        }
    }

    private suspend fun pullSequenceSteps(uid: String, since: Instant?) {
        for (doc in fetchDocs(uid, "sequenceSteps", since)) {
            val remote = SequenceStepDocument.fromSnapshot(doc)
            val parent = sequenceDao.getByFirestoreId(remote.sequenceFirestoreId) ?: continue
            val local = sequenceDao.getStepByFirestoreId(doc.id)
            if (local == null || remote.lastModifiedAt.toInstant() > local.lastModifiedAt) {
                sequenceDao.upsertStep(
                    remote.toEntity(firestoreId = doc.id, sequenceLocalId = parent.id, localId = local?.id ?: 0L)
                )
            }
        }
    }

    private suspend fun pullSequenceRuns(uid: String, since: Instant?) {
        for (doc in fetchDocs(uid, "sequenceRuns", since)) {
            val remote = SequenceRunDocument.fromSnapshot(doc)
            val parent = sequenceDao.getByFirestoreId(remote.sequenceFirestoreId) ?: continue
            val local = sequenceDao.getRunByFirestoreId(doc.id)
            if (local == null || remote.lastModifiedAt.toInstant() > local.lastModifiedAt) {
                sequenceDao.upsertRun(
                    remote.toEntity(firestoreId = doc.id, sequenceLocalId = parent.id, localId = local?.id ?: 0L)
                )
            }
        }
    }

    private fun newId() = UUID.randomUUID().toString()

    private suspend fun fetchDocs(uid: String, collection: String, since: Instant?) =
        (if (since != null) source.fetchSince(uid, collection, since) else source.fetchAll(uid, collection)).documents
}
