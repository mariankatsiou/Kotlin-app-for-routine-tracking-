package si.uni_lj.fri.pbd.routinetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // Study, Exercise, Socialise
    val startTime: String,
    val endTime: String,
    val days: String,
    val notificationsEnabled: Boolean
)