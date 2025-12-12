package org.example.project.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import org.example.project.data.local.entity.ShapeEntity

@Dao
interface ShapeDao {
    @Query("SELECT * FROM ShapeEntity")
    suspend fun getAllShapes(): List<ShapeEntity>

    @Insert
    suspend fun insertShapes(shapes: List<ShapeEntity>)

    @Query("DELETE FROM ShapeEntity")
    suspend fun deleteAllShapes()
}
