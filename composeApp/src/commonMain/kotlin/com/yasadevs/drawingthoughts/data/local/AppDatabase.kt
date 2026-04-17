package com.yasadevs.drawingthoughts.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.yasadevs.drawingthoughts.data.local.dao.CanvasSettingsDao
import com.yasadevs.drawingthoughts.data.local.dao.FolderDao
import com.yasadevs.drawingthoughts.data.local.dao.ShapeDao
import com.yasadevs.drawingthoughts.data.local.entity.CanvasSettingsEntity
import com.yasadevs.drawingthoughts.data.local.entity.FolderEntity
import com.yasadevs.drawingthoughts.data.local.entity.ShapeEntity

@Database(
    entities = [
        ShapeEntity::class,
        FolderEntity::class,
        CanvasSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun shapeDao(): ShapeDao
    abstract fun folderDao(): FolderDao
    abstract fun canvasSettingsDao(): CanvasSettingsDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>