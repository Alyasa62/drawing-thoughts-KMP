package org.example.project.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import org.example.project.data.local.dao.ShapeDao
import org.example.project.data.local.entity.ShapeEntity

@Database(entities = [ShapeEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shapeDao(): ShapeDao
}
