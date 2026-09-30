package si.uni_lj.fri.pbd.routinetracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "routine_executions",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("routineId"),
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RoutineExecution(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val dateTimestamp: Long,
    val completed: Boolean
)