package si.uni_lj.fri.pbd.routinetracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import si.uni_lj.fri.pbd.routinetracker.data.entity.Routine
import si.uni_lj.fri.pbd.routinetracker.data.entity.RoutineExecution


data class RoutineWithStatus(
    @Embedded val routine: Routine,
    val isCompleted: Boolean?
)

@Dao
interface RoutineDao {

    @Insert
    suspend fun insertRoutine(routine: Routine): Long

    @Update
    suspend fun updateRoutine(routine: Routine): Int

    @Delete
    suspend fun deleteRoutine(routine: Routine): Int

    @Query("SELECT * FROM routines")
    fun getAllRoutines(): Flow<List<Routine>>

    @Query("SELECT * FROM routines WHERE id = :id LIMIT 1")
    fun getRoutineById(id: Long): Flow<Routine>

    @Insert
    suspend fun insertRoutineExecution(execution: RoutineExecution)

    @Query("SELECT * FROM routine_executions WHERE routineId = :routineId ORDER BY dateTimestamp DESC")
    fun getExecutionsForRoutine(routineId: Long): Flow<List<RoutineExecution>>


    @Query("""
        SELECT r.*, 
               (SELECT completed FROM routine_executions WHERE routineId = r.id ORDER BY dateTimestamp DESC LIMIT 1) as isCompleted
        FROM routines r
    """)
    fun getRoutinesWithStatus(): Flow<List<RoutineWithStatus>>
}