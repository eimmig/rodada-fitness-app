package br.edu.utfpr.rodada.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Local storage for workout photos, as BLOBs in SQLite
 * (same approach as referencias/UsandoCamera-pm46s-2026-2).
 */
class PhotoStore(context: Context) : SQLiteOpenHelper(context, "photos.db", null, 1) {

    /** Bumps whenever a photo changes, so the UI can reload. */
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE photos (workout_id TEXT PRIMARY KEY, image BLOB NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS photos")
        onCreate(db)
    }

    suspend fun save(workoutId: String, jpeg: ByteArray) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("workout_id", workoutId)
            put("image", jpeg)
        }
        writableDatabase.insertWithOnConflict("photos", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        _version.value++
    }

    suspend fun load(workoutId: String): ByteArray? = withContext(Dispatchers.IO) {
        readableDatabase.query("photos", arrayOf("image"), "workout_id = ?", arrayOf(workoutId), null, null, null).use { c ->
            if (c.moveToFirst()) c.getBlob(0) else null
        }
    }

    suspend fun delete(workoutId: String) = withContext(Dispatchers.IO) {
        writableDatabase.delete("photos", "workout_id = ?", arrayOf(workoutId))
        _version.value++
    }
}
