package si.uni_lj.fri.pbd.routinetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import si.uni_lj.fri.pbd.routinetracker.data.dao.RoutineDao
import si.uni_lj.fri.pbd.routinetracker.data.entity.Routine
import si.uni_lj.fri.pbd.routinetracker.data.entity.RoutineExecution

@Database(entities = [Routine::class, RoutineExecution::class], version = 1, exportSchema = false)
abstract class RoutinesDatabase : RoomDatabase() {

    abstract fun routineDao(): RoutineDao

    companion object {
        @Volatile
        private var INSTANCE: RoutinesDatabase? = null

        fun getDatabase(context: Context): RoutinesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RoutinesDatabase::class.java,
                    "routine_tracker_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}