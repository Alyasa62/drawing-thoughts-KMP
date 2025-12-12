package org.example.project.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "drawing_thoughts.db")
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath
    )
    .setDriver(BundledSQLiteDriver())
}
