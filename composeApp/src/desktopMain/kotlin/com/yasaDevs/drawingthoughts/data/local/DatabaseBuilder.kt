package com.yasadevs.drawingthoughts.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

/**
 * Desktop Database Builder
 *
 * CRITICAL: Desktop KMP requires explicit SQLite driver configuration.
 * - BundledSQLiteDriver: Provides cross-platform SQLite support
 * - Database stored in system temp directory for persistence across app restarts
 */
actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "drawing_thoughts.db")
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
}
