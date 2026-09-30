/*
package si.uni_lj.fri.pbd.routinetracker

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "routines.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_ROUTINES = "routines"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_TYPE = "type"
        const val COLUMN_START_TIME = "startTime"
        const val COLUMN_END_TIME = "endTime"
        const val COLUMN_DAYS = "days"
        const val COLUMN_NOTIFICATIONS = "notificationsEnabled"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE $TABLE_ROUTINES (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_NAME TEXT, " +
                "$COLUMN_TYPE TEXT, " +
                "$COLUMN_START_TIME TEXT, " +
                "$COLUMN_END_TIME TEXT, " +
                "$COLUMN_DAYS TEXT, " +
                "$COLUMN_NOTIFICATIONS INTEGER)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ROUTINES")
        onCreate(db)
    }

    fun insertRoutine(routine: Routine): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, routine.name)
            put(COLUMN_TYPE, routine.type)
            put(COLUMN_START_TIME, routine.startTime)
            put(COLUMN_END_TIME, routine.endTime)
            put(COLUMN_DAYS, routine.days)
            put(COLUMN_NOTIFICATIONS, if (routine.notificationsEnabled) 1 else 0)
        }
        val id = db.insert(TABLE_ROUTINES, null, values)
        db.close()
        return id
    }
    fun getAllRoutines(): List<Routine> {
        val routineList = mutableListOf<Routine>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_ROUTINES", null)

        if (cursor.moveToFirst()) {
            do {
                val routine = Routine(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)),
                    startTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_START_TIME)),
                    endTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_END_TIME)),
                    days = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DAYS)),
                    notificationsEnabled = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NOTIFICATIONS)) == 1
                )
                routineList.add(routine)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return routineList
    }
    fun getRoutineById(id: Long): Routine? {
        val db = this.readableDatabase
        val cursor = db.query(TABLE_ROUTINES, null, "$COLUMN_ID=?", arrayOf(id.toString()), null, null, null)

        var routine: Routine? = null
        if (cursor.moveToFirst()) {
            routine = Routine(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)),
                startTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_START_TIME)),
                endTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_END_TIME)),
                days = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DAYS)),
                notificationsEnabled = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NOTIFICATIONS)) == 1
            )
        }
        cursor.close()
        db.close()
        return routine
    }

    fun deleteRoutine(id: Long) {
        val db = this.writableDatabase
        db.delete(TABLE_ROUTINES, "$COLUMN_ID=?", arrayOf(id.toString()))
        db.close()
    }
    fun updateRoutine(routine: Routine): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, routine.name)
            put(COLUMN_TYPE, routine.type)
            put(COLUMN_START_TIME, routine.startTime)
            put(COLUMN_END_TIME, routine.endTime)
            put(COLUMN_DAYS, routine.days)
            put(COLUMN_NOTIFICATIONS, if (routine.notificationsEnabled) 1 else 0)
        }
        val result = db.update(TABLE_ROUTINES, values, "$COLUMN_ID=?", arrayOf(routine.id.toString()))
        db.close()
        return result
    }
    fun deleteAllRoutines() {
        val db = this.writableDatabase
        db.execSQL("DELETE FROM $TABLE_ROUTINES")
        db.close()
    }
}*/
