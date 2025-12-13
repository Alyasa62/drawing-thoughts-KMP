package org.example.project.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

object AndroidWrappedContext {
    lateinit var context: Context
}

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val context = AndroidWrappedContext.context
    val dbFile = context.getDatabasePath("drawing_thoughts.db")
    return Room.databaseBuilder<AppDatabase>(
        context = context,
        name = dbFile.absolutePath
    ).fallbackToDestructiveMigration(true)
}
