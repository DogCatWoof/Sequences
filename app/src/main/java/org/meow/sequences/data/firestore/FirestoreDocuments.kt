package org.meow.sequences.data.firestore

import com.google.firebase.Timestamp
import java.time.Instant

fun Instant.toFirestoreTimestamp() = Timestamp(epochSecond, nano)
fun Timestamp.toInstant(): Instant = Instant.ofEpochSecond(seconds, nanoseconds.toLong())
