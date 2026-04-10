package com.yasaDevs.drawingthoughts.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yasaDevs.drawingthoughts.data.local.dao.CanvasSettingsDao
import com.yasaDevs.drawingthoughts.data.local.dao.FolderDao
import com.yasaDevs.drawingthoughts.data.local.dao.ShapeDao
import com.yasaDevs.drawingthoughts.data.local.entity.CanvasSettingsEntity
import com.yasaDevs.drawingthoughts.data.local.entity.FolderEntity
import com.yasaDevs.drawingthoughts.data.local.entity.ShapeEntity

/** Non-destructive migration: adds the new textBoxWidth column (nullable) for text wrapping. */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ShapeEntity ADD COLUMN textBoxWidth REAL")
    }
}

@Database(entities = [ShapeEntity::class, FolderEntity::class, CanvasSettingsEntity::class], version = 8)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shapeDao(): ShapeDao
    abstract fun folderDao(): FolderDao
    abstract fun canvasSettingsDao(): CanvasSettingsDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>

