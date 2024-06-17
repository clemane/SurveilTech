com.surveiltech.application

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

class ScanDao(private val dbHelper: AppDatabaseHelper) {

    fun insertScan(scan: Scan): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SOME_FIELD, scan.someField)
        }
        return db.insert(TABLE_SCAN, null, values)
    }

    fun getAllScans(): List<Scan> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            TABLE_SCAN,
            null, // All columns
            null, // No selection
            null, // No selection args
            null, // No group by
            null, // No having
            null  // No order by
        )
        return cursor.use { parseScans(cursor) }
    }

    fun getScanById(scanId: Long): Scan? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            TABLE_SCAN,
            null,
            "$COLUMN_SCAN_ID = ?",
            arrayOf(scanId.toString()),
            null,
            null,
            null
        )
        return cursor.use { if (it.moveToFirst()) parseScan(it) else null }
    }

    private fun parseScans(cursor: Cursor): List<Scan> {
        val scans = mutableListOf<Scan>()
        while (cursor.moveToNext()) {
            scans.add(parseScan(cursor))
        }
        return scans
    }

    private fun parseScan(cursor: Cursor): Scan {
        val scanId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SCAN_ID))
        val someField = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SOME_FIELD))
        return Scan(scanId, someField)
    }
}

// Similaire pour les autres DAO (NetworkDao, DeviceDao, PortDao, MacVendorsDao)
