package org.meow.sequences.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.meow.sequences.data.sequence.SequenceDao
import org.meow.sequences.data.sequence.SequenceEntity
import org.meow.sequences.data.sequence.SequenceRunEntity
import org.meow.sequences.data.sequence.SequenceStepEntity
import org.meow.sequences.data.sequence.SequenceStepProgressEntity
import java.time.Instant

class InstantConverter {
    @TypeConverter
    fun fromInstant(value: Instant?): String? = value?.toString()

    @TypeConverter
    fun toInstant(value: String?): Instant? = value?.let { Instant.parse(it) }
}

@Database(
    entities = [
        SequenceEntity::class,
        SequenceStepEntity::class,
        SequenceRunEntity::class,
        SequenceStepProgressEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(InstantConverter::class)
abstract class SequencesDatabase : RoomDatabase() {
    abstract fun sequenceDao(): SequenceDao

    companion object {
        @Volatile
        private var Instance: SequencesDatabase? = null

        fun getDatabase(context: Context): SequencesDatabase {
            return Instance ?: synchronized(this) {
                try {
                    Room.databaseBuilder(context, SequencesDatabase::class.java, "sequences_database")
                        .fallbackToDestructiveMigration(true)
                        .build()
                        .also { Instance = it }
                } catch (e: Exception) {
                    android.util.Log.e("SequencesDatabase", "Room init failed", e)
                    throw e
                }
            }
        }
    }
}
