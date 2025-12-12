package org.example.project.data.local

import androidx.room.RoomDatabase

expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>
