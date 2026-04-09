package com.yasaDevs.drawingthoughts.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yasaDevs.drawingthoughts.data.local.entity.ShapeEntity

@Dao
interface ShapeDao {
    @Query("SELECT * FROM ShapeEntity")
    suspend fun getAllShapes(): List<ShapeEntity>

    @Query("SELECT * FROM ShapeEntity WHERE folderId = :folderId ORDER BY id ASC")
    suspend fun getShapesByFolder(folderId: String): List<ShapeEntity>

    @Query("SELECT * FROM ShapeEntity WHERE folderId IS NULL ORDER BY id ASC")
    suspend fun getShapesWithoutFolder(): List<ShapeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShapes(shapes: List<ShapeEntity>)

    @Query("DELETE FROM ShapeEntity")
    suspend fun deleteAllShapes()

    @Query("DELETE FROM ShapeEntity WHERE folderId = :folderId")
    suspend fun deleteShapesByFolder(folderId: String)

    @Query("DELETE FROM ShapeEntity WHERE folderId IS NULL")
    suspend fun deleteShapesWithoutFolder()

    @Query("SELECT COUNT(*) FROM ShapeEntity WHERE fileName = :fileName")
    suspend fun countShapesWithFileName(fileName: String): Int
}
