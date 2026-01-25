package org.example.project.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import org.example.project.data.local.dao.CanvasSettingsDao
import org.example.project.data.local.dao.FolderDao
import org.example.project.data.local.dao.ShapeDao
import org.example.project.data.local.entity.CanvasSettingsEntity
import org.example.project.data.local.entity.FolderEntity
import org.example.project.data.local.entity.ShapeEntity

@Database(entities = [ShapeEntity::class, FolderEntity::class, CanvasSettingsEntity::class], version = 6)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shapeDao(): ShapeDao
    abstract fun folderDao(): FolderDao
    abstract fun canvasSettingsDao(): CanvasSettingsDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>
